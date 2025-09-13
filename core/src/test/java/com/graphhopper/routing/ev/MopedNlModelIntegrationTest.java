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
 * Integration test to verify the fixed moped_nl_model.json logic works correctly
 */
class MopedNlModelIntegrationTest {

    @Test
    void testCompleteCustomModelLikeMopedNl() {
        // Create encoding manager with all required encoded values for moped_nl profile
        EncodingManager encodingManager = EncodingManager.start()
                .add(MopedAccess.create()) // moped_access
                .add(VehicleAccess.create("car")) // car_access 
                .add(new DecimalEncodedValueImpl("car_average_speed", 7, 2, true))
                .add(RoadAccess.create()) // road_access
                .add(RoadClass.create()) // road_class
                .build();
        
        // Create the complete custom model matching moped_nl_model.json structure
        CustomModel customModel = new CustomModel();
        customModel.setDistanceInfluence(90.0);
        
        // Priority rules (corrected)
        customModel.addToPriority(Statement.If("moped_access == MISSING || moped_access == NO", Statement.Op.MULTIPLY, "0"));
        customModel.addToPriority(Statement.If("road_access == DESTINATION || road_access == PRIVATE", Statement.Op.MULTIPLY, "0.1"));
        customModel.addToPriority(Statement.If("moped_access == YES || moped_access == DESIGNATED", Statement.Op.MULTIPLY, "1.2"));
        customModel.addToPriority(Statement.If("road_class == CYCLEWAY && moped_access == YES", Statement.Op.MULTIPLY, "1.0"));
        customModel.addToPriority(Statement.If("road_class == TRACK", Statement.Op.MULTIPLY, "0.3"));
        
        // Speed rules
        customModel.addToSpeed(Statement.If("true", Statement.Op.LIMIT, "car_average_speed"));
        customModel.addToSpeed(Statement.If("true", Statement.Op.LIMIT, "45"));
        customModel.addToSpeed(Statement.If("road_class == RESIDENTIAL", Statement.Op.LIMIT, "30"));
        customModel.addToSpeed(Statement.If("road_class == LIVING_STREET", Statement.Op.LIMIT, "15"));
        customModel.addToSpeed(Statement.If("road_class == CYCLEWAY", Statement.Op.LIMIT, "25"));
        
        // This should work without throwing an exception
        assertDoesNotThrow(() -> {
            CustomModelParser.createWeighting(encodingManager, null, customModel);
        });
    }
}