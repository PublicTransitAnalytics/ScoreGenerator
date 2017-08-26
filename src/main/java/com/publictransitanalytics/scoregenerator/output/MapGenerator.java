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
package com.publictransitanalytics.scoregenerator.output;

import com.google.common.collect.Sets;
import com.publictransitanalytics.scoregenerator.SectorTable;
import com.publictransitanalytics.scoregenerator.scoring.ScoreCard;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Shape;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.svggen.SVGGraphics2DIOException;
import org.opensextant.geodesy.Geodetic2DArc;
import org.opensextant.geodesy.Geodetic2DBounds;
import org.opensextant.geodesy.Geodetic2DPoint;
import org.opensextant.geodesy.Latitude;
import org.opensextant.geodesy.Longitude;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

/**
 *
 * @author Public Transit Analytics
 */
public class MapGenerator {

    private final static Color[] NINE_GREEN_LEVELS = {
        new Color(0xf7, 0xfc, 0xf5),
        new Color(0xe5, 0xf5, 0xe0),
        new Color(0xc7, 0xe9, 0xc0),
        new Color(0xa1, 0xd9, 0x9b),
        new Color(0x74, 0xc4, 0x76),
        new Color(0x41, 0xab, 0x5d),
        new Color(0x23, 0x8b, 0x45),
        new Color(0x00, 0x6d, 0x2c),
        new Color(0x00, 0x44, 0x1b)};

    private final static Color[] NINE_BLUE_LEVELS = {
        new Color(0xf7, 0xfb, 0xff),
        new Color(0xde, 0xeb, 0xf7),
        new Color(0xc6, 0xdb, 0xef),
        new Color(0x9e, 0xca, 0xe1),
        new Color(0x6b, 0xae, 0xd6),
        new Color(0x42, 0x92, 0xc6),
        new Color(0x21, 0x71, 0xb5),
        new Color(0x08, 0x51, 0x9c),
        new Color(0x08, 0x30, 0x6b)};

    private final static Color[] NINE_ORANGE_LEVELS = {
        new Color(0xff, 0xf5, 0xeb),
        new Color(0xfe, 0xe6, 0xce),
        new Color(0xfd, 0xd0, 0xa2),
        new Color(0xfd, 0xae, 0x6b),
        new Color(0xfd, 0x8d, 0x3c),
        new Color(0xf1, 0x69, 0x13),
        new Color(0xd9, 0x48, 0x01),
        new Color(0xa6, 0x36, 0x03),
        new Color(0x7f, 0x27, 0x04)};

    private final static Color NEUTRAL_COLOR = new Color(0xcc, 0xcc, 0xcc);
    private final static Color HIGH_OUTLIER_COLOR = new Color(0x00, 0x00, 0x00);
    private final static Color LOW_OUTLIER_COLOR = new Color(0xff, 0xff, 0xff);

    private final String OUTPUT_FILE = "map.svg";

    public void makeThresholdMap(final String boundsString,
                                 final Map<String, Integer> reachCounts,
                                 final int threshold) throws IOException {
        final Map<Geodetic2DBounds, Color> sectorColors
                = reachCounts.entrySet().stream().collect(Collectors.toMap(
                        entry -> getBounds(entry.getKey()),
                        entry -> entry.getValue() > threshold ? Color.GREEN :
                                Color.LIGHT_GRAY));
        makeMap(getBounds(boundsString), sectorColors);
    }

    public void makeRangeMap(final SectorTable sectorTable,
                             final ScoreCard scoreCard)
            throws SVGGraphics2DIOException, IOException {

        final Geodetic2DBounds bounds = sectorTable.getBounds();

        final Map<Geodetic2DBounds, Color> sectorBounds
                = sectorTable.getSectors().stream().collect(Collectors.toMap(
                        sector -> sector.getBounds(),
                        sector -> getColor(getColorLevel(
                                scoreCard.getReachedCount(sector)))));
        makeMap(bounds, sectorBounds);
    }

