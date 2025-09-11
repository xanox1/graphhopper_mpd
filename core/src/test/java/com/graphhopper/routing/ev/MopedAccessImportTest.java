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

import com.graphhopper.util.PMap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MopedAccessImportTest {

    @Test
    void testImportRegistryCreatesMopedAccess() {
        DefaultImportRegistry registry = new DefaultImportRegistry();
        ImportUnit unit = registry.createImportUnit(MopedAccess.KEY);
        
        assertNotNull(unit);
        assertNotNull(unit.getCreateEncodedValue());
        assertNotNull(unit.getCreateTagParser());
        
        // Test that the encoded value is properly created
        EncodedValue encodedValue = unit.getCreateEncodedValue().apply(new PMap());
        assertTrue(encodedValue instanceof EnumEncodedValue);
        assertEquals(MopedAccess.KEY, encodedValue.getName());
        
        EnumEncodedValue<MopedAccess> mopedAccessEnc = (EnumEncodedValue<MopedAccess>) encodedValue;
        assertEquals(MopedAccess.class, mopedAccessEnc.getEnumType());
    }
}