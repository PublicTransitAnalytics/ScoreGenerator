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
package com.publictransitanalytics.scoregenerator.visitors;

import com.google.common.collect.ImmutableSet;
import com.publictransitanalytics.scoregenerator.ModeType;
import com.publictransitanalytics.scoregenerator.ScoreGeneratorFatalException;
import com.publictransitanalytics.scoregenerator.distanceclient.DistanceClientException;
import com.publictransitanalytics.scoregenerator.distanceclient.ReachabilityClient;
import com.publictransitanalytics.scoregenerator.location.Landmark;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.location.Sector;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.location.VisitableLocation;
import com.publictransitanalytics.scoregenerator.walking.TimeTracker;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * A walking visitor that does not recurse.
 * 
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class FlatWalkVisitor implements FlatVisitor<Set<ReachabilityOutput>> {

    private final LocalDateTime cutoffTime;
    private final LocalDateTime currentTime;
    private final ReachabilityClient reachabilityClient;
    private final TimeTracker timeTracker;

    private Set<ReachabilityOutput> output;

    @Override
    public Set<ReachabilityOutput> getOutput() {
        return output;
    }

    @Override
    public void visit(Sector sector) throws InterruptedException {
        output = Collections.emptySet();
    }

    @Override
    public void visit(TransitStop transitStop) throws InterruptedException {
        walkFrom(transitStop);
    }

    @Override
    public void visit(Landmark point) throws InterruptedException {
        walkFrom(point);
    }

    private void walkFrom(final PointLocation location)
            throws InterruptedException {
        final ImmutableSet.Builder<ReachabilityOutput> outputBuilder
                = ImmutableSet.builder();

        final Sector sector = location.getContainingSector();
        outputBuilder.add(new ReachabilityOutput(
                sector, currentTime, currentTime, ModeInfo.NONE));

        final Map<VisitableLocation, WalkingCosts> walkCosts
                = getWalkCosts(location);
        for (final Map.Entry<VisitableLocation, WalkingCosts> entry
                     : walkCosts.entrySet()) {
            final WalkingCosts costs = entry.getValue();
            final Duration walkingDuration = costs.getDuration();

            if (timeTracker.canAdjust(currentTime, walkingDuration,
                                      cutoffTime)) {
                final LocalDateTime newTime = timeTracker.adjust(
                        currentTime, walkingDuration);
                final VisitableLocation walkableLocation = entry.getKey();
                

                outputBuilder.add(new ReachabilityOutput(
                        walkableLocation, newTime, currentTime,
                        new ModeInfo(ModeType.WALKING, null, costs)));
            }
        }
        output = outputBuilder.build();
    }

    private Map<VisitableLocation, WalkingCosts> getWalkCosts(
            final PointLocation location) throws InterruptedException {
        try {
            final Map<VisitableLocation, WalkingCosts> walkCosts
                    = reachabilityClient.getWalkingDistances(
                            location, currentTime, cutoffTime);
            return walkCosts;
        } catch (DistanceClientException e) {
            throw new ScoreGeneratorFatalException(e);
        }
    }

}