    public void makeRangeMap(final String boundsString,
                             final Map<String, Integer> reachCounts)
            throws IOException {
        final Map<Geodetic2DBounds, Color> sectorColors
                = reachCounts.entrySet().stream().collect(Collectors.toMap(
                        entry -> getBounds(entry.getKey()),
                        entry -> getColor(getColorLevel(entry.getValue()))));
        makeMap(getBounds(boundsString), sectorColors);
    }

    public void makeComparativeMap(
            final String boundsString,
            final Map<String, Integer> baseReachCounts,
            final Map<String, Integer> trialReachCounts) throws IOException {

        final Set<String> boundsStringSet = Sets.union(
                baseReachCounts.keySet(), trialReachCounts.keySet());

        final Map<Geodetic2DBounds, Integer> sectorBounds
                = boundsStringSet.stream().collect(Collectors.toMap(
                        key -> getBounds(key),
                        key -> getComparativeColorLevel(
                                baseReachCounts.getOrDefault(key, 0),
                                trialReachCounts.getOrDefault(key, 0))));
        makeComparativeMap(getBounds(boundsString), sectorBounds);
    }

    private void makeMap(final Geodetic2DBounds bounds,
                         final Map<Geodetic2DBounds, Color> sectorColors)
            throws IOException {
        final SVGGraphics2D svgGenerator = createDocument();

        final Geodetic2DPoint topCorner = new Geodetic2DPoint(
                bounds.getWestLon(), bounds.getNorthLat());

        svgGenerator.setSVGCanvasSize(new Dimension(
                (int) (getLonSize(bounds) + 0.5),
                (int) (getLatSize(bounds) + 0.5)));
        svgGenerator.setBackground(Color.DARK_GRAY);
        svgGenerator.setStroke(new BasicStroke(17.0F));
        for (final Geodetic2DBounds sectorBounds : sectorColors.keySet()) {
            final Shape rectangle
                    = getRectangle(bounds, sectorBounds, topCorner);

            svgGenerator.setPaint(sectorColors.get(sectorBounds));
            svgGenerator.fill(rectangle);
            svgGenerator.setPaint(Color.GRAY);
            svgGenerator.draw(rectangle);
        }
        boolean useCSS = true; // we want to use CSS style attributes
        final Writer out = new FileWriter(new File(OUTPUT_FILE));
        svgGenerator.stream(out, useCSS);
    }

    private void makeComparativeMap(
            final Geodetic2DBounds bounds,
            final Map<Geodetic2DBounds, Integer> sectorBuckets)
            throws IOException {
        final SVGGraphics2D svgGenerator = createDocument();

        final Geodetic2DPoint topCorner = new Geodetic2DPoint(
                bounds.getWestLon(), bounds.getNorthLat());

        svgGenerator.setSVGCanvasSize(new Dimension(
                (int) (getLonSize(bounds) + 0.5),
                (int) (getLatSize(bounds) + 0.5)));
        svgGenerator.setBackground(Color.DARK_GRAY);
        svgGenerator.setStroke(new BasicStroke(17.0F));
        for (final Geodetic2DBounds sectorBounds : sectorBuckets.keySet()) {
            final Shape rectangle
                    = getRectangle(bounds, sectorBounds, topCorner);

            svgGenerator.setPaint(getComparativeColor(
                    sectorBuckets.get(sectorBounds)));
            svgGenerator.fill(rectangle);
            svgGenerator.setPaint(Color.GRAY);
            svgGenerator.draw(rectangle);
        }
        boolean useCSS = true; // we want to use CSS style attributes
        final Writer out = new FileWriter(new File(OUTPUT_FILE));
        svgGenerator.stream(out, useCSS);
    }

    private static SVGGraphics2D createDocument() {
        final DOMImplementation domImpl = GenericDOMImplementation
                .getDOMImplementation();

        final String svgNS = "http://www.w3.org/2000/svg";
        final Document document = domImpl.createDocument(svgNS, "svg", null);

        final SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
        return svgGenerator;
    }

    private static double getLonDelta(final Geodetic2DBounds bounds,
                                      final Geodetic2DBounds sectorBounds,
                                      final Geodetic2DPoint topCorner) {
        return new Geodetic2DArc(new Geodetic2DPoint(
                sectorBounds.getWestLon(), bounds.getNorthLat()), topCorner)
                .getDistanceInMeters();
    }

