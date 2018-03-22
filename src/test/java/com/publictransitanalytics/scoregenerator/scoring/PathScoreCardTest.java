/*
 * Copyright 2018 Public Transit Analytics.
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
package com.publictransitanalytics.scoregenerator.scoring;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import com.publictransitanalytics.scoregenerator.geography.AngleUnit;
import com.publictransitanalytics.scoregenerator.geography.GeoBounds;
import com.publictransitanalytics.scoregenerator.geography.GeoLatitude;
import com.publictransitanalytics.scoregenerator.geography.GeoLongitude;
import com.publictransitanalytics.scoregenerator.geography.GeoPoint;
import com.publictransitanalytics.scoregenerator.location.Center;
import com.publictransitanalytics.scoregenerator.location.Landmark;
import com.publictransitanalytics.scoregenerator.location.LogicalCenter;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.testhelpers.PreloadedMovementAssembler;
import com.publictransitanalytics.scoregenerator.visitors.ModeInfo;
import com.publictransitanalytics.scoregenerator.walking.ForwardTimeTracker;
import com.publictransitanalytics.scoregenerator.walking.TimeTracker;
import com.publictransitanalytics.scoregenerator.workflow.DynamicProgrammingRecord;
import com.publictransitanalytics.scoregenerator.workflow.MovementAssembler;
import com.publictransitanalytics.scoregenerator.workflow.TaskIdentifier;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class PathScoreCardTest {

    private static final PointLocation REACHED_POINT = new Landmark(
            new GeoPoint(new GeoLongitude("-122.32370", AngleUnit.DEGREES),
                         new GeoLatitude("47.654656", AngleUnit.DEGREES)));
    private static final Sector REACHED_SECTOR = new Sector(new GeoBounds(
            new GeoLongitude("-122.45970", AngleUnit.DEGREES),
            new GeoLatitude("47.48172", AngleUnit.DEGREES),
            new GeoLongitude("-122.22443", AngleUnit.DEGREES),
            new GeoLatitude("47.734145", AngleUnit.DEGREES)));

    private static final SetMultimap<PointLocation, Sector> POINT_SECTOR_MAP
            = ImmutableSetMultimap.of(REACHED_POINT, REACHED_SECTOR);

    private static final Map<PointLocation, DynamicProgrammingRecord> FULL_STATE_MAP
            = ImmutableMap.of(REACHED_POINT, new DynamicProgrammingRecord(
                              LocalDateTime.MIN, ModeInfo.NONE, null));

    private static final PointLocation PHYSICAL_CENTER1 = new Landmark(
            new GeoPoint(new GeoLongitude("-122", AngleUnit.DEGREES),
                         new GeoLatitude("47", AngleUnit.DEGREES)));

    private static final LogicalCenter LOGICAL_CENTER1 = new Landmark(
            new GeoPoint(new GeoLongitude("-123", AngleUnit.DEGREES),
                         new GeoLatitude("48", AngleUnit.DEGREES)));

    private static final MovementAssembler ASSEMBLER
            = new PreloadedMovementAssembler();

    private static final TimeTracker TIME_TRACKER = new ForwardTimeTracker();

    @Test
    public void testScoresTask() {
        final PathScoreCard scoreCard = new PathScoreCard(
                1, POINT_SECTOR_MAP, ASSEMBLER, TIME_TRACKER);
        final Center center = new Center(
                LOGICAL_CENTER1, Collections.singleton(PHYSICAL_CENTER1));

        final TaskIdentifier task = new TaskIdentifier(LocalDateTime.MIN,
                                                       center);

        scoreCard.scoreTask(task, FULL_STATE_MAP);
        Assert.assertTrue(scoreCard.hasPath(REACHED_SECTOR));
        Assert.assertEquals(1, scoreCard.getReachedCount(REACHED_SECTOR));
    }

}
