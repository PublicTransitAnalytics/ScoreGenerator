/*
 * Copyright 2017 Public Transit Analytics.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.publictransitanalytics.scoregenerator.environment;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.TreeBasedTable;
import com.google.common.collect.TreeMultimap;
import com.publictransitanalytics.scoregenerator.GeoPoint;
import com.publictransitanalytics.scoregenerator.GeoBounds;
import com.publictransitanalytics.scoregenerator.GeoLatitude;
import com.publictransitanalytics.scoregenerator.GeoLongitude;
import com.publictransitanalytics.scoregenerator.ScoreGeneratorFatalException;
import com.publictransitanalytics.scoregenerator.geography.InEnvironmentDetectorException;
import com.publictransitanalytics.scoregenerator.location.GridPoint;
import com.publictransitanalytics.scoregenerator.location.Landmark;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import com.publictransitanalytics.scoregenerator.geography.InEnvironmentDetector;

/**
 *
 * @author Public Transit Analytics
 */
@Slf4j
public class Grid {

    private final Set<GridPoint> gridPoints;
    @Getter
    private final GeoBounds bounds;
    private final ImmutableSetMultimap<PointLocation, Sector> pointSectorMap;
    @Getter
    private final Set<Sector> allSectors;
    private final TreeBasedTable<GeoLatitude, GeoLongitude, Sector> sectorTable;

    public Grid(final Set<Segment> segments, final GeoBounds bounds,
                final int numLatitudeGridlines, final int numLongitudeGridlines,
                final InEnvironmentDetector waterDetector) throws InterruptedException {

        final NavigableSet<GeoLatitude> latitudeGridlines
                = bounds.getLatitudeGridlines(numLongitudeGridlines);
        final NavigableSet<GeoLongitude> longitudeGridlines
                = bounds.getLongitudeGridlines(numLongitudeGridlines);

        final ImmutableSetMultimap<GeoLatitude, GridPoint> latitudePointMap
                = getLatitudePointMap(segments, bounds, latitudeGridlines);
        final ImmutableSetMultimap<GeoLongitude, GridPoint> longitudePointMap
                = getLongitudePointMap(segments, bounds, longitudeGridlines);

        sectorTable = getSectorTable(latitudeGridlines, longitudeGridlines,
                                     waterDetector);
        allSectors = sectorTable.cellSet().stream().map(cell -> cell.getValue())
                .collect(Collectors.toSet());

        final SetMultimap<GridPoint, Sector> gridPointSectorMap
                = dispositionSectors(latitudeGridlines, latitudePointMap,
                                     longitudeGridlines, longitudePointMap,
                                     sectorTable);
        pointSectorMap = ImmutableSetMultimap.copyOf(gridPointSectorMap);

        gridPoints = ImmutableSet.copyOf(gridPointSectorMap.keySet());
        this.bounds = bounds;
    }

    public Set<GridPoint> getGridPoints() {
        return gridPoints;
    }

    public Set<PointLocation> getCenters() {
        return allSectors.stream().map(sector -> new Landmark(
                sector.getCenter())).collect(Collectors.toSet());
    }

    public Set<Sector> getSectors(final PointLocation point) {
        if (pointSectorMap.containsKey(point)) {
            return pointSectorMap.get(point);
        }
        final Sector sector = findSector(point.getLocation());
        return (sector == null) ? null : Collections.singleton(sector);
    }

    public boolean coversPoint(final GeoPoint location) {
        return bounds.contains(location);
    }

    private static TreeBasedTable<GeoLatitude, GeoLongitude, Sector>
            getSectorTable(final NavigableSet<GeoLatitude> latitudeGridlines,
                           final NavigableSet<GeoLongitude> longitudeGridlines,
                           final InEnvironmentDetector waterDetector)
            throws InterruptedException {
        final TreeBasedTable<GeoLatitude, GeoLongitude, Sector> table
                = TreeBasedTable.create();

        try {
            final Iterator<GeoLatitude> latitudeIterator
                    = latitudeGridlines.iterator();
            GeoLatitude latitude = latitudeIterator.next();

            while (latitudeIterator.hasNext()) {
                final GeoLatitude nextLatitude = latitudeIterator.next();
                final Iterator<GeoLongitude> longitudeIterator
                        = longitudeGridlines.iterator();
                GeoLongitude longitude = longitudeIterator.next();
                while (longitudeIterator.hasNext()) {
                    final GeoLongitude nextLongitude = longitudeIterator.next();
                    final GeoBounds bounds = new GeoBounds(
                            longitude, latitude, nextLongitude, nextLatitude);
                    if (!waterDetector.isOutOfBounds(bounds)) {
                        final Sector sector = new Sector(bounds);
                        table.put(latitude, longitude, sector);
                    }
                    longitude = nextLongitude;
                }
                latitude = nextLatitude;
            }
            return table;
        } catch (final InEnvironmentDetectorException e) {
            throw new ScoreGeneratorFatalException(e);
        }
    }

