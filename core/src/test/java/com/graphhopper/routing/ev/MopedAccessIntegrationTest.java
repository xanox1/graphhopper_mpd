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

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.OSMParsers;
import com.graphhopper.routing.util.parsers.MopedAccessParser;
import com.graphhopper.storage.IntsRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test showing MopedAccess working with EncodingManager
 */
class MopedAccessIntegrationTest {
    
    private EncodingManager encodingManager;
    private EnumEncodedValue<MopedAccess> mopedAccessEnc;
    private OSMParsers osmParsers;

    @BeforeEach
    void setUp() {
        // Create the encoding manager with moped access
        mopedAccessEnc = MopedAccess.create();
        encodingManager = EncodingManager.start()
                .add(mopedAccessEnc)
                .build();
        
        // Create OSM parsers and add moped access parser
        osmParsers = new OSMParsers();
        osmParsers.addWayTagParser(new MopedAccessParser(mopedAccessEnc));
    }

    @Test
    void testEndToEndMopedAccessParsing() {
        // Test various moped tag values
        testMopedAccessValue("yes", MopedAccess.YES);
        testMopedAccessValue("no", MopedAccess.NO);
        testMopedAccessValue("designated", MopedAccess.DESIGNATED);
        testMopedAccessValue("use_sidepath", MopedAccess.USE_SIDEPATH);
        testMopedAccessValue(null, MopedAccess.MISSING);
    }
    
    private void testMopedAccessValue(String mopedTag, MopedAccess expected) {
        ReaderWay way = new ReaderWay(1);
        if (mopedTag != null) {
            way.setTag("moped", mopedTag);
        }
        
        EdgeIntAccess edgeIntAccess = new ArrayEdgeIntAccess(1);
        IntsRef relationFlags = encodingManager.createRelationFlags();
        
        // Process the way tags through OSM parsers
        osmParsers.handleWayTags(0, edgeIntAccess, way, relationFlags);
        
        // Check that the moped access was set correctly
        MopedAccess actualAccess = mopedAccessEnc.getEnum(false, 0, edgeIntAccess);
        assertEquals(expected, actualAccess, 
                "Moped access should be " + expected + " for tag value: " + mopedTag);
    }

    @Test
    void testMopedAccessInEncodingManagerRegistry() {
        // Test that MopedAccess is properly registered and can be retrieved
        assertTrue(encodingManager.hasEncodedValue(MopedAccess.KEY));
        
        EnumEncodedValue<MopedAccess> retrievedEnc = encodingManager.getEnumEncodedValue(MopedAccess.KEY, MopedAccess.class);
        assertNotNull(retrievedEnc);
        assertEquals(MopedAccess.KEY, retrievedEnc.getName());
        assertEquals(MopedAccess.class, retrievedEnc.getEnumType());
    }
}