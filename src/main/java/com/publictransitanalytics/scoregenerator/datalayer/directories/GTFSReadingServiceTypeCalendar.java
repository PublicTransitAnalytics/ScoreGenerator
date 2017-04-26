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

import com.bitvantage.bitvantagecaching.Store;
import com.publictransitanalytics.scoregenerator.ScoreGeneratorFatalException;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.ServiceSet;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.DateKey;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Calendar for tracking what service type is in effect for a day.
 *
 * @author Public Transit Analytics
 */
public class GTFSReadingServiceTypeCalendar implements ServiceTypeCalendar {

    private final Store<DateKey, ServiceSet> serviceTypesStore;

    public GTFSReadingServiceTypeCalendar(
            final Store<DateKey, ServiceSet> serviceTypesStore,
            final Reader calendarReader, final Reader calendarDatesReader)
            throws IOException, InterruptedException {

        this.serviceTypesStore = serviceTypesStore;
        if (serviceTypesStore.isEmpty()) {
            final SetMultimap<LocalDate, String> serviceTypesMap
                    = HashMultimap.create();
            parseCalendarFile(calendarReader, serviceTypesMap);
            parseCalendarDatesFile(calendarDatesReader, serviceTypesMap);

            for (Map.Entry<LocalDate, Collection<String>> entry
                         : serviceTypesMap.asMap().entrySet()) {
                serviceTypesStore.put(
                        new DateKey(entry.getKey()),
                        new ServiceSet(ImmutableSet.copyOf(entry.getValue())));
            }
        }
    }

    @Override
    public ServiceSet getServiceType(final LocalDate date)
            throws InterruptedException {
        return serviceTypesStore.get(new DateKey(date));
    }

    private void parseCalendarFile(
            final Reader calendarReader,
            final Multimap<LocalDate, String> serviceTypesMap)
            throws IOException {

        final CSVParser calendarParser
                = new CSVParser(calendarReader, CSVFormat.DEFAULT.withHeader());
        final List<CSVRecord> calendarRecords = calendarParser.getRecords();

        LocalDate earliestDate = null;
        LocalDate latestDate = null;
        for (final CSVRecord record : calendarRecords) {
            final String serviceType = record.get("service_id");

            final LocalDate start = LocalDate.parse(
                    record.get("start_date"), DateTimeFormatter.BASIC_ISO_DATE);
            if (earliestDate == null || start.isBefore(earliestDate)) {
                earliestDate = start;
            }

            final LocalDate end = LocalDate.parse(
                    record.get("end_date"), DateTimeFormatter.BASIC_ISO_DATE);
            if (latestDate == null || end.isAfter(latestDate)) {
                latestDate = end;
            }

            final EnumSet<DayOfWeek> daysOfWeek = EnumSet
                    .noneOf(DayOfWeek.class);
            if (record.get("monday").equals("1")) {
                daysOfWeek.add(DayOfWeek.MONDAY);
            }
            if (record.get("tuesday").equals("1")) {
                daysOfWeek.add(DayOfWeek.TUESDAY);
            }
            if (record.get("wednesday").equals("1")) {
                daysOfWeek.add(DayOfWeek.WEDNESDAY);
            }
            if (record.get("thursday").equals("1")) {
                daysOfWeek.add(DayOfWeek.THURSDAY);
            }
            if (record.get("friday").equals("1")) {
                daysOfWeek.add(DayOfWeek.FRIDAY);
            }
            if (record.get("saturday").equals("1")) {
                daysOfWeek.add(DayOfWeek.SATURDAY);
            }
            if (record.get("sunday").equals("1")) {
                daysOfWeek.add(DayOfWeek.SUNDAY);
            }

            LocalDate targetDate = start;
            while (!targetDate.isAfter(end)) {
                if (daysOfWeek.contains(targetDate.getDayOfWeek())) {
                    serviceTypesMap.put(targetDate, serviceType);
                }
                targetDate = targetDate.plusDays(1);
            }
        }
    }

    private void parseCalendarDatesFile(
            final Reader calendarDatesReader,
            final Multimap<LocalDate, String> serviceTypesMap)
            throws FileNotFoundException, IOException {

        final CSVParser calendarDatesParser = new CSVParser(
                calendarDatesReader, CSVFormat.DEFAULT.withHeader());
        final List<CSVRecord> calendarDatesRecords
                = calendarDatesParser.getRecords();

        for (final CSVRecord record : calendarDatesRecords) {
            final String serviceType = record.get("service_id");
            final LocalDate date = LocalDate.parse(
                    record.get("date"), DateTimeFormatter.BASIC_ISO_DATE);
            final String exceptionType = record.get("exception_type");

            switch (exceptionType) {
            case "1":
                serviceTypesMap.put(date, serviceType);
                break;
            case "2":
                serviceTypesMap.remove(date, serviceType);
                break;
            default:
                throw new ScoreGeneratorFatalException(String.format(
                        "Invalid exception type %s", exceptionType));
            }
        }
    }

}
