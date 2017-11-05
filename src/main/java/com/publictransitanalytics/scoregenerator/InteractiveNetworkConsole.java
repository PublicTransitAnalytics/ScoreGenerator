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
package com.publictransitanalytics.scoregenerator;

import com.publictransitanalytics.scoregenerator.location.TransitStop;
import com.publictransitanalytics.scoregenerator.schedule.EntryPoint;
import com.publictransitanalytics.scoregenerator.schedule.TransitNetwork;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
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
            case "exit":
                exitted = true;
                break;
            }
        }
        scanner.close();
    }

}
