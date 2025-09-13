package com.graphhopper.routing.ev;

import com.graphhopper.json.Statement;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.weighting.custom.CustomModelParser;
import com.graphhopper.util.CustomModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that reproduces the exact scenario from the problem statement:
 * 
 * java.lang.IllegalArgumentException: Could not create weighting for profile: 'moped_nl'.
 * Error: Cannot compile expression: File 'priority entry', Line 1, Column 6: Not a boolean expression
 * 
 * The issue was caused by using !moped_access where moped_access is an enum, not a boolean.
 */
class MopedProfileCompilationTest {

    @Test
    void testOriginalProblemScenario() {
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
        
        // This is the problematic expression that caused the original error
        problematicModel.addToPriority(Statement.If("!moped_access", Statement.Op.MULTIPLY, "0")); // This should fail
        problematicModel.addToPriority(Statement.If("road_access == DESTINATION || road_access == PRIVATE", Statement.Op.MULTIPLY, "0.1"));
        problematicModel.addToPriority(Statement.If("moped_access == YES || moped_access == DESIGNATED", Statement.Op.MULTIPLY, "1.2"));
        problematicModel.addToPriority(Statement.If("road_class == CYCLEWAY && moped_access == YES", Statement.Op.MULTIPLY, "1.0"));
        problematicModel.addToPriority(Statement.If("road_class == TRACK", Statement.Op.MULTIPLY, "0.3"));
        
        problematicModel.addToSpeed(Statement.If("true", Statement.Op.LIMIT, "car_average_speed"));
        problematicModel.addToSpeed(Statement.If("true", Statement.Op.LIMIT, "45"));
        problematicModel.addToSpeed(Statement.If("road_class == RESIDENTIAL", Statement.Op.LIMIT, "30"));
        problematicModel.addToSpeed(Statement.If("road_class == LIVING_STREET", Statement.Op.LIMIT, "15"));
        problematicModel.addToSpeed(Statement.If("road_class == CYCLEWAY", Statement.Op.LIMIT, "25"));

        // This should fail with the original error  
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            CustomModelParser.createWeighting(encodingManager, null, problematicModel);
        });
        
        // Verify the error message mentions the compilation issue
        assertTrue(exception.getMessage().contains("Cannot compile expression") || 
                  exception.getMessage().contains("Not a boolean expression") ||
                  exception.getMessage().contains("invalid condition"),
                  "Expected compilation error, but got: " + exception.getMessage());
    }

    @Test
    void testFixedScenario() {
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
        
        // This is the corrected expression using proper enum comparisons
        fixedModel.addToPriority(Statement.If("moped_access == MISSING || moped_access == NO", Statement.Op.MULTIPLY, "0")); // Fixed!
        fixedModel.addToPriority(Statement.If("road_access == DESTINATION || road_access == PRIVATE", Statement.Op.MULTIPLY, "0.1"));
        fixedModel.addToPriority(Statement.If("moped_access == YES || moped_access == DESIGNATED", Statement.Op.MULTIPLY, "1.2"));
        fixedModel.addToPriority(Statement.If("road_class == CYCLEWAY && moped_access == YES", Statement.Op.MULTIPLY, "1.0"));
        fixedModel.addToPriority(Statement.If("road_class == TRACK", Statement.Op.MULTIPLY, "0.3"));
        
        fixedModel.addToSpeed(Statement.If("true", Statement.Op.LIMIT, "car_average_speed"));
        fixedModel.addToSpeed(Statement.If("true", Statement.Op.LIMIT, "45"));
        fixedModel.addToSpeed(Statement.If("road_class == RESIDENTIAL", Statement.Op.LIMIT, "30"));
        fixedModel.addToSpeed(Statement.If("road_class == LIVING_STREET", Statement.Op.LIMIT, "15"));
        fixedModel.addToSpeed(Statement.If("road_class == CYCLEWAY", Statement.Op.LIMIT, "25"));

        // This should work without throwing an exception
        assertDoesNotThrow(() -> {
            CustomModelParser.createWeighting(encodingManager, null, fixedModel);
        });
    }

    @Test
    void testMopedAccessEnumValues() {
        // Verify all expected enum values are available
        assertEquals(MopedAccess.MISSING, MopedAccess.find(null));
        assertEquals(MopedAccess.MISSING, MopedAccess.find(""));
        assertEquals(MopedAccess.MISSING, MopedAccess.find("unknown"));
        assertEquals(MopedAccess.NO, MopedAccess.find("no"));
        assertEquals(MopedAccess.YES, MopedAccess.find("yes"));
        assertEquals(MopedAccess.DESIGNATED, MopedAccess.find("designated"));
        assertEquals(MopedAccess.USE_SIDEPATH, MopedAccess.find("use_sidepath"));
    }
}