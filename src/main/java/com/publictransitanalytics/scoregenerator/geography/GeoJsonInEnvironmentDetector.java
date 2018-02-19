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
import com.publictransitanalytics.scoregenerator.ScoreGeneratorFatalException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import org.json.JSONException;

/**
 * Uses a GeoJson file of water polygons to detect water. Naively does a linear
 * search each time.
 *
 * @author Public Transit Analytics
 */
public class GeoJsonInEnvironmentDetector implements InEnvironmentDetector {

    private static final SpatialReference WSG84_PROJECTION
            = SpatialReference.create(4326);
    private final Collection<Geometry> waterGeometry;
    private final Geometry borderGeometry;

    public GeoJsonInEnvironmentDetector(final Path geoJsonBorderFile,
                                        final Path geoJsonWaterFile)
            throws IOException, InEnvironmentDetectorException {

        final ImmutableSet.Builder<Geometry> builder = ImmutableSet
                .builder();
        Files.lines(geoJsonWaterFile, StandardCharsets.UTF_8)
                .map(line -> convertToGeometry(line))
                .forEach(builder::add);
        waterGeometry = builder.build();

        final String borderJson = new String(
                Files.readAllBytes(geoJsonBorderFile),
                StandardCharsets.UTF_8);

        borderGeometry = convertToGeometry(borderJson);
    }

    private static Geometry convertToGeometry(final String line) {
        final OperatorImportFromGeoJson operation
                = OperatorImportFromGeoJson.local();
        try {
            final MapOGCStructure featureMap = operation.executeOGC(
                    GeoJsonImportFlags.geoJsonImportDefaults, line, null);
            return featureMap.m_ogcStructure.m_structures
                    .get(0).m_geometry;
        } catch (final JSONException e) {
            throw new ScoreGeneratorFatalException(e);
        }
    }

    @Override
    public boolean isOutOfBounds(final GeoPoint point) {
        final Point internalPoint = new Point(point.getLatitude().getDegrees(),
                                              point.getLatitude().getDegrees());

        boolean onWater = false;
        for (final Geometry featureGeometry : waterGeometry) {
            if (GeometryEngine.contains(featureGeometry, internalPoint,
                                        WSG84_PROJECTION)) {
                onWater = true;
                break;
            }
        }
        final boolean inBorder = GeometryEngine.contains(
                borderGeometry, internalPoint, WSG84_PROJECTION);
        return onWater || !inBorder;
    }

    @Override
    public boolean isOutOfBounds(final GeoBounds bounds) {
        final Envelope envelope = new Envelope(
                bounds.getSouthLat().getDegrees(),
                bounds.getEastLon().getDegrees(),
                bounds.getNorthLat().getDegrees(),
                bounds.getWestLon().getDegrees());

        boolean onWater = false;
        for (final Geometry featureGeometry : waterGeometry) {
            if (GeometryEngine.contains(featureGeometry, envelope,
                                        WSG84_PROJECTION)) {
                onWater = true;
                break;
            }
        }
        final boolean inBorder
                = GeometryEngine.overlaps(
                        borderGeometry, envelope, WSG84_PROJECTION) ||
                  GeometryEngine.contains(
                          borderGeometry, envelope, WSG84_PROJECTION);
        return onWater || !inBorder;
    }
}
