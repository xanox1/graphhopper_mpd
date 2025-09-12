/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.routing.util.parsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.*;
import com.graphhopper.routing.util.WayAccess;
import com.graphhopper.util.PMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class MopedVehicleAccessParserTest {
    private MopedVehicleAccessParser parser;
    private BooleanEncodedValue accessEnc;
    private BooleanEncodedValue roundaboutEnc;

    @BeforeEach
    void setUp() {
        accessEnc = VehicleAccess.create("moped");
        roundaboutEnc = Roundabout.create();
        accessEnc.init(new EncodedValue.InitializerConfig());
        roundaboutEnc.init(new EncodedValue.InitializerConfig());
        parser = new MopedVehicleAccessParser(accessEnc, roundaboutEnc, new PMap(), 
                Arrays.asList("motorcar", "motor_vehicle", "vehicle", "access"));
    }

    @Test
    void testCyclewayWithMopedYes() {
        ReaderWay way = new ReaderWay(1);
        way.setTag("highway", "cycleway");
        way.setTag("moped", "yes");
        
        WayAccess access = parser.getAccess(way);
        assertEquals(WayAccess.WAY, access);
    }

    @Test
    void testCyclewayWithMopedDesignated() {
        ReaderWay way = new ReaderWay(1);
        way.setTag("highway", "cycleway");
        way.setTag("moped", "designated");
        
        WayAccess access = parser.getAccess(way);
        assertEquals(WayAccess.WAY, access);
    }

    @Test
    void testCyclewayWithoutMopedTag() {
        ReaderWay way = new ReaderWay(1);
        way.setTag("highway", "cycleway");
        // No moped tag - should be denied
        
        WayAccess access = parser.getAccess(way);
        assertEquals(WayAccess.CAN_SKIP, access);
    }

    @Test
    void testCyclewayWithMopedNo() {
        ReaderWay way = new ReaderWay(1);
        way.setTag("highway", "cycleway");
        way.setTag("moped", "no");
        
        WayAccess access = parser.getAccess(way);
        assertEquals(WayAccess.CAN_SKIP, access);
    }

    @Test
    void testRegularRoadAccess() {
        ReaderWay way = new ReaderWay(1);
        way.setTag("highway", "residential");
        
        WayAccess access = parser.getAccess(way);
        assertEquals(WayAccess.WAY, access);
    }

    @Test
    void testMotorwayAccess() {
        ReaderWay way = new ReaderWay(1);
        way.setTag("highway", "motorway");
        
        WayAccess access = parser.getAccess(way);
        assertEquals(WayAccess.WAY, access); // Mopeds should be able to use motorways like cars
    }
}