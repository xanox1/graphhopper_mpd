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
package com.graphhopper.application.resources;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.ResponsePath;
import com.graphhopper.api.GraphHopperWeb;
import com.graphhopper.application.util.ExternalEndpointTestUtils;
import com.graphhopper.util.shapes.GHPoint;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dedicated external endpoint tests for the moped_nl profile.
 * This test class specifically targets the production endpoint and moped routing functionality.
 */
public class ExternalEndpointMopedTest {

    @BeforeAll
    public static void checkExternalEndpoint() {
        String externalEndpoint = ExternalEndpointTestUtils.getExternalEndpointWithDefault();
        if (externalEndpoint != null) {
            System.out.println("Testing external endpoint: " + externalEndpoint);
            System.out.println("External endpoint testing enabled. Tests will attempt to connect to: " + externalEndpoint);
        } else {
            System.out.println("Skipping external endpoint tests - no external endpoint configured");
        }
    }

    private GraphHopperWeb createGH() {
        String externalEndpoint = ExternalEndpointTestUtils.getExternalEndpointWithDefault();
        if (externalEndpoint == null) {
            throw new RuntimeException("External endpoint testing not configured. Use -Pexternal-endpoint-test or set test.external.endpoint property.");
        }
        String routingUrl = ExternalEndpointTestUtils.buildRoutingUrl(externalEndpoint);
        return new GraphHopperWeb(routingUrl);
    }

    @Test
    public void testMopedRouting() {
        GraphHopperWeb gh = createGH();
        
        // Test with Netherlands coordinates (moped routing is optimized for Netherlands)
        GHRequest req = new GHRequest()
                .addPoint(new GHPoint(52.370216, 4.895168))  // Amsterdam
                .addPoint(new GHPoint(52.520008, 6.083887))  // Groningen
                .setProfile("moped_nl")
                .putHint("elevation", false)
                .putHint("instructions", true)
                .putHint("calc_points", true);
        
        GHResponse rsp = gh.route(req);
        
        // Do not ignore routing errors - routing should work for Netherlands coordinates
        assertFalse(rsp.hasErrors(), "Moped routing should succeed for Amsterdam to Groningen. Errors: " + rsp.getErrors().toString());
        ResponsePath res = rsp.getBest();
        assertTrue(res.getDistance() > 0, "Moped route should have positive distance");
        assertTrue(res.getTime() > 0, "Moped route should have positive time");
        
        System.out.println("✅ Moped routing test passed:");
        System.out.println("   Distance: " + res.getDistance() + "m");
        System.out.println("   Time: " + res.getTime() + "ms");
        System.out.println("   Points: " + res.getPoints().size());
    }

    @Test
    public void testMopedRoutingShortRoute() {
        GraphHopperWeb gh = createGH();
        
        // Test with closer Netherlands coordinates for a shorter route
        GHRequest req = new GHRequest()
                .addPoint(new GHPoint(52.3676, 4.9041))   // Amsterdam center
                .addPoint(new GHPoint(52.3702, 4.8952))   // Amsterdam nearby
                .setProfile("moped_nl")
                .putHint("elevation", false)
                .putHint("instructions", true)
                .putHint("calc_points", true);
        
        GHResponse rsp = gh.route(req);
        
        // Do not ignore routing errors - routing should work for Amsterdam coordinates
        assertFalse(rsp.hasErrors(), "Short moped routing should succeed within Amsterdam. Errors: " + rsp.getErrors().toString());
        ResponsePath res = rsp.getBest();
        assertTrue(res.getDistance() > 0, "Short moped route should have positive distance");
        assertTrue(res.getTime() > 0, "Short moped route should have positive time");
        
        System.out.println("✅ Short moped routing test passed:");
        System.out.println("   Distance: " + res.getDistance() + "m");
        System.out.println("   Time: " + res.getTime() + "ms");
    }

    @Test 
    public void testOverijsselselaanAvoidance() {
        GraphHopperWeb gh = createGH();
        
        // Test the specific route that should avoid Overijsselselaan
        // Using the coordinates provided by the user: 53.11684,5.782628 to 53.21662,5.80059
        GHRequest req = new GHRequest()
                .addPoint(new GHPoint(53.11684, 5.782628))
                .addPoint(new GHPoint(53.21662, 5.80059))
                .setProfile("moped_nl")
                .putHint("elevation", false)
                .putHint("instructions", true)
                .putHint("calc_points", true);
        
        GHResponse rsp = gh.route(req);
        
        // Do not ignore routing errors - this route should be possible
        assertFalse(rsp.hasErrors(), "Routing should succeed for coordinates 53.11684,5.782628 to 53.21662,5.80059. Errors: " + rsp.getErrors().toString());
        
        ResponsePath res = rsp.getBest();
        assertTrue(res.getDistance() > 0, "Overijsselselaan avoidance route should have positive distance");
        assertTrue(res.getTime() > 0, "Overijsselselaan avoidance route should have positive time");
        
        // Check that the route does not use Overijsselselaan
        String routeInstructions = res.getInstructions().toString().toLowerCase();
        assertFalse(routeInstructions.contains("overijsselselaan"), 
                "Route should not use Overijsselselaan for these coordinates. Route instructions: " + routeInstructions);
        
        System.out.println("✅ Overijsselselaan avoidance test passed:");
        System.out.println("   Distance: " + res.getDistance() + "m");
        System.out.println("   Time: " + res.getTime() + "ms");
        System.out.println("   Route correctly avoids Overijsselselaan");
    }
}