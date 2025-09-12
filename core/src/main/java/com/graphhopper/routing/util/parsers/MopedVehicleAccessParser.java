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
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.util.TransportationMode;
import com.graphhopper.routing.util.WayAccess;
import com.graphhopper.util.PMap;

import java.util.Arrays;

/**
 * Vehicle access parser for moped routing that extends car access logic
 * but allows cycleways when mopeds are explicitly permitted.
 */
public class MopedVehicleAccessParser extends CarAccessParser {

    public MopedVehicleAccessParser(EncodedValueLookup lookup, PMap properties) {
        this(
                lookup.getBooleanEncodedValue(VehicleAccess.key("moped")),
                lookup.getBooleanEncodedValue("roundabout"),
                properties,
                OSMRoadAccessParser.toOSMRestrictions(TransportationMode.CAR)
        );
    }

    public MopedVehicleAccessParser(BooleanEncodedValue accessEnc,
                                   BooleanEncodedValue roundaboutEnc, PMap properties,
                                   java.util.List<String> restrictionsKeys) {
        super(accessEnc, roundaboutEnc, properties, restrictionsKeys);
        
        // Add cycleway to allowed highways for mopeds (unlike cars)
        highwayValues.add("cycleway");
    }

    @Override
    public WayAccess getAccess(ReaderWay way) {
        String highwayValue = way.getTag("highway");
        
        // Special handling for cycleways - only allow if moped is explicitly permitted
        if ("cycleway".equals(highwayValue)) {
            String mopedTag = way.getTag("moped");
            if ("yes".equals(mopedTag) || "designated".equals(mopedTag)) {
                // Allow access to this cycleway since moped is explicitly permitted
                return WayAccess.WAY;
            } else {
                // Deny access to this cycleway since moped is not explicitly permitted
                return WayAccess.CAN_SKIP;
            }
        }
        
        // For all other highway types, use the standard car access logic
        return super.getAccess(way);
    }
}