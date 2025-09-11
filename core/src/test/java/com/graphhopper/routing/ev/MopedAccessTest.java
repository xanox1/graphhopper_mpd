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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MopedAccessTest {

    @Test
    void testCreate() {
        EnumEncodedValue<MopedAccess> enc = MopedAccess.create();
        assertEquals("moped_access", enc.getName());
        assertEquals(MopedAccess.class, enc.getEnumType());
    }

    @Test
    void testFind() {
        assertEquals(MopedAccess.MISSING, MopedAccess.find(null));
        assertEquals(MopedAccess.MISSING, MopedAccess.find(""));
        assertEquals(MopedAccess.YES, MopedAccess.find("yes"));
        assertEquals(MopedAccess.NO, MopedAccess.find("no"));
        assertEquals(MopedAccess.DESIGNATED, MopedAccess.find("designated"));
        assertEquals(MopedAccess.USE_SIDEPATH, MopedAccess.find("use_sidepath"));
        
        // Test case insensitive
        assertEquals(MopedAccess.YES, MopedAccess.find("YES"));
        assertEquals(MopedAccess.DESIGNATED, MopedAccess.find("Designated"));
        
        // Test unknown values
        assertEquals(MopedAccess.MISSING, MopedAccess.find("unknown"));
        assertEquals(MopedAccess.MISSING, MopedAccess.find("invalid"));
    }

    @Test
    void testToString() {
        assertEquals("missing", MopedAccess.MISSING.toString());
        assertEquals("no", MopedAccess.NO.toString());
        assertEquals("yes", MopedAccess.YES.toString());
        assertEquals("designated", MopedAccess.DESIGNATED.toString());
        assertEquals("use_sidepath", MopedAccess.USE_SIDEPATH.toString());
    }
}