    private static TreeMultimap<GeoLatitude, GridPoint> getLatitudeSortedPointsForLongitude(
            final GeoLongitude longitude,
            final SetMultimap<GeoLongitude, GridPoint> longitudePointMap) {

        final TreeMultimap<GeoLatitude, GridPoint> map = TreeMultimap.create(
                Comparator.naturalOrder(),
                (p1, p2) -> p1.getIdentifier().compareTo(p2.getIdentifier()));
        if (longitudePointMap.containsKey(longitude)) {
            for (final GridPoint point : longitudePointMap.get(longitude)) {
                map.put(point.getLocation().getLatitude(), point);
            }
        }
        return map;
    }

    private static TreeMultimap<GeoLongitude, GridPoint> getLongitudeSortedPointsForLatitude(
            final GeoLatitude latitude,
            final SetMultimap<GeoLatitude, GridPoint> latitudePointMap) {
        final TreeMultimap<GeoLongitude, GridPoint> map = TreeMultimap.create(
                Comparator.naturalOrder(),
                (p1, p2) -> p1.getIdentifier().compareTo(p2.getIdentifier()));

        if (latitudePointMap.containsKey(latitude)) {
            for (final GridPoint point : latitudePointMap.get(latitude)) {
                map.put(point.getLocation().getLongitude(), point);
            }
        }
        return map;
    }

    private Sector findSector(final GeoPoint location) {
        final SortedMap<GeoLatitude, Map<GeoLongitude, Sector>> latitudeMap
                = sectorTable.rowMap();

        final GeoLatitude maxLatitude = bounds.getNorthLat();
        /* Include the equality test to ensure that floating point rounding
         * is not making point appear unequal. */
        if (!location.getLatitude().equals(maxLatitude) &&
            location.getLatitude().compareTo(maxLatitude) > 0) {
            return null;
        }

        final GeoLatitude floorLatitudeKey;
        if (latitudeMap.containsKey(location.getLatitude())) {
            floorLatitudeKey = location.getLatitude();
        } else {
            final SortedMap<GeoLatitude, Map<GeoLongitude, Sector>> headMap
                    = latitudeMap.headMap(location.getLatitude());
            if (headMap.isEmpty()) {
                return null;
            }

            floorLatitudeKey = headMap.lastKey();
        }

        final SortedMap<GeoLongitude, Sector> longitudeMap
                = sectorTable.row(floorLatitudeKey);

        /* Include the equality test to ensure that floating point rounding
         * is not making point appear unequal. */
        final GeoLongitude maxLongitude = bounds.getEastLon();
        if (!location.getLongitude().equals(maxLongitude) && location
            .getLongitude().compareTo(maxLongitude) > 0) {
            return null;
        }
        final Sector floorLongitudeValue;

        if (longitudeMap.containsKey(location.getLongitude())) {
            floorLongitudeValue = longitudeMap.get(location.getLongitude());
        } else {
            final SortedMap<GeoLongitude, Sector> headMap
                    = longitudeMap.headMap(location.getLongitude());
            if (headMap.isEmpty()) {
                return null;
            }
            final GeoLongitude lastKey = headMap.lastKey();
            floorLongitudeValue = headMap.get(lastKey);
        }

        return floorLongitudeValue;
    }

    private static ImmutableSetMultimap<GeoLatitude, GridPoint> getLatitudePointMap(
            final Set<Segment> segments, final GeoBounds bounds,
            final NavigableSet<GeoLatitude> gridlines) {

        final List<Segment> southSortedSegments = new ArrayList<>(segments);

        southSortedSegments.sort((segment, other) -> segment.getSouthPoint()
                .getLatitude().compareTo(other.getSouthPoint().getLatitude()));

        final ImmutableSetMultimap.Builder<GeoLatitude, GridPoint> latitudePointMapBuilder
                = ImmutableSetMultimap.builder();

        for (final GeoLatitude gridline : gridlines) {

            for (final Segment segment : southSortedSegments) {

                final GeoPoint intersection = gridline.getIntersection(
                        segment, bounds);

                if (intersection != null) {
                    final GridPoint gridPoint
                            = new GridPoint(intersection, segment, gridline);
                    latitudePointMapBuilder.put(gridline, gridPoint);
                }
            }
        }
        return latitudePointMapBuilder.build();
    }

