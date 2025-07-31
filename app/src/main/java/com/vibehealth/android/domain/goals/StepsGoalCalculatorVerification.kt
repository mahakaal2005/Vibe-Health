package com.vibehealth.android.domain.goals

import com.vibehealth.android.domain.user.Gender

/**
 * Simple verification class to test StepsGoalCalculator functionality.
 * This can be used to manually verify the implementation works correctly.
 */
object StepsGoalCalculatorVerification {
    
    fun runVerification(): String {
        val calculator = StepsGoalCalculator()
        val results = mutableListOf<String>()
        
        results.add("=== StepsGoalCalculator Verification ===")
        
        // Test 1: Typical adult male
        val adultMale = GoalCalculationInput(
            age = 30,
            gender = Gender.MALE,
            heightInCm = 175,
            weightInKg = 70.0
        )
        val maleResult = calculator.calculateStepsGoal(adultMale)
        results.add("Adult Male (30y): $maleResult steps (expected: 10500)")
        
        // Test 2: Typical adult female
        val adultFemale = GoalCalculationInput(
            age = 28,
            gender = Gender.FEMALE,
            heightInCm = 165,
            weightInKg = 60.0
        )
        val femaleResult = calculator.calculateStepsGoal(adultFemale)
        results.add("Adult Female (28y): $femaleResult steps (expected: 9500)")
        
        // Test 3: Youth
        val youth = GoalCalculationInput(
            age = 16,
            gender = Gender.OTHER,
            heightInCm = 170,
            weightInKg = 60.0
        )
        val youthResult = calculator.calculateStepsGoal(youth)
        results.add("Youth (16y): $youthResult steps (expected: 12000)")
        
        // Test 4: Older adult
        val olderAdult = GoalCalculationInput(
            age = 70,
            gender = Gender.OTHER,
            heightInCm = 170,
            weightInKg = 65.0
        )
        val olderResult = calculator.calculateStepsGoal(olderAdult)
        results.add("Older Adult (70y): $olderResult steps (expected: 8000)")
        
        // Test 5: Inclusive genders
        val inclusive = GoalCalculationInput(
            age = 25,
            gender = Gender.PREFER_NOT_TO_SAY,
            heightInCm = 170,
            weightInKg = 65.0
        )
        val inclusiveResult = calculator.calculateStepsGoal(inclusive)
        results.add("Inclusive Gender (25y): $inclusiveResult steps (expected: 10000)")
        
        // Test 6: Boundary test - very old
        val veryOld = GoalCalculationInput(
            age = 90,
            gender = Gender.FEMALE,
            heightInCm = 150,
            weightInKg = 45.0
        )
        val veryOldResult = calculator.calculateStepsGoal(veryOld)
        results.add("Very Old (90y): $veryOldResult steps (should be >= 5000)")
        
        // Test 7: Boundary test - youth with high adjustment
        val activeYouth = GoalCalculationInput(
            age = 16,
            gender = Gender.MALE,
            heightInCm = 180,
            weightInKg = 70.0
        )
        val activeYouthResult = calculator.calculateStepsGoal(activeYouth)
        results.add("Active Youth (16y Male): $activeYouthResult steps (should be <= 20000)")
        
        // Validation checks
        results.add("\n=== Validation Results ===")
        val allResults = listOf(maleResult, femaleResult, youthResult, olderResult, inclusiveResult, veryOldResult, activeYouthResult)
        val allValid = allResults.all { it in 5000..20000 }
        results.add("All results within bounds (5000-20000): $allValid")
        
        val expectedResults = mapOf(
            "Male" to (maleResult == 10500),
            "Female" to (femaleResult == 9500),
            "Youth" to (youthResult == 12000),
            "Older" to (olderResult == 8000),
            "Inclusive" to (inclusiveResult == 10000)
        )
        
        expectedResults.forEach { (test, passed) ->
            results.add("$test calculation correct: $passed")
        }
        
        val overallSuccess = allValid && expectedResults.values.all { it }
        results.add("\n=== Overall Result ===")
        results.add("StepsGoalCalculator implementation: ${if (overallSuccess) "✅ PASSED" else "❌ FAILED"}")
        
        return results.joinToString("\n")
    }
}