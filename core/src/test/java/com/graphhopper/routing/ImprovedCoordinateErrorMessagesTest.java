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
package com.graphhopper.routing;

import com.graphhopper.GraphHopper;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.util.Helper;
import com.graphhopper.util.exceptions.PointNotFoundException;
import com.graphhopper.util.shapes.GHPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that validates the improved error messages for "coordinates not found" scenarios.
 * This test ensures that users receive helpful, actionable error messages when coordinates
 * cannot be snapped to the road network.
 */
public class ImprovedCoordinateErrorMessagesTest {

    private static final String ANDORRA = "../core/files/andorra.osm.pbf";
    private static final String GH_LOCATION = "target/improved-coordinate-test-gh";
    private GraphHopper hopper;

    @BeforeEach
    public void setUp() {
        hopper = new GraphHopper().
                setGraphHopperLocation(GH_LOCATION).
                setOSMFile(ANDORRA).
                setStoreOnFlush(false).
                setEncodedValuesString("car_access,car_average_speed").
                setProfiles(Arrays.asList(TestProfiles.accessAndSpeed("car"))).
                importOrLoad();
    }

    @AfterEach
    public void tearDown() {
        Helper.removeDir(new File(GH_LOCATION));
    }

    @Test
    public void testImprovedErrorMessageContent() {
        // Use coordinates from the existing web test that are known to fail
        GHRequest req = new GHRequest().
                addPoint(new GHPoint(42.49058, 1.602974)). // Known problematic coordinate from Andorra test
                addPoint(new GHPoint(42.510383, 1.533392)). // Valid Andorra point
                setProfile("car");

        GHResponse rsp = hopper.route(req);
        assertTrue(rsp.hasErrors(), "Expected errors for coordinates that cannot be found");

        // Find the PointNotFoundException
        PointNotFoundException pnfe = null;
        for (Throwable error : rsp.getErrors()) {
            if (error instanceof PointNotFoundException) {
                pnfe = (PointNotFoundException) error;
                break;
            }
        }

        assertNotNull(pnfe, "Should have found PointNotFoundException");
        
        String message = pnfe.getMessage();
        System.out.println("Improved error message: " + message);

        // Verify the improved error message contains helpful information
        assertTrue(message.contains("Cannot find a route to/from point"), 
                   "Error message should explain the routing problem: " + message);
        
        assertTrue(message.contains("too far from the nearest road"), 
                   "Error message should explain the likely cause: " + message);
        
        assertTrue(message.contains("Please ensure the coordinate"), 
                   "Error message should provide actionable guidance: " + message);
        
        assertTrue(message.contains("accessible for the selected profile"), 
                   "Error message should mention the profile: " + message);
        
        assertTrue(message.contains("'car'"), 
                   "Error message should specify the actual profile used: " + message);
        
        // Verify the point index is correctly included
        assertEquals(0, pnfe.getPointIndex(), "Point index should be correct");
        
        // The error message should be more helpful than the old one
        assertFalse(message.equals("Cannot find point 0: 42.49058,1.602974"), 
                    "Error message should be improved from the old format");
    }

    @Test
    public void testErrorMessageIncludesCorrectCoordinates() {
        // Test with a different profile to ensure profile name is included correctly
        GraphHopper bikeHopper = new GraphHopper().
                setGraphHopperLocation(GH_LOCATION + "_bike").
                setOSMFile(ANDORRA).
                setStoreOnFlush(false).
                setEncodedValuesString("bike_access,bike_priority,bike_average_speed").
                setProfiles(Arrays.asList(TestProfiles.accessSpeedAndPriority("bike", "bike"))).
                importOrLoad();

        try {
            GHRequest req = new GHRequest().
                    addPoint(new GHPoint(42.49058, 1.602974)).
                    addPoint(new GHPoint(42.510383, 1.533392)).
                    setProfile("bike");

            GHResponse rsp = bikeHopper.route(req);
            
            if (rsp.hasErrors()) {
                for (Throwable error : rsp.getErrors()) {
                    if (error instanceof PointNotFoundException) {
                        String message = error.getMessage();
                        System.out.println("Bike profile error message: " + message);
                        
                        // Should include the specific coordinates
                        assertTrue(message.contains("42.49058,1.602974"), 
                                   "Error message should include the problematic coordinates: " + message);
                        
                        // Should mention bike profile
                        assertTrue(message.contains("'bike'"), 
                                   "Error message should mention the bike profile: " + message);
                        
                        break;
                    }
                }
            }
        } finally {
            Helper.removeDir(new File(GH_LOCATION + "_bike"));
        }
    }

    @Test
    public void testValidCoordinatesShouldNotGenerateErrors() {
        // Test with coordinates that should work in Andorra to ensure we don't break valid routing
        GHRequest req = new GHRequest().
                addPoint(new GHPoint(42.510383, 1.533392)). // Valid Andorra coordinates
                addPoint(new GHPoint(42.511383, 1.534392)).
                setProfile("car");

        GHResponse rsp = hopper.route(req);
        
        if (rsp.hasErrors()) {
            System.out.println("Unexpected errors with valid coordinates:");
            for (Throwable error : rsp.getErrors()) {
                System.out.println("  " + error.getClass().getSimpleName() + ": " + error.getMessage());
            }
        }
        
        // Valid coordinates should not produce coordinate-related errors
        assertFalse(rsp.hasErrors(), "Valid Andorra coordinates should not produce errors");
        assertTrue(rsp.getBest().getDistance() > 0, "Should have a valid route");
    }
}