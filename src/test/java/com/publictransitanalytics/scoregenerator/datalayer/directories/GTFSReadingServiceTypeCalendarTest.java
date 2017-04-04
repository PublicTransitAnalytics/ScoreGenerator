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

import com.publictransitanalytics.scoregenerator.datalayer.directories.GTFSReadingServiceTypeCalendar;
import com.publictransitanalytics.scoregenerator.datalayer.directories.ServiceTypeCalendar;
import com.bitvantage.bitvantagecaching.Store;
import com.bitvantage.bitvantagecaching.mocks.MapStore;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.ServiceSet;
import com.publictransitanalytics.scoregenerator.datalayer.directories.types.keys.DateKey;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.Reader;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.HashMap;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Public Transit Analytics
 */
public class GTFSReadingServiceTypeCalendarTest {

    @Test
    public void testFindsRecuringServiceOnDay() throws Exception {
        final Store<DateKey, ServiceSet> serviceTypesStore = new MapStore<>(
                new HashMap<>());
        final Reader calendarReader = new StringReader(
                "service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date\n"
                + "79524,1,1,1,1,1,0,0,20160304,20160617");
        final Reader calendarDatesReader = new StringReader("");

        final ServiceTypeCalendar calendar = new GTFSReadingServiceTypeCalendar(
                serviceTypesStore, calendarReader, calendarDatesReader);
        final ServiceSet serviceSet = calendar.getServiceType(
                LocalDate.of(2016, Month.MARCH, 8));
        Assert.assertEquals(Collections.singleton("79524"),
                            serviceSet.getServiceCodes());

    }

    @Test
    public void testFindsAllRecuringServiceOnDay() throws Exception {
        final Store<DateKey, ServiceSet> serviceTypesStore = new MapStore<>(
                new HashMap<>());
        final Reader calendarReader = new StringReader(
                "service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date\n"
                + "79524,1,1,1,1,1,0,0,20160304,20160617\n"
                        + "13383,1,1,0,1,1,0,0,20160304,20160325\n");
        final Reader calendarDatesReader = new StringReader("");

        final ServiceTypeCalendar calendar = new GTFSReadingServiceTypeCalendar(
                serviceTypesStore, calendarReader, calendarDatesReader);
        final ServiceSet serviceSet = calendar.getServiceType(
                LocalDate.of(2016, Month.MARCH, 8));
        Assert.assertEquals(ImmutableSet.of("79524", "13383"),
                            serviceSet.getServiceCodes());
    }

    @Test
    public void testFindsNoServiceOnDay() throws Exception {
        final Store<DateKey, ServiceSet> serviceTypesStore = new MapStore<>(
                new HashMap<>());
        final Reader calendarReader = new StringReader(
                "service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date\n"
                + "79524,1,1,1,1,1,0,0,20160304,20160617");
        final Reader calendarDatesReader = new StringReader("");

        final ServiceTypeCalendar calendar = new GTFSReadingServiceTypeCalendar(
                serviceTypesStore, calendarReader, calendarDatesReader);
        final ServiceSet serviceSet = calendar.getServiceType(
                LocalDate.of(2016, Month.MARCH, 12));
        Assert.assertNull(serviceSet);

    }

    @Test
    public void testDoesNotFindCanceledServiceOnDay() throws Exception {
        final Store<DateKey, ServiceSet> serviceTypesStore = new MapStore<>(
                new HashMap<>());
        final Reader calendarReader = new StringReader(
                "service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date\n"
                + "79524,1,1,1,1,1,0,0,20160304,20160617\n"
                        + "13383,1,1,0,1,1,0,0,20160304,20160325\n");
        final Reader calendarDatesReader = new StringReader(
                "service_id,date,exception_type\n"
                        + "79524,20160308,2");

        final ServiceTypeCalendar calendar = new GTFSReadingServiceTypeCalendar(
                serviceTypesStore, calendarReader, calendarDatesReader);
        final ServiceSet serviceSet = calendar.getServiceType(
                LocalDate.of(2016, Month.MARCH, 8));
        Assert.assertEquals(Collections.singleton("13383"),
                            serviceSet.getServiceCodes());
    }

    @Test
    public void testFindsExceptionServiceOnDay() throws Exception {
        final Store<DateKey, ServiceSet> serviceTypesStore = new MapStore<>(
                new HashMap<>());
        final Reader calendarReader = new StringReader(
                "service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date\n"
                + "79524,1,1,1,1,1,0,0,20160304,20160617\n");
        final Reader calendarDatesReader = new StringReader(
                "service_id,date,exception_type\n"
                        + "79524,20160312,1");

        final ServiceTypeCalendar calendar = new GTFSReadingServiceTypeCalendar(
                serviceTypesStore, calendarReader, calendarDatesReader);
        final ServiceSet serviceSet = calendar.getServiceType(
                LocalDate.of(2016, Month.MARCH, 12));
        Assert.assertEquals(Collections.singleton("79524"),
                            serviceSet.getServiceCodes());
    }

    @Test
    public void testDoesNotRebuild() throws Exception {
        final ImmutableMap<String, ServiceSet> immutableMap = ImmutableMap.of(
                new DateKey(LocalDate.of(2016, Month.MARCH, 1)).getKeyString(),
                new ServiceSet(Collections.singleton("11111")));
        final Store<DateKey, ServiceSet> serviceTypesStore 
                = new MapStore<>(immutableMap);
        
        final Reader calendarReader = new StringReader(
                "service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date\n"
                + "79524,1,1,1,1,1,0,0,20160304,20160617\n");
        final Reader calendarDatesReader = new StringReader(
                "service_id,date,exception_type\n"
                        + "79524,20160312,1");

        final ServiceTypeCalendar calendar = new GTFSReadingServiceTypeCalendar(
                serviceTypesStore, calendarReader, calendarDatesReader);
    }

}
