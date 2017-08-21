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

import com.publictransitanalytics.scoregenerator.ModeType;
import com.publictransitanalytics.scoregenerator.distanceclient.ReachabilityClient;
import com.publictransitanalytics.scoregenerator.rider.RiderBehaviorFactory;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Workflow for computing paths from tasks.
 *
 * @author Public Transit Analytics
 */
public interface Workflow {

    void getPathsForTasks(
            final Duration duration, final Set<ModeType> allowedModes,
            final RiderBehaviorFactory riderFactory,
            final ReachabilityClient reachabilityClient,
            final Set<TaskIdentifier> tasks) throws
            InterruptedException, ExecutionException;

}
