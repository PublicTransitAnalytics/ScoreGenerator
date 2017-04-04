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
package com.publictransitanalytics.scoregenerator.schedule;

import com.google.common.collect.ImmutableList;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.Value;

/**
 * An interval of a single day.
 *
 * @author Public Transit Analytics
 */
@Value
public class IntervalOfDay {

    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;

    public static List<IntervalOfDay> getIntervals(
            final LocalDateTime earliestTime, final LocalDateTime latestTime) {
        final ImmutableList.Builder<IntervalOfDay> intervals
                = ImmutableList.builder();

        if (earliestTime.toLocalDate().equals(latestTime.toLocalDate())) {
            intervals.add(new IntervalOfDay(
                    earliestTime.toLocalDate(), earliestTime.toLocalTime(),
                    latestTime.toLocalTime()));
        } else {
            intervals.add(new IntervalOfDay(
                    earliestTime.toLocalDate(), earliestTime.toLocalTime(),
                    LocalTime.MAX));
            LocalDate date = earliestTime.toLocalDate().plusDays(1);
            while (date.isBefore(latestTime.toLocalDate())) {
                intervals.add(new IntervalOfDay(date, LocalTime.MIN,
                                                LocalTime.MAX));
                date = date.plusDays(1);
            }
            intervals.add(new IntervalOfDay(
                    latestTime.toLocalDate(), LocalTime.MIN,
                    latestTime.toLocalTime()));
        }
        return intervals.build();
    }

}
