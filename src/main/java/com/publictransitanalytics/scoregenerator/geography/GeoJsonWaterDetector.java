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
package com.publictransitanalytics.scoregenerator.geography;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeoJsonImportFlags;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.MapOGCStructure;
import com.esri.core.geometry.OperatorImportFromGeoJson;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.google.common.collect.ImmutableSet;
import com.publictransitanalytics.scoregenerator.GeoPoint;
import com.publictransitanalytics.scoregenerator.GeoBounds;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;
import org.json.JSONException;

/**
 * Uses a GeoJson file of water polygons to detect water. Naively does a linear
 * search each time.
 *
 * @author Public Transit Analytics
 */
public class GeoJsonWaterDetector implements WaterDetector {

    private static final SpatialReference MERCATOR_PROJECTION
            = SpatialReference.create(4326);
    private final Collection<Geometry> waterGeometry;

    public GeoJsonWaterDetector(final Path geoJsonFile) throws IOException,
            WaterDetectorException {

        try {
            final Stream<String> stream = Files.lines(geoJsonFile,
                                                      StandardCharsets.UTF_8);
            final OperatorImportFromGeoJson operation
                    = OperatorImportFromGeoJson.local();
            final Iterator<String> iterator = stream.iterator();
            final ImmutableSet.Builder<Geometry> builder = ImmutableSet
                    .builder();

            while (iterator.hasNext()) {
                final String json = iterator.next();
                final MapOGCStructure featureMap = operation.executeOGC(
                        GeoJsonImportFlags.geoJsonImportDefaults, json, null);

                builder.add(featureMap.m_ogcStructure.m_structures
                        .get(0).m_geometry);
            }
            waterGeometry = builder.build();
        } catch (JSONException e) {
            throw new WaterDetectorException(e);
        }

    }

    @Override
    public boolean isOnWater(final GeoPoint point) {
        final Point internalPoint = new Point(point.getLatitude().getDegrees(),
                                              point.getLatitude().getDegrees());

        for (final Geometry featureGeometry : waterGeometry) {
            if (GeometryEngine.contains(featureGeometry, internalPoint,
                                        MERCATOR_PROJECTION)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isEntirelyWater(final GeoBounds bounds) {
        final Envelope envelope = new Envelope(
                bounds.getSouthLat().getDegrees(),
                bounds.getEastLon().getDegrees(),
                bounds.getNorthLat().getDegrees(),
                bounds.getWestLon().getDegrees());

        for (final Geometry featureGeometry : waterGeometry) {
            if (GeometryEngine.contains(featureGeometry, envelope,
                                        MERCATOR_PROJECTION)) {
                return true;
            }
        }
        return false;
    }
}