    private static ImmutableSetMultimap<GeoLongitude, GridPoint> getLongitudePointMap(
            final Set<Segment> segments, final GeoBounds bounds,
            final NavigableSet<GeoLongitude> gridlines) {

        final List<Segment> westSortedSegments = new ArrayList<>(segments);
        westSortedSegments.sort((segment, other) -> segment.getWestPoint()
                .getLongitude().compareTo(other.getWestPoint().getLongitude()));

        final ImmutableSetMultimap.Builder<GeoLongitude, GridPoint> longitudePointMapBuilder
                = ImmutableSetMultimap.builder();

        for (final GeoLongitude gridline : gridlines) {

            for (final Segment segment : westSortedSegments) {
                final GeoPoint intersection = gridline.getIntersection(
                        segment, bounds);
                if (intersection != null) {
                    final GridPoint gridPoint
                            = new GridPoint(intersection, segment, gridline);
                    longitudePointMapBuilder.put(gridline, gridPoint);
                }
            }
        }

        return longitudePointMapBuilder.build();
    }

    private static SetMultimap<GridPoint, Sector> dispositionSectors(
            final NavigableSet<GeoLatitude> latitudeGridlines,
            final ImmutableSetMultimap<GeoLatitude, GridPoint> latitudePointMap,
            final NavigableSet<GeoLongitude> longitudeGridlines,
            final ImmutableSetMultimap<GeoLongitude, GridPoint> longitudePointMap,
            TreeBasedTable<GeoLatitude, GeoLongitude, Sector> sectorTable)
            throws InterruptedException {
        final ImmutableSetMultimap.Builder<GridPoint, Sector> pointSectorMapBuilder
                = ImmutableSetMultimap.builder();

        final Iterator<GeoLongitude> longitudeIterator
                = longitudeGridlines.iterator();

        final GeoLongitude firstLongitude = longitudeIterator.next();
        GeoLongitude longitude = firstLongitude;
        TreeMultimap<GeoLatitude, GridPoint> latitudeSortedPoints
                = getLatitudeSortedPointsForLongitude(
                        longitude, longitudePointMap);

        while (longitudeIterator.hasNext()) {
            final GeoLongitude nextLongitude = longitudeIterator.next();
            final TreeMultimap<GeoLatitude, GridPoint> nextLatitudeSortedPoints
                    = getLatitudeSortedPointsForLongitude(
                            nextLongitude, longitudePointMap);

            final Iterator<GeoLatitude> latitudeIterator
                    = latitudeGridlines.iterator();

            final GeoLatitude firstLatitude = latitudeIterator.next();
            GeoLatitude latitude = firstLatitude;
            TreeMultimap<GeoLongitude, GridPoint> longitudeSortedPoints
                    = getLongitudeSortedPointsForLatitude(
                            latitude, latitudePointMap);

            while (latitudeIterator.hasNext()) {
                final GeoLatitude nextLatitude = latitudeIterator.next();
                final TreeMultimap<GeoLongitude, GridPoint> nextLongitudeSortedPoints
                        = getLongitudeSortedPointsForLatitude(
                                nextLatitude, latitudePointMap);

                final Sector sector = sectorTable.get(latitude, longitude);

                if (sector != null) {

                    final Set<GridPoint> filteredLatitudePoints
                            = latitudeSortedPoints.asMap().subMap(
                                    latitude, true, nextLatitude, true).values()
                                    .stream().flatMap(Collection::stream)
                                    .collect(Collectors.toSet());
                    for (final GridPoint point : filteredLatitudePoints) {
                        pointSectorMapBuilder.put(point, sector);
                    }

                    final Set<GridPoint> filteredNextLatitudePoints
                            = nextLatitudeSortedPoints.asMap().subMap(
                                    latitude, true, nextLatitude, true).values()
                                    .stream().flatMap(Collection::stream)
                                    .collect(Collectors.toSet());
                    for (final GridPoint point : filteredNextLatitudePoints) {
                        pointSectorMapBuilder.put(point, sector);
                    }

                    final Set<GridPoint> filteredLongitudePoints
                            = longitudeSortedPoints.asMap().subMap(
                                    longitude, true, nextLongitude, true)
                                    .values()
                                    .stream().flatMap(Collection::stream)
                                    .collect(Collectors.toSet());
                    for (final GridPoint point : filteredLongitudePoints) {
                        pointSectorMapBuilder.put(point, sector);
                    }

                    final Set<GridPoint> filteredNextLongitudePoints
                            = nextLongitudeSortedPoints.asMap().subMap(
                                    longitude, true, nextLongitude, true)
                                    .values()
                                    .stream().flatMap(Collection::stream)
                                    .collect(Collectors.toSet());
                    for (final GridPoint point : filteredNextLongitudePoints) {
                        pointSectorMapBuilder.put(point, sector);
                    }

                } 

                latitude = nextLatitude;
                longitudeSortedPoints = nextLongitudeSortedPoints;
            }

            longitude = nextLongitude;
            latitudeSortedPoints = nextLatitudeSortedPoints;
        }

        return pointSectorMapBuilder.build();
    }

}
