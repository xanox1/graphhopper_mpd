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
 * Test that reproduces the exact scenario from the problem statement
 */
class MopedProfileValidationTest {

    @Test
    void testOriginalProblemScenario() {
        // This recreates the scenario from the problem statement:
        // Error: Cannot compile expression: priority entry invalid condition "!moped_access": 'moped_access' not available
        
        // Create encoding manager with the encoded values from the configuration
        EncodingManager encodingManager = EncodingManager.start()
                .add(MopedAccess.create()) // moped_access (enum)
                .add(VehicleAccess.create("car")) // car_access 
                .add(new DecimalEncodedValueImpl("car_average_speed", 7, 2, true))
                .add(RoadAccess.create()) // road_access
                .add(RoadClass.create()) // road_class
                .build();

        // Create the problematic custom model (from the original error)
        CustomModel problematicModel = new CustomModel();
        problematicModel.setDistanceInfluence(90.0);
        problematicModel.addToPriority(Statement.If("!moped_access", Statement.Op.MULTIPLY, "0")); // This would fail
        problematicModel.addToPriority(Statement.If("road_access == DESTINATION || road_access == PRIVATE", Statement.Op.MULTIPLY, "0.1"));
        problematicModel.addToSpeed(Statement.If("true", Statement.Op.LIMIT, "car_average_speed"));
        problematicModel.addToSpeed(Statement.If("true", Statement.Op.LIMIT, "45"));

        // This should fail with the original error  
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            CustomModelParser.createWeighting(encodingManager, null, problematicModel);
        });
        
        assertTrue(exception.getMessage().contains("Cannot compile expression") || 
                  exception.getMessage().contains("Not a boolean expression"));
    }

    @Test
    void testFixedScenario() {
        // This tests the fixed version that should work
        
        // Create encoding manager with the encoded values from the configuration
        EncodingManager encodingManager = EncodingManager.start()
                .add(MopedAccess.create()) // moped_access (enum)
                .add(VehicleAccess.create("car")) // car_access 
                .add(new DecimalEncodedValueImpl("car_average_speed", 7, 2, true))
                .add(RoadAccess.create()) // road_access
                .add(RoadClass.create()) // road_class
                .build();

        // Create the fixed custom model
        CustomModel fixedModel = new CustomModel();
        fixedModel.setDistanceInfluence(90.0);
        fixedModel.addToPriority(Statement.If("moped_access == MISSING || moped_access == NO", Statement.Op.MULTIPLY, "0")); // Fixed!
        fixedModel.addToPriority(Statement.If("road_access == DESTINATION || road_access == PRIVATE", Statement.Op.MULTIPLY, "0.1"));
        fixedModel.addToPriority(Statement.If("moped_access == YES || moped_access == DESIGNATED", Statement.Op.MULTIPLY, "1.2"));
        fixedModel.addToSpeed(Statement.If("true", Statement.Op.LIMIT, "car_average_speed"));
        fixedModel.addToSpeed(Statement.If("true", Statement.Op.LIMIT, "45"));

        // This should work without throwing an exception
        assertDoesNotThrow(() -> {
            CustomModelParser.createWeighting(encodingManager, null, fixedModel);
        });
    }
}