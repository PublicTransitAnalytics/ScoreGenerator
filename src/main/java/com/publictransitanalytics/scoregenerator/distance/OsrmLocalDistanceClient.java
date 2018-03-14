/*
 * Copyright 2018 Public Transit Analytics.
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
package com.publictransitanalytics.scoregenerator.distance;

import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.publictransitanalytics.scoregenerator.location.PointLocation;
import com.publictransitanalytics.scoregenerator.walking.WalkingCosts;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Public Transit Analytics
 */
@RequiredArgsConstructor
@Slf4j
public class OsrmLocalDistanceClient implements DistanceClient {

    private static final String OK_STATUS = "Ok";
    
    private final OkHttpClient client;
    private final String host;
    private final int port;
    private final PointSequencerFactory factory;

    @Override
    public Map<PointLocation, WalkingCosts> getDistances(
            final PointLocation point,
            final Set<PointLocation> consideredPoints)
            throws DistanceClientException, InterruptedException {
        final PointSequencer sequencer = factory.getSequencer(
                point, consideredPoints);

        final StringJoiner coordinateJoiner = new StringJoiner(";");
        final List<PointLocation> sequence = sequencer.getSequence();
        sequence.stream().map(location -> String.format(
                "%f,%f", location.getLocation().getLongitude().getDegrees(),
                location.getLocation().getLatitude().getDegrees()))
                .forEach(coordinateJoiner::add);

        final StringJoiner originJoiner = new StringJoiner(";");
        sequencer.getOrigins().stream().map(i -> i.toString())
                .forEach(originJoiner::add);
        final String originIndexString = originJoiner.toString();

        final StringJoiner destinationJoiner = new StringJoiner(";");
        sequencer.getDestinations().stream().map(i -> i.toString())
                .forEach(destinationJoiner::add);
        final String destinationIndexString = destinationJoiner.toString();

        final String coordinateString = coordinateJoiner.toString();

        final String url = String.format(
                "http://%s:%d/table/v1/foot/%s?sources=%s&destinations=%s&generate_hints=false",
                host, port, coordinateString, originIndexString,
                destinationIndexString);

        try {
            final Request request = new Request.Builder().url(url).build();
            final Response response = client.newCall(request).execute();
            final String responseJsonString = response.body().string();
            final DocumentContext json = JsonPath.parse(responseJsonString);

            final String status = json.read("$.code");

            if (status.equals(OK_STATUS)) {
                final List durations = json.read("$.durations[*][*]");

                final ImmutableMap.Builder<PointLocation, WalkingCosts> builder
                        = ImmutableMap.builder();
                for (int i = 0; i < consideredPoints.size(); i++) {
                    final PointLocation location = sequence.get(i);
                    final Double durationSeconds = Double.valueOf(
                            durations.get(i).toString());

                    if (durationSeconds != null) {
                        final Duration duration = Duration.ofSeconds(
                                (long) Math.ceil(durationSeconds));
                        builder.put(location, new WalkingCosts(duration, -1));
                    } else {
                        log.warn("Could not get duration for {}", location);
                    }

                }
                return builder.build();
            } else {
                throw new DistanceClientException(String.format(
                        "osrm-routed request failed with code %s", status));
            }
        } catch (final IOException e) {
            throw new DistanceClientException(e);
        }
    }

    @Override
    public void close() {
    }

}
