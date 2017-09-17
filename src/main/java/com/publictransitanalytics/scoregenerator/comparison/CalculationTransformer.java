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
package com.publictransitanalytics.scoregenerator.comparison;

import com.publictransitanalytics.scoregenerator.workflow.RangeCalculation;
import com.publictransitanalytics.scoregenerator.rider.ForwardRiderFactory;
import com.publictransitanalytics.scoregenerator.rider.RetrospectiveRiderFactory;
import com.publictransitanalytics.scoregenerator.rider.RiderFactory;
import com.publictransitanalytics.scoregenerator.schedule.PatchedTransitNetwork;
import com.publictransitanalytics.scoregenerator.scoring.ScoreCardFactory;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import com.publictransitanalytics.scoregenerator.schedule.TransitNetwork;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class CalculationTransformer {

    private final RangeCalculation original;
    private final ScoreCardFactory scoreCardFactory;
    private final Set<String> removedRoutes;

    public CalculationTransformer(final RangeCalculation original, 
                                  final ScoreCardFactory scoreCardFactory) {
        this.original = original;
        this.scoreCardFactory = scoreCardFactory;
        removedRoutes = new HashSet<>();
    }

    public void deleteRoute(final String route) {
        removedRoutes.add(route);
    }

    public RangeCalculation transform() {
        final TransitNetwork patchedNetwork = getPatchedNetwork();
        final RiderFactory newRiderFactory = getRiderFactory(patchedNetwork);
        final int newTaskCount = original.getTimesByTask().size();
        
        return new RangeCalculation(
                original.getTimesByTask(),
                scoreCardFactory.makeScoreCard(newTaskCount), 
                original.getDistanceClient(), original.getTimeTracker(),
                original.getAllowedModes(), patchedNetwork,
                original.isBackward(), original.getReachabilityClient(),
                newRiderFactory);
    }

    private TransitNetwork getPatchedNetwork() {
        if (removedRoutes.isEmpty()) {
            return original.getTransitNetwork();
        } else {
            return new PatchedTransitNetwork(removedRoutes,
                                          original.getTransitNetwork());
        }
    }

    private RiderFactory getRiderFactory(final TransitNetwork newNetwork) {
        // TODO: Account for direction changes
        if (newNetwork == original.getTransitNetwork()) {
            return original.getRiderFactory();
        } else {
            return original.isBackward() ? new RetrospectiveRiderFactory(
                    newNetwork) : new ForwardRiderFactory(newNetwork);
        }

    }

}
