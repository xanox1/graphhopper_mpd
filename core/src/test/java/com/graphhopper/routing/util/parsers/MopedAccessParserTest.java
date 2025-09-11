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
import com.graphhopper.storage.IntsRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MopedAccessParserTest {
    private MopedAccessParser parser;
    private EnumEncodedValue<MopedAccess> mopedAccessEnc;

    @BeforeEach
    void setUp() {
        mopedAccessEnc = MopedAccess.create();
        mopedAccessEnc.init(new EncodedValue.InitializerConfig());
        parser = new MopedAccessParser(mopedAccessEnc);
    }

    @Test
    void testHandleWayTagsWithYes() {
        ReaderWay way = new ReaderWay(1);
        way.setTag("moped", "yes");
        
        EdgeIntAccess edgeIntAccess = new ArrayEdgeIntAccess(1);
        int edgeId = 0;
        parser.handleWayTags(edgeId, edgeIntAccess, way, new IntsRef(2));
        
        assertEquals(MopedAccess.YES, mopedAccessEnc.getEnum(false, edgeId, edgeIntAccess));
    }

    @Test
    void testHandleWayTagsWithNo() {
        ReaderWay way = new ReaderWay(1);
        way.setTag("moped", "no");
        
        EdgeIntAccess edgeIntAccess = new ArrayEdgeIntAccess(1);
        int edgeId = 0;
        parser.handleWayTags(edgeId, edgeIntAccess, way, new IntsRef(2));
        
        assertEquals(MopedAccess.NO, mopedAccessEnc.getEnum(false, edgeId, edgeIntAccess));
    }

    @Test
    void testHandleWayTagsWithDesignated() {
        ReaderWay way = new ReaderWay(1);
        way.setTag("moped", "designated");
        
        EdgeIntAccess edgeIntAccess = new ArrayEdgeIntAccess(1);
        int edgeId = 0;
        parser.handleWayTags(edgeId, edgeIntAccess, way, new IntsRef(2));
        
        assertEquals(MopedAccess.DESIGNATED, mopedAccessEnc.getEnum(false, edgeId, edgeIntAccess));
    }

    @Test
    void testHandleWayTagsWithUseSidepath() {
        ReaderWay way = new ReaderWay(1);
        way.setTag("moped", "use_sidepath");
        
        EdgeIntAccess edgeIntAccess = new ArrayEdgeIntAccess(1);
        int edgeId = 0;
        parser.handleWayTags(edgeId, edgeIntAccess, way, new IntsRef(2));
        
        assertEquals(MopedAccess.USE_SIDEPATH, mopedAccessEnc.getEnum(false, edgeId, edgeIntAccess));
    }

    @Test
    void testHandleWayTagsWithoutMopedTag() {
        ReaderWay way = new ReaderWay(1);
        // No moped tag set
        
        EdgeIntAccess edgeIntAccess = new ArrayEdgeIntAccess(1);
        int edgeId = 0;
        parser.handleWayTags(edgeId, edgeIntAccess, way, new IntsRef(2));
        
        // Should remain at default value (0, which corresponds to MISSING)
        assertEquals(MopedAccess.MISSING, mopedAccessEnc.getEnum(false, edgeId, edgeIntAccess));
    }

    @Test
    void testHandleWayTagsWithInvalidValue() {
        ReaderWay way = new ReaderWay(1);
        way.setTag("moped", "invalid_value");
        
        EdgeIntAccess edgeIntAccess = new ArrayEdgeIntAccess(1);
        int edgeId = 0;
        parser.handleWayTags(edgeId, edgeIntAccess, way, new IntsRef(2));
        
        assertEquals(MopedAccess.MISSING, mopedAccessEnc.getEnum(false, edgeId, edgeIntAccess));
    }
}