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
package com.graphhopper.routing.ev;

import com.graphhopper.json.Statement;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.weighting.custom.CustomModelParser;
import com.graphhopper.util.CustomModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to reproduce and fix the moped_access validation issue
 */
class MopedAccessValidationTest {

    @Test
    void testMopedAccessInCustomModel() {
        // Create encoding manager with moped access support like in the moped_nl profile
        EncodingManager encodingManager = EncodingManager.start()
                .add(MopedAccess.create()) // This should create "moped_access" encoded value
                .add(VehicleAccess.create("car")) // For completeness
                .add(new DecimalEncodedValueImpl("car_average_speed", 7, 2, true))
                .add(RoadClass.create())
                .add(RoadAccess.create())
                .build();
        
        // Verify moped_access is available
        assertTrue(encodingManager.hasEncodedValue(MopedAccess.KEY));
        assertNotNull(encodingManager.getEnumEncodedValue(MopedAccess.KEY, MopedAccess.class));

        // Create custom model similar to moped_nl_model.json (corrected)
        CustomModel customModel = new CustomModel();
        customModel.addToPriority(Statement.If("moped_access == MISSING || moped_access == NO", Statement.Op.MULTIPLY, "0"));
        customModel.addToPriority(Statement.If("moped_access == YES || moped_access == DESIGNATED", Statement.Op.MULTIPLY, "1.2"));
        customModel.addToSpeed(Statement.If("true", Statement.Op.LIMIT, "car_average_speed"));
        customModel.addToSpeed(Statement.If("true", Statement.Op.LIMIT, "45"));
        
        // This should work without throwing an exception
        assertDoesNotThrow(() -> {
            CustomModelParser.createWeighting(encodingManager, null, customModel);
        });
    }

    @Test
    void testInvalidBooleanNegationOnMopedAccessEnum() {
        // Create encoding manager with moped access support
        EncodingManager encodingManager = EncodingManager.start()
                .add(MopedAccess.create()) 
                .add(VehicleAccess.create("car"))
                .add(new DecimalEncodedValueImpl("car_average_speed", 7, 2, true))
                .build();

        // Create custom model with incorrect boolean negation on enum
        CustomModel customModel = new CustomModel();
        customModel.addToPriority(Statement.If("!moped_access", Statement.Op.MULTIPLY, "0")); // This should fail
        customModel.addToSpeed(Statement.If("true", Statement.Op.LIMIT, "car_average_speed"));
        
        // This should throw an exception due to incorrect boolean operation on enum
        assertThrows(IllegalArgumentException.class, () -> {
            CustomModelParser.createWeighting(encodingManager, null, customModel);
        });
    }
}