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
import org.jfree.graphics2d.svg.SVGGraphics2D;
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

    private final String OUTPUT_FILENAME_TEMPLATE = "%s.svg";

    public void makeThresholdMap(final String boundsString,
                                 final Map<String, Integer> reachCounts,
                                 final int threshold, final String outputName)
            throws IOException {
        final Map<Geodetic2DBounds, Color> sectorColors
                = reachCounts.entrySet().stream().collect(Collectors.toMap(
                        entry -> getBounds(entry.getKey()),
                        entry -> entry.getValue() > threshold ? Color.GREEN
                        : Color.LIGHT_GRAY));
        makeMap(outputName, getBounds(boundsString), sectorColors);
    }

    public void makeRangeMap(final SectorTable sectorTable,
                             final ScoreCard scoreCard,
                             final double lowEnd, final double highEnd,
                             final String outputName)
            throws IOException {

        final Geodetic2DBounds bounds = sectorTable.getBounds();
        final int taskCount = scoreCard.getTaskCount();

        final Map<Geodetic2DBounds, Color> sectorColors
                = sectorTable.getSectors().stream().collect(Collectors.toMap(
                        sector -> sector.getBounds(),
                        sector -> getColor(getColorLevel(
                                scoreCard.getReachedCount(sector),
                                taskCount, lowEnd, highEnd))));
        makeMap(outputName, bounds, sectorColors);
    }

    public void makeRangeMap(final String boundsString,
                             final Map<String, Integer> reachCounts,
                             final int taskCount, final double lowEnd,
                             final double highEnd, final String outputName)
            throws IOException {

        final Map<Geodetic2DBounds, Color> sectorColors
                = reachCounts.entrySet().stream().collect(Collectors.toMap(
                        entry -> getBounds(entry.getKey()),
                        entry -> getColor(getColorLevel(
                                entry.getValue(), taskCount, lowEnd,
                                highEnd))));
        makeMap(outputName, getBounds(boundsString), sectorColors);
    }

    public void makeComparativeMap(
            final String output, final SectorTable sectorTable,
            final ScoreCard scoreCard, final ScoreCard trialScoreCard,
            final double range, final String outputName) throws IOException {
        final Geodetic2DBounds bounds = sectorTable.getBounds();

        final Map<Geodetic2DBounds, Color> sectorColors
                = sectorTable.getSectors().stream().collect(Collectors.toMap(
                        sector -> sector.getBounds(),
                        sector -> getComparativeColor(getComparativeColorLevel(
                                scoreCard.getReachedCount(sector),
                                trialScoreCard.getReachedCount(sector),
                                range))));

        makeMap(outputName, bounds, sectorColors);
    }

    public void makeComparativeMap(
            final String boundsString,
            final Map<String, Integer> baseReachCounts,
            final Map<String, Integer> trialReachCounts, final double range,
            final String outputName) throws IOException {

        final Set<String> boundsStringSet = Sets.union(
                baseReachCounts.keySet(), trialReachCounts.keySet());

        final Map<Geodetic2DBounds, Color> sectorColors
                = boundsStringSet.stream().collect(Collectors.toMap(
                        key -> getBounds(key),
                        key -> getComparativeColor(getComparativeColorLevel(
                                baseReachCounts.getOrDefault(key, 0),
                                trialReachCounts.getOrDefault(key, 0),
                                range))));
        makeMap(outputName, getBounds(boundsString), sectorColors);
    }

    private void makeMap(final String outputName, final Geodetic2DBounds bounds,
                         final Map<Geodetic2DBounds, Color> sectorColors)
            throws IOException {
        final SVGGraphics2D svgGenerator = createDocument(bounds);

        final Geodetic2DPoint topCorner = new Geodetic2DPoint(
                bounds.getWestLon(), bounds.getNorthLat());

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
        final Writer out = new FileWriter(new File(
                String.format(OUTPUT_FILENAME_TEMPLATE, outputName)));
        out.write(svgGenerator.getSVGDocument());
        out.close();
    }

    private static SVGGraphics2D createDocument(final Geodetic2DBounds bounds) {

        final SVGGraphics2D svgGenerator = new SVGGraphics2D(
                (int) (getLonSize(bounds) + 0.5), 
                (int) (getLatSize(bounds) + 0.5));
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

    private int getColorLevel(final int reachCount, final int samples,
                              final double lowEnd, final double highEnd) {
        return BigDecimal.valueOf(reachCount)
                .divide(BigDecimal.valueOf(samples), 10,
                        RoundingMode.HALF_EVEN)
                .subtract(BigDecimal.valueOf(lowEnd))
                .divide(BigDecimal.valueOf(highEnd - lowEnd), 0,
                        RoundingMode.FLOOR).intValueExact();
    }

    private static Color getColor(final int colorLevel) {
        return NINE_GREEN_LEVELS[colorLevel];
    }

    private static Color getComparativeColor(final int colorLevel) {
        if (colorLevel > NINE_ORANGE_LEVELS.length) {
            return HIGH_OUTLIER_COLOR;
        } else if (colorLevel > 0) {
            return NINE_ORANGE_LEVELS[colorLevel - 1];
        } else if (colorLevel < -NINE_BLUE_LEVELS.length) {
            return HIGH_OUTLIER_COLOR;
        } else if (colorLevel < 0) {
            final Color level = NINE_BLUE_LEVELS[(-colorLevel) - 1];
            return level;
        }
        return NEUTRAL_COLOR;
    }

    private int getComparativeColorLevel(final int baseReachCount,
                                         final int trialReachCount,
                                         final double range) {
        if (baseReachCount == 0 && trialReachCount == 0) {
            return 0;
        } else if (baseReachCount == 0) {
            return NINE_ORANGE_LEVELS.length + 1;
        } else {
            final BigDecimal change = BigDecimal.valueOf(
                    trialReachCount - baseReachCount);
            final BigDecimal changeRatio = change.divide(
                    BigDecimal.valueOf(baseReachCount), 10,
                    RoundingMode.HALF_EVEN);
            final BigDecimal scaled = changeRatio
                    .multiply(BigDecimal.valueOf(NINE_ORANGE_LEVELS.length))
                    .divide(BigDecimal.valueOf(range), 0, RoundingMode.DOWN);

            return scaled.intValueExact();
        }
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
