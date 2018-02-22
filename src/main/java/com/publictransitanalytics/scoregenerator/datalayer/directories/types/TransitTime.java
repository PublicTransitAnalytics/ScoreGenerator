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
package com.publictransitanalytics.scoregenerator.datalayer.directories.types;

import java.time.Duration;
import java.time.LocalTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Value;

/**
 * TransitTime is a local time that allows improper times, such as 25 hours to
 * indicate overlapping into the next actual day on the same day of transit
 * service. TransitTime has second granularity.
 *
 * @author Public Transit Analytics
 */
@Value
public class TransitTime implements Comparable<TransitTime> {

    /**
     * Regular expression for transit dates as specified by the GTFS format.
     */
    private static final Pattern PATTERN = Pattern.compile(
            "(\\d{1,2}):(\\d{2}):(\\d{2})");

    public static final TransitTime MIN_TRANSIT_TIME
            = new TransitTime(0, 0, 0);

    public static final TransitTime MAX_TRANSIT_TIME
            = new TransitTime(47, 59, 59);
    
    private final String stringRepresentation; 

    /**
     * Create a transit time from an ordinary time.
     *
     * @param time The time.
     * @return The transit time.
     */
    public static TransitTime fromLocalTime(LocalTime time) {
        return new TransitTime(time.getHour(), time.getMinute(),
                               time.getSecond());
    }

    /**
     * Create a transit time from an ordinary time as though that time were in
     * the next actual day, but was being considered as part of the current
     * service day.
     *
     * @param time The time.
     * @return The transit time.
     */
    public static TransitTime fromLocalTimeWithOverflow(LocalTime time) {
        return new TransitTime(time.getHour() + 24, time.getMinute(),
                               time.getSecond());
    }

    private final int hours;
    private final int minutes;
    private final int seconds;

    /**
     * Create a new TransitTime with the specified hours, minutes, and seconds.
     *
     * @param hours Number of hours. Must be less than 48.
     * @param minutes Number of minutes. Must be less than 60.
     * @param seconds Number of seconds. Must be less than 60.
     */
    public TransitTime(final int hours, final int minutes, final int seconds) {
        if (seconds > 59 || seconds < 0) {
            throw new IllegalArgumentException();
        }
        if (minutes > 59 || minutes < 0) {
            throw new IllegalArgumentException();
        }

        if (hours > 47 || hours < 0) {
            throw new IllegalArgumentException();
        }

        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;
        stringRepresentation = String.format(
                "%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Parse a String in the GTFS standard format and create a new TransitTime.
     *
     * @param stringRep The string representation of a transit time.
     * @return A new TransitTime.
     */
    public static TransitTime parse(final String stringRep) {
        Matcher matcher = PATTERN.matcher(stringRep);
        if (matcher.matches()) {
            return new TransitTime(Integer.valueOf(matcher.group(1)),
                                   Integer.valueOf(matcher.group(2)),
                                   Integer.valueOf(matcher.group(3)));
        }
        throw new IllegalArgumentException(String.format(
                "%s cannot be converted to a TransitTime.", stringRep));
    }

    /**
     * Get the duration between two TransitTimes.
     *
     * @param first A TransitTime..
     * @param second A TransitTime.
     * @return The duration.
     */
    public static Duration durationBetween(TransitTime first, TransitTime second) {
        return Duration.ofSeconds(
                (second.hours * 3600 + second.minutes * 60 + second.seconds)
                        - (first.hours * 3600 + first.minutes * 60
                           + first.seconds));
    }

    @Override
    public int compareTo(TransitTime o) {
        int hourDifference = hours - o.hours;
        if (hourDifference == 0) {
            int minuteDifference = minutes - o.minutes;
            if (minuteDifference == 0) {
                return seconds - o.seconds;
            }
            return minuteDifference;
        }
        return hourDifference;
    }

    public boolean isBefore(TransitTime endTime) {
        return compareTo(endTime) < 0;
    }

    public boolean isAfter(TransitTime endTime) {
        return compareTo(endTime) > 0;
    }

    /**
     * Add a Duration to the TransitTime, creating a new TransitTime object.
     *
     * @param duration The duration to add.
     * @return A new TransitTime.
     */
    public TransitTime plus(Duration duration) {
        long newSeconds = duration.getSeconds() + (this.hours * 3600)
                                  + (this.minutes * 60) + this.seconds;
        long justSeconds = (newSeconds % 60);
        long newMinutes = newSeconds / 60;
        long justMinutes = (newMinutes % 60);
        long newHours = newMinutes / 60;

        return new TransitTime((int) newHours, (int) justMinutes,
                               (int) justSeconds);
    }

    /**
     * Subtract a Duration from the TransitTime, creating a new TransitTime
     * object.
     *
     * @param duration The duration to add.
     * @return A new TransitTime.
     */
    public TransitTime minus(Duration duration) {
        return plus(duration.negated());
    }

    @Override
    public String toString() {
        return stringRepresentation;
    }

}
