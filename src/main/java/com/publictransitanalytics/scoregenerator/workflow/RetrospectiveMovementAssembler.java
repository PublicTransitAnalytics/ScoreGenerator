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

import com.google.common.collect.ImmutableList;
import com.publictransitanalytics.scoregenerator.ModeType;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.schedule.EntryPoint;
import com.publictransitanalytics.scoregenerator.tracking.FixedPath;
import com.publictransitanalytics.scoregenerator.tracking.Movement;
import com.publictransitanalytics.scoregenerator.tracking.MovementPath;
import com.publictransitanalytics.scoregenerator.tracking.TransitRideMovement;
import com.publictransitanalytics.scoregenerator.tracking.WalkMovement;
import com.publictransitanalytics.scoregenerator.visitors.ModeInfo;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Public Transit Analytics
 */
@Slf4j
public class RetrospectiveMovementAssembler implements MovementAssembler {

    @Override
    public MovementPath assemble(
            final PointLocation terminal,
            final Map<PointLocation, DynamicProgrammingRecord> lastRow) {

        final ImmutableList.Builder<Movement> movementsBuilder
                = ImmutableList.builder();

        PointLocation location = terminal;
        DynamicProgrammingRecord record = lastRow.get(location);

        while (record != null) {
            final ModeInfo mode = record.getMode();
            final ModeType type = mode.getType();
            if (type.equals(ModeType.TRANSIT)) {
                final EntryPoint trip = mode.getTransitTrip();
                final Movement movement = new TransitRideMovement(
                        trip.getTrip(), location, record.getReachTime(),
                        record.getPredecessor(), trip.getTime());
                movementsBuilder.add(movement);
            } else if (type.equals(ModeType.WALKING)) {
                final WalkingCosts costs = mode.getWalkCosts();
                final PointLocation predecessor
                        = record.getPredecessor();
                final LocalDateTime predecessorReachTime = lastRow.get(
                        predecessor).getReachTime();
                final double distanceMeters = costs.getDistanceMeters();
                final LocalDateTime reachTime = record.getReachTime();
                final Movement movement = new WalkMovement(
                        reachTime, distanceMeters, location,
                        predecessorReachTime, predecessor);
                movementsBuilder.add(movement);
            }
            location = record.getPredecessor();
            record = (location == null) ? null : lastRow.get(location);
            final ImmutableList<Movement> movements = movementsBuilder.build();
            if (movements.size() > 1) {
                final Movement recentMovement = movements.get(movements.size()
                                                              - 1);
                final Movement priorMovement = movements.get(movements.size()
                                                             - 2);
                if (recentMovement instanceof WalkMovement
                            && priorMovement instanceof WalkMovement) {
                }
            }

        }
        return new FixedPath(movementsBuilder.build());
    }

}