    private static double getLatDelta(final Geodetic2DBounds bounds,
                                      final Geodetic2DBounds sectorBounds,
                                      final Geodetic2DPoint topCorner) {
        return new Geodetic2DArc(new Geodetic2DPoint(
                bounds.getWestLon(), sectorBounds.getNorthLat()), topCorner)
                .getDistanceInMeters();
    }

    private static double getLatSize(final Geodetic2DBounds bounds) {
        return new Geodetic2DArc(
                new Geodetic2DPoint(bounds.getWestLon(),
                                    bounds.getNorthLat()),
                new Geodetic2DPoint(bounds.getWestLon(),
                                    bounds.getSouthLat()))
                .getDistanceInMeters();
    }

    private static double getLonSize(final Geodetic2DBounds bounds) {
        return new Geodetic2DArc(
                new Geodetic2DPoint(bounds.getEastLon(),
                                    bounds.getNorthLat()),
                new Geodetic2DPoint(bounds.getWestLon(),
                                    bounds.getNorthLat()))
                .getDistanceInMeters();
    }

    private static Shape getRectangle(final Geodetic2DBounds bounds,
                                      final Geodetic2DBounds sectorBounds,
                                      final Geodetic2DPoint topCorner) {
        final double lonDelta = getLonDelta(bounds, sectorBounds,
                                            topCorner);

        final double latDelta = getLatDelta(bounds, sectorBounds,
                                            topCorner);

        final double latSize = getLatSize(sectorBounds);

        final double lonSize = getLonSize(sectorBounds);
        final Shape rectangle = new Rectangle.Double(lonDelta, latDelta,
                                                     lonSize, latSize);
        return rectangle;
    }

    private int getColorLevel(final int reachCount) {
        return BigDecimal.valueOf(reachCount)
                .divide(BigDecimal.valueOf(6063 * 1440), 10,
                        RoundingMode.HALF_EVEN)
                .divide(BigDecimal.valueOf(0.02), 0, RoundingMode.FLOOR)
                .intValueExact();
    }

    private static Color getColor(final int colorLevel) {
        return NINE_GREEN_LEVELS[colorLevel];
    }

    private static Color getComparativeColor(final int colorLevel) {
        if (colorLevel > 9) {
            System.err.println(String.format("higher outlier %d", colorLevel));
            return HIGH_OUTLIER_COLOR;
        } else if (colorLevel > 0) {
            return NINE_ORANGE_LEVELS[colorLevel - 1];
        } else if (colorLevel < -9) {
            System.err.println(String.format("low outlier %d", colorLevel));
            return HIGH_OUTLIER_COLOR;
        } else if (colorLevel < 0) {
            final Color level = NINE_BLUE_LEVELS[(-colorLevel) - 1];
            return level;
        }
        return NEUTRAL_COLOR;
    }

    private int getComparativeColorLevel(final int baseReachCount,
                                         final int trialReachCount) {
        final BigDecimal change = BigDecimal.valueOf(
                trialReachCount - baseReachCount);
        final BigDecimal changeRatio = change.divide(
                BigDecimal.valueOf(baseReachCount), 10, RoundingMode.HALF_EVEN);
        final BigDecimal scaled = changeRatio.divide(
                BigDecimal.valueOf(0.0525), 0, RoundingMode.DOWN);

        System.err.println(String.format(
                "trial %d base %d change %s scaled %s",
                trialReachCount, baseReachCount, changeRatio, scaled));

        return scaled.intValueExact();
    }

    private static Geodetic2DBounds getBounds(final String boundsString) {
        final String[] boundElements = boundsString.split(",");
        final Geodetic2DPoint westPoint = new Geodetic2DPoint(
                new Longitude(boundElements[3]), new Latitude(boundElements[0]));
        final Geodetic2DPoint eastPoint = new Geodetic2DPoint(
                new Longitude(boundElements[2]), new Latitude(boundElements[1]));
        final Geodetic2DBounds bounds
                = new Geodetic2DBounds(westPoint, eastPoint);
        return bounds;
    }
}
