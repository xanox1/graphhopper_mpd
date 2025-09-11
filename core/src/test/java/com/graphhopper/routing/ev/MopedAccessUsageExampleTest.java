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

import com.graphhopper.routing.util.EncodingManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Usage example showing how to configure GraphHopper to use MopedAccess
 */
class MopedAccessUsageExampleTest {

    @Test
    void demonstrateBasicMopedAccessUsage() {
        // Step 1: Create an EncodingManager with MopedAccess support
        EncodingManager encodingManager = EncodingManager.start()
                .add(MopedAccess.create())  // Add moped access encoded value
                .build();
        
        // Step 2: Verify moped access is available
        assertTrue(encodingManager.hasEncodedValue(MopedAccess.KEY));
        
        // Step 3: Retrieve the moped access encoded value for use in routing
        EnumEncodedValue<MopedAccess> mopedAccessEnc = 
            encodingManager.getEnumEncodedValue(MopedAccess.KEY, MopedAccess.class);
        
        assertNotNull(mopedAccessEnc);
        assertEquals("moped_access", mopedAccessEnc.getName());
        
        // This encoded value can now be used in custom routing profiles
        // to check if mopeds are allowed on specific ways
    }

    @Test
    void demonstrateImportRegistryUsage() {
        // The DefaultImportRegistry automatically registers MopedAccess
        DefaultImportRegistry registry = new DefaultImportRegistry();
        ImportUnit unit = registry.createImportUnit(MopedAccess.KEY);
        
        assertNotNull(unit, "MopedAccess should be registered in DefaultImportRegistry");
        
        // This means that in GraphHopper configuration files, you can now include:
        // graph.encoded_values: moped_access
        // And the system will automatically parse "moped" OSM tags
    }
}

/**
 * Configuration example (not a test, just documentation):
 * 
 * To use MopedAccess in a real GraphHopper configuration:
 * 
 * 1. In your GraphHopper config file, add moped_access to encoded values:
 *    graph.encoded_values: car_access,car_average_speed,moped_access
 * 
 * 2. The system will automatically:
 *    - Create the MopedAccess EncodedValue 
 *    - Create the MopedAccessParser to read "moped" OSM tags
 *    - Parse OSM data and populate moped access information
 * 
 * 3. In custom vehicle profiles or routing logic, you can then:
 *    - Check moped access permissions: mopedAccessEnc.getEnum(false, edgeId, edgeIntAccess)
 *    - Make routing decisions based on moped=yes/no/designated/use_sidepath values
 *    - Handle cycleway access properly for mopeds (future enhancement)
 */