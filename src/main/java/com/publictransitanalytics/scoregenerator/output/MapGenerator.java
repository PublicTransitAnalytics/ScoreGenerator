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

import com.publictransitanalytics.scoregenerator.geography.GeoPoint;
import com.publictransitanalytics.scoregenerator.geography.GeoBounds;
import com.publictransitanalytics.scoregenerator.environment.Grid;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.environment.Segment;
import com.publictransitanalytics.scoregenerator.scoring.ScoreCard;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jfree.graphics2d.svg.SVGGraphics2D;

/**
 *
 * @author Public Transit Analytics
 */
public class MapGenerator {

    private final static Color[] ONE_GREEN_LEVEL = {
        new Color(0x23, 0x8b, 0x45)};

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

    public void makeEmptyMap(final Grid grid,
                             final Set<? extends PointLocation> markedPoints,
                             final Set<Segment> markedLines,
                             final String outputName) throws IOException {
        final GeoBounds bounds = grid.getBounds();
        final Map<GeoBounds, Color> sectorColors
                = grid.getAllSectors().stream().collect(
                        Collectors.toMap(entry -> entry.getBounds(),
                                         entry -> Color.WHITE));
        makeMap(outputName, bounds, markedPoints, markedLines, sectorColors);
    }

    public void makeRangeMap(final Grid grid, final ScoreCard scoreCard,
                             final Set<PointLocation> markedPoints,
                             final double lowEnd, final double highEnd,
                             final String outputName)
            throws IOException {

        final GeoBounds bounds = grid.getBounds();
        final int taskCount = scoreCard.getTaskCount();

        final Color[] colorSet;
        if (taskCount == 1) {
            colorSet = ONE_GREEN_LEVEL;
        } else {
            colorSet = NINE_GREEN_LEVELS;
        }

        final Map<GeoBounds, Color> sectorColors
                = grid.getAllSectors().stream().collect(Collectors.toMap(
                        sector -> sector.getBounds(),
                        sector -> getColor(
                                scoreCard.getReachedCount(sector),
                                taskCount, lowEnd, highEnd, colorSet)));

        makeMap(outputName, bounds, markedPoints, Collections.emptySet(),
                sectorColors);
    }

    public void makeComparativeMap(
            final Grid grid, final ScoreCard scoreCard,
            final ScoreCard trialScoreCard,
            final Set<PointLocation> markedPoints, final double range,
            final String outputName) throws IOException {
        final GeoBounds bounds = grid.getBounds();

        final Map<GeoBounds, Color> sectorColors
                = grid.getAllSectors().stream().collect(Collectors.toMap(
                        sector -> sector.getBounds(),
                        sector -> getComparativeColor(
                                trialScoreCard.getReachedCount(sector) -
                                scoreCard.getReachedCount(sector),
                                range, NINE_ORANGE_LEVELS, NINE_BLUE_LEVELS)));

        makeMap(outputName, bounds, markedPoints, Collections.emptySet(),
                sectorColors);
    }

    private void makeMap(final String outputName, final GeoBounds bounds,
                         final Set<? extends PointLocation> markedPoints,
                         final Set<Segment> segments,
                         final Map<GeoBounds, Color> sectorColors)
            throws IOException {
        final SVGGraphics2D svgGenerator = createDocument(bounds);

        svgGenerator.setBackground(Color.DARK_GRAY);
        svgGenerator.setStroke(new BasicStroke(17.0F));
        for (final GeoBounds sectorBounds : sectorColors.keySet()) {
            final Shape rectangle
                    = getRectangle(bounds, sectorBounds);

            svgGenerator.setPaint(sectorColors.get(sectorBounds));
            svgGenerator.fill(rectangle);
            svgGenerator.setPaint(Color.GRAY);
            svgGenerator.draw(rectangle);
        }
        for (final PointLocation markedPoint : markedPoints) {
            final Shape point = getPoint(bounds, markedPoint.getLocation(),
                                         200.0F);
            svgGenerator.setPaint(Color.BLACK);
            svgGenerator.fill(point);
        }
        for (final Segment segment : segments) {
            final Shape line = getLine(bounds, segment.getEastPoint(),
                                       segment.getWestPoint());
            svgGenerator.setPaint(Color.RED);
            svgGenerator.draw(line);
        }

        final Writer out = new FileWriter(new File(
                String.format(OUTPUT_FILENAME_TEMPLATE, outputName)));
        out.write(svgGenerator.getSVGDocument());
        out.close();
    }

    private static SVGGraphics2D createDocument(final GeoBounds bounds) {

        final SVGGraphics2D svgGenerator = new SVGGraphics2D(
                (int) (getLonSize(bounds) + 0.5),
                (int) (getLatSize(bounds) + 0.5));
        return svgGenerator;
    }

    private static double getLatDelta(final GeoBounds bounds,
                                      final GeoPoint topCorner) {
        return topCorner.getDistanceMeters(
                new GeoPoint(topCorner.getLongitude(),
                             bounds.getNorthLat()));
    }

    private static double getLonDelta(final GeoBounds bounds,
                                      final GeoPoint topCorner) {
        return topCorner.getDistanceMeters(new GeoPoint(bounds.getWestLon(),
                                                        topCorner.getLatitude()));
    }

    private static double getLatSize(final GeoBounds bounds) {
        return new GeoPoint(bounds.getWestLon(),
                            bounds.getNorthLat()).getDistanceMeters(
                new GeoPoint(bounds.getWestLon(),
                             bounds.getSouthLat()));
    }

    private static double getLonSize(final GeoBounds bounds) {
        return new GeoPoint(bounds.getEastLon(),
                            bounds.getNorthLat()).getDistanceMeters(
                new GeoPoint(bounds.getWestLon(),
                             bounds.getNorthLat()));
    }

    private static Shape getPoint(final GeoBounds bounds,
                                  final GeoPoint point,
                                  final double size) {
        final double offset = size / 2;

        final double lonDelta = getLonDelta(bounds, point) - offset;
        final double latDelta = getLatDelta(bounds, point) - offset;
        return new Ellipse2D.Double(lonDelta, latDelta, size, size);
    }

    private static Shape getRectangle(final GeoBounds bounds,
                                      final GeoBounds sectorBounds) {
        final GeoPoint topCorner = new GeoPoint(
                sectorBounds.getWestLon(), sectorBounds.getNorthLat());
        final double lonDelta = getLonDelta(bounds, topCorner);

        final double latDelta = getLatDelta(bounds, topCorner);

        final double latSize = getLatSize(sectorBounds);

        final double lonSize = getLonSize(sectorBounds);
        final Shape rectangle = new Rectangle.Double(lonDelta, latDelta,
                                                     lonSize, latSize);
        return rectangle;
    }

    private static Shape getLine(final GeoBounds bounds,
                                 final GeoPoint point1,
                                 final GeoPoint point2) {

        final double firstLonDelta = getLonDelta(bounds, point1);
        final double firstLatDelta = getLatDelta(bounds, point1);

        final double secondLonDelta = getLonDelta(bounds, point2);
        final double secondLatDelta = getLatDelta(bounds, point2);

        final Line2D line = new Line2D.Double(firstLonDelta, firstLatDelta,
                                              secondLonDelta, secondLatDelta);
        return line;
    }

    private Color getColor(final int reachCount, final int samples,
                           final double lowEnd, final double highEnd,
                           final Color[] colors) {
        final double reachProportion = ((double) reachCount) /
                                       ((double) samples);
        if (reachProportion == highEnd) {
            return colors[colors.length - 1];
        } else if (reachProportion > highEnd) {
            return HIGH_OUTLIER_COLOR;
        } else if (reachProportion <= lowEnd) {
            return LOW_OUTLIER_COLOR;
        } else {
            final int numColors = colors.length;
            double adjustedPropertion = reachProportion - lowEnd;
            double range = highEnd - lowEnd;
            double size = range / numColors;
            int index = (int) Math.floor(adjustedPropertion / size);
            return colors[index];
        }
    }

    private static Color getComparativeColor(
            final int reachChange, final double range, final Color[] baseColors,
            final Color[] trialColors) {

        final double degree;
        final Color[] colors;
        if (reachChange > 0) {
            degree = reachChange;
            colors = baseColors;
        } else if (reachChange < 0) {
            degree = -reachChange;
            colors = trialColors;
        } else {
            return NEUTRAL_COLOR;
        }

        final int level = (int) Math.floor(
                (degree / range) * (colors.length + 1));
        if (level > colors.length) {
            return HIGH_OUTLIER_COLOR;
        }
        return colors[level - 1];
    }

}
