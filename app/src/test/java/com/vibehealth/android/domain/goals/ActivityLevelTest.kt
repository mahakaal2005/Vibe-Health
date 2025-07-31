package com.vibehealth.android.domain.goals

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for ActivityLevel enum.
 * 
 * Tests verify activity factors, descriptions, and default values
 * align with WHO guidelines and Harris-Benedict equation standards.
 */
class ActivityLevelTest {
    
    @Test
    fun shouldHaveCorrectActivityFactors() {
        // Verify activity factors match Harris-Benedict equation standards
        assertEquals(1.2, ActivityLevel.SEDENTARY.factor)
        assertEquals(1.375, ActivityLevel.LIGHT.factor)
        assertEquals(1.55, ActivityLevel.MODERATE.factor)
        assertEquals(1.725, ActivityLevel.ACTIVE.factor)
        assertEquals(1.9, ActivityLevel.VERY_ACTIVE.factor)
    }
    
    @Test
    fun shouldHaveDescriptiveNames() {
        // Verify all activity levels have meaningful descriptions
        assertEquals("Little to no exercise, desk job", ActivityLevel.SEDENTARY.description)
        assertEquals("Light exercise 1-3 days/week", ActivityLevel.LIGHT.description)
        assertEquals("Moderate exercise 3-5 days/week", ActivityLevel.MODERATE.description)
        assertEquals("Heavy exercise 6-7 days/week", ActivityLevel.ACTIVE.description)
        assertEquals("Very heavy exercise, physical job", ActivityLevel.VERY_ACTIVE.description)
    }
    
    @Test
    fun shouldHaveCorrectDefaultForUrbanProfessionals() {
        // Verify default activity level is appropriate for target users
        assertEquals(ActivityLevel.LIGHT, ActivityLevel.DEFAULT_FOR_URBAN_PROFESSIONALS)
    }
    
    @Test
    fun shouldHaveIncreasingActivityFactors() {
        // Verify activity factors increase with activity level
        val factors = listOf(
            ActivityLevel.SEDENTARY.factor,
            ActivityLevel.LIGHT.factor,
            ActivityLevel.MODERATE.factor,
            ActivityLevel.ACTIVE.factor,
            ActivityLevel.VERY_ACTIVE.factor
        )
        
        // Check that each factor is greater than the previous
        for (i in 1 until factors.size) {
            assertTrue(
                "Activity factor should increase with activity level",
                factors[i] > factors[i - 1]
            )
        }
    }
    
    @Test
    fun shouldHaveReasonableFactorRanges() {
        // Verify activity factors are within medically reasonable ranges
        ActivityLevel.values().forEach { level ->
            assertTrue(
                "Activity factor for ${level.name} should be between 1.0 and 2.5",
                level.factor in 1.0..2.5
            )
        }
    }
    
    @Test
    fun shouldHaveAllRequiredActivityLevels() {
        // Verify all expected activity levels are present
        val expectedLevels = setOf(
            "SEDENTARY",
            "LIGHT", 
            "MODERATE",
            "ACTIVE",
            "VERY_ACTIVE"
        )
        
        val actualLevels = ActivityLevel.values().map { it.name }.toSet()
        
        assertEquals(expectedLevels, actualLevels)
    }
    
    @Test
    fun shouldProvideConsistentEnumOrdering() {
        // Verify enum values are in logical order from least to most active
        val levels = ActivityLevel.values()
        
        assertEquals(ActivityLevel.SEDENTARY, levels[0])
        assertEquals(ActivityLevel.LIGHT, levels[1])
        assertEquals(ActivityLevel.MODERATE, levels[2])
        assertEquals(ActivityLevel.ACTIVE, levels[3])
        assertEquals(ActivityLevel.VERY_ACTIVE, levels[4])
    }
    
    @Test
    fun shouldHaveNonEmptyDescriptions() {
        // Verify all activity levels have non-empty descriptions
        ActivityLevel.values().forEach { level ->
            assertTrue(
                "Activity level ${level.name} should have non-empty description",
                level.description.isNotBlank()
            )
        }
    }
    
    @Test
    fun shouldHaveUniqueFactors() {
        // Verify all activity factors are unique
        val factors = ActivityLevel.values().map { it.factor }
        val uniqueFactors = factors.toSet()
        
        assertEquals(
            factors.size,
            uniqueFactors.size,
            "All activity factors should be unique"
        )
    }
    
    @Test
    fun shouldHaveUniqueDescriptions() {
        // Verify all descriptions are unique
        val descriptions = ActivityLevel.values().map { it.description }
        val uniqueDescriptions = descriptions.toSet()
        
        assertEquals(
            descriptions.size,
            uniqueDescriptions.size,
            "All activity descriptions should be unique"
        )
    }
}