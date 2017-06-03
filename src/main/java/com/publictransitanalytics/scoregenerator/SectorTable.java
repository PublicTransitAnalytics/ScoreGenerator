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
package com.publictransitanalytics.scoregenerator;

import com.publictransitanalytics.scoregenerator.location.Sector;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.TreeBasedTable;
import com.publictransitanalytics.scoregenerator.geography.WaterDetector;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;
import lombok.Getter;
import org.opensextant.geodesy.Angle;
import org.opensextant.geodesy.Geodetic2DBounds;
import org.opensextant.geodesy.Geodetic2DPoint;
import org.opensextant.geodesy.Latitude;
import org.opensextant.geodesy.Longitude;

/**
 * A table that allows fast lookups from a point to its containing sector.
 *
 * @author Public Transit Analytics
 */
public class SectorTable {

    @Getter
    private final Geodetic2DBounds bounds;
    private final Angle latitudeDelta;
    private final Angle longitudeDelta;
    private final TreeBasedTable<Latitude, Longitude, Sector> sectorTable;
    private final Set<Sector> blacklist;

    public SectorTable(final Geodetic2DBounds bounds,
                       final int numLatitudeSectors,
                       final int numLongitudeSectors,
                       final WaterDetector waterDetector) {
        sectorTable = TreeBasedTable.create();
        this.bounds = bounds;

        final Angle latitudeDifference = bounds.getSouthLat().difference(bounds
                .getNorthLat());
        final double latitudeDeltaDegrees = latitudeDifference.inDegrees()
                                                    / numLatitudeSectors;
        latitudeDelta = new Angle(latitudeDeltaDegrees, Latitude.DEGREES);

        final Angle longitudeDifference = bounds.getWestLon().difference(bounds
                .getEastLon());
        final double longitudeDeltaDegrees = longitudeDifference.inDegrees()
                                                     / numLongitudeSectors;
        longitudeDelta = new Angle(longitudeDeltaDegrees, Longitude.DEGREES);

        final ImmutableSet.Builder<Sector> blacklistBuilder
                = ImmutableSet.builder();
        // work south to north
        Angle latitude = new Latitude(bounds.getSouthLat());
        for (int i = 0; i < numLatitudeSectors; i++) {
            final Angle nextLatitude = latitude.add(latitudeDelta);
            // work west to east
            Angle longitude = new Longitude(bounds.getWestLon());
            for (int j = 0; j < numLongitudeSectors; j++) {
                Angle nextLongitude = longitude.add(longitudeDelta);

                Geodetic2DPoint point = new Geodetic2DPoint(
                        new Longitude(longitude),
                        new Latitude(latitude));
                Geodetic2DPoint nextPoint = new Geodetic2DPoint(
                        new Longitude(nextLongitude),
                        new Latitude(nextLatitude));

                final Geodetic2DBounds sectorBounds
                        = new Geodetic2DBounds(point, nextPoint);

                final Latitude indexLatitude = new Latitude(latitude);
                final Longitude indexLongitude = new Longitude(longitude);

                final Sector sector = new Sector(sectorBounds);
                if (waterDetector.isEntirelyWater(sector.getBounds())) {
                    blacklistBuilder.add(sector);
                }
                sectorTable.put(indexLatitude, indexLongitude, sector);
                longitude = nextLongitude;
            }
            latitude = nextLatitude;
        }
        blacklist = blacklistBuilder.build();
    }

    public Sector findSector(final Geodetic2DPoint location) {
        final SortedMap<Latitude, Map<Longitude, Sector>> latitudeMap
                = sectorTable.rowMap();

        final Angle maxLatitude = latitudeMap.lastKey().add(latitudeDelta);
        /* Include the equality test to ensure that floating point rounding
         * is not making point appear unequal. */
        if (!location.getLatitude().equals(maxLatitude) && location
                .getLatitude().compareTo(maxLatitude) > 0) {
            return null;
        }

        final Latitude floorLatitudeKey;
        if (latitudeMap.containsKey(location.getLatitude())) {
            floorLatitudeKey = location.getLatitude();
        } else {
            final SortedMap<Latitude, Map<Longitude, Sector>> headMap
                    = latitudeMap.headMap(location.getLatitude());
            if (headMap.isEmpty()) {
                return null;
            }

            floorLatitudeKey = headMap.lastKey();
        }

        final SortedMap<Longitude, Sector> longitudeMap
                = sectorTable.row(floorLatitudeKey);

        /* Include the equality test to ensure that floating point rounding
         * is not making point appear unequal. */
        final Angle maxLongitude = longitudeMap.lastKey().add(longitudeDelta);
        if (!location.getLongitude().equals(maxLongitude) && location
                .getLongitude().compareTo(maxLongitude) > 0) {
            return null;
        }
        final Sector floorLongitudeValue;

        if (longitudeMap.containsKey(location.getLongitude())) {
            floorLongitudeValue = longitudeMap.get(location.getLongitude());
        } else {
            final SortedMap<Longitude, Sector> headMap
                    = longitudeMap.headMap(location.getLongitude());
            if (headMap.isEmpty()) {
                return null;
            }
            final Longitude lastKey = headMap.lastKey();
            floorLongitudeValue = headMap.get(lastKey);
        }

        return blacklist.contains(floorLongitudeValue) ? null
                : floorLongitudeValue;
    }

    public Sector getSector(final Geodetic2DBounds bounds) {
        final Sector sector
                = sectorTable.get(bounds.getSouthLat(), bounds.getWestLon());
        return blacklist.contains(sector) ? null : sector;
    }

    public Set<Sector> getSectors() {
        return sectorTable.values().stream()
                .filter(sector -> !blacklist.contains(sector))
                .collect(Collectors.toSet());
    }

    public Sector northSector(final Sector center) {
        final SortedMap<Latitude, Map<Longitude, Sector>> rowMap
                = sectorTable.rowMap();
        final Iterator<Latitude> iterator = rowMap.tailMap(center.getBounds()
                .getSouthLat()).keySet().iterator();
        iterator.next();
        final Latitude nextLatitude = iterator.next();
        return sectorTable.get(nextLatitude,
                               center.getBounds().getWestLon());
    }

    public Sector southSector(final Sector center) {
        final SortedMap<Latitude, Map<Longitude, Sector>> rowMap
                = sectorTable.rowMap();
        final Latitude previousLatitude = rowMap.headMap(center.getBounds()
                .getSouthLat()).lastKey();
        return sectorTable.get(previousLatitude,
                               center.getBounds().getWestLon());
    }

    public Sector westSector(final Sector center) {
        final SortedMap<Longitude, Sector> latitude = sectorTable.row(center
                .getBounds().getSouthLat());
        final Longitude previousLongitude
                = latitude.headMap(center.getBounds().getWestLon()).lastKey();
        return sectorTable.get(center.getBounds().getSouthLat(),
                               previousLongitude);

    }

    public Sector eastSector(final Sector center) {
        final SortedMap<Longitude, Sector> latitude = sectorTable.row(center
                .getBounds().getSouthLat());
        final Iterator<Longitude> iterator = latitude.tailMap(center.getBounds()
                .getWestLon()).keySet().iterator();
        iterator.next();
        final Longitude nextLongitude = iterator.next();
        return sectorTable.get(center.getBounds().getSouthLat(),
                               nextLongitude);

    }

}
