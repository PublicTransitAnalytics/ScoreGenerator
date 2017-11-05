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
package com.publictransitanalytics.scoregenerator.workflow;

import com.publictransitanalytics.scoregenerator.distanceclient.ReachabilityClient;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import com.publictransitanalytics.scoregenerator.rider.RiderFactory;
import com.publictransitanalytics.scoregenerator.scoring.ScoreCard;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import com.publictransitanalytics.scoregenerator.walking.TimeTracker;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
@Slf4j
public class RepeatedRangeExecutor implements RangeExecutor {

    private final DynamicProgrammingAlgorithm algorithm;
    private final Environment environment;

    @Override
    public void executeRange(final Calculation calculation,
                             final TaskGroupIdentifier taskGroup) throws
            InterruptedException {
        final int maxDepth = environment.getMaxDepth();
        final Duration duration = environment.getLongestDuration();

        final ScoreCard scoreCard = calculation.getScoreCard();
        final TimeTracker timeTracker = calculation.getTimeTracker();
        final RiderFactory riderFactory = calculation.getRiderFactory();
        final ReachabilityClient reachabilityClient
                = calculation.getReachabilityClient();

        final Instant profileStartTime = Instant.now();
        final PointLocation startLocation = taskGroup.getCenter();

        final Iterator<LocalDateTime> timeIterator
                = timeTracker.getTimeIterator(calculation.getTimes());

        while (timeIterator.hasNext()) {
             final LocalDateTime startTime = timeIterator.next();
            final LocalDateTime cutoffTime = timeTracker.adjust(
                    startTime, duration);
            Map<VisitableLocation, DynamicProgrammingRecord> map
                    = algorithm.getMap(startTime, cutoffTime,
                                       startLocation, timeTracker, duration,
                                       maxDepth, reachabilityClient,
                                       riderFactory);

            final TaskIdentifier latestFullTask = new TaskIdentifier(
                    startTime, startLocation);

            final MovementAssembler assembler = calculation
                    .getMovementAssembler();

            updateScoreCard(map, latestFullTask, scoreCard, assembler);
        }
        final Instant profileEndTime = Instant.now();

        log.info("Finished {} at {} (wallclock {}).", taskGroup,
                 profileEndTime.toString(),
                 Duration.between(profileStartTime, profileEndTime));
    }
    
    private void updateScoreCard(
            final Map<VisitableLocation, DynamicProgrammingRecord> map,
            final TaskIdentifier task, final ScoreCard scoreCard,
            final MovementAssembler assembler)
            throws InterruptedException {

        final Set<VisitableLocation> reachedLocations = map.keySet();

        for (final VisitableLocation reachedLocation : reachedLocations) {
            final MovementPath path = assembler.assemble(reachedLocation, map);
            scoreCard.putPath(reachedLocation, task, path);
        }
    }

}
