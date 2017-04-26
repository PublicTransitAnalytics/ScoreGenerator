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
package com.publictransitanalytics.scoregenerator.datalayer.directories;

import com.publictransitanalytics.scoregenerator.datalayer.directories.types.ServiceSet;
import java.time.LocalDate;

/**
 * A calendar that maps days to the service types available on that day.
 *
 * @author Public Transit Analytics
 */
public interface ServiceTypeCalendar {

    /**
     * Gets the description of the service available on a day. Returns null if
     * no service is available.
     *
     * @param date The date on which to retrieve service.
     * @return the service available.
     */
    ServiceSet getServiceType(final LocalDate date)
            throws InterruptedException;

}
