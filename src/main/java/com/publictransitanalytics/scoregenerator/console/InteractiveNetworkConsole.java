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
package com.publictransitanalytics.scoregenerator.console;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.UnmodifiableIterator;
import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.schedule.EntryPoint;
import com.publictransitanalytics.scoregenerator.schedule.VehicleEvent;
import com.publictransitanalytics.scoregenerator.schedule.TransitNetwork;
import com.publictransitanalytics.scoregenerator.schedule.Trip;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
public class InteractiveNetworkConsole implements NetworkConsole {

    private final TransitNetwork transitNetwork;
    private final Map<String, TransitStop> stopIdMap;

    @Override
    public void enterConsole() {
        final Scanner scanner = new Scanner(System.in);
        boolean exitted = false;
        while (!exitted) {
            System.out.print("> ");
            final String command = scanner.next();
            switch (command) {
            case "schedule":
                final String stopId = scanner.next();
                final TransitStop stop = stopIdMap.get(stopId);
                final Set<EntryPoint> entryPoints
                        = transitNetwork.getEntryPoints(stop);
                for (final EntryPoint entryPoint : entryPoints) {
                    System.out.println(String.format(
                            "%s: %s", entryPoint.getTime(),
                            entryPoint.getTrip().getRouteNumber()));
                }
                break;
            case "deltas":
                final String route = scanner.next();
                final String beginningStopId = scanner.next();
                final String endingStopId = scanner.next();
                showDeltas(route, beginningStopId, endingStopId);
                break;
            case "exit":
                exitted = true;
                break;
            }
        }
        scanner.close();
    }

    private void showDeltas(final String route,
                            final String beginningStopId,
                            final String endingStopId) {
        final TransitStop beginningStop
                = stopIdMap.get(beginningStopId);
        final TransitStop endingStop = stopIdMap.get(endingStopId);
        final Set<EntryPoint> entryPoints
                = transitNetwork.getEntryPoints(beginningStop);
        final ImmutableListMultimap.Builder<Hop, Duration> durationsBuilder
                = ImmutableListMultimap.builder();
        for (final EntryPoint entryPoint : entryPoints) {
            final Trip trip = entryPoint.getTrip();
            if (trip.getRouteNumber().equals(route)) {
                final String tripId = trip.getTripId().toString();
                final LocalDateTime beginningTime = entryPoint.getTime();

                System.out.println(String.format("Trip %s %s", tripId,
                                                 beginningTime));
                TransitStop stop = beginningStop;
                LocalDateTime time = beginningTime;

                final Iterator<VehicleEvent> iterator = trip.getForwardIterator(
                        entryPoint.getSequence());
                while (!stop.equals(endingStop)) {
                    final VehicleEvent nextScheduledLocation = iterator.next();
                    final LocalDateTime nextTime
                            = nextScheduledLocation.getScheduledTime();
                    final TransitStop nextStop
                            = nextScheduledLocation.getLocation();
                    final Duration delta = Duration.between(time, nextTime);
                    final Hop hop = new Hop(stop, nextStop);
                    durationsBuilder.put(hop, delta);
                    System.out.println(String.format(
                            "%s -> %s: %s", stop.getStopId(),
                            nextStop.getStopId(), delta));
                    stop = nextStop;
                    time = nextTime;
                }
                System.out.println("---");
            }
        }
        final ImmutableListMultimap<Hop, Duration> hopDurations
                = durationsBuilder.build();
        
        for (final Hop hop : hopDurations.keySet()) {
            final ImmutableList<Duration> durations = hopDurations.get(hop);
            final Duration min = durations.stream().min(
                    Comparator.naturalOrder()).get();
            final Duration max = durations.stream().max(
                    Comparator.naturalOrder()).get();
            final double averageSeconds = durations.stream().collect(
                    Collectors.averagingLong(Duration::getSeconds));
            final Duration average = Duration.ofSeconds(
                    Math.round(averageSeconds));
            System.out.println(String.format(
                    "%s -> %s: min: %s max: %s avg: %s",
                    hop.getStart().getStopId(), hop.getEnd().getStopId(),
                    min, max, average));
        }

    }
}
