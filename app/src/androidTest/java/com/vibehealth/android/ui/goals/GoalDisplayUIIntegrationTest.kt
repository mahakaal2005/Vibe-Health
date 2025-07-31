package com.vibehealth.android.ui.goals

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vibehealth.android.domain.goals.CalculationSource
import com.vibehealth.android.domain.goals.DailyGoals
import com.vibehealth.android.ui.goals.components.GoalDisplayCard
import com.vibehealth.android.ui.goals.dialogs.GoalExplanationDialog
import com.vibehealth.android.ui.theme.VibeHealthTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/**
 * UI integration tests for goal display components.
 * 
 * Tests goal display components with real data, loading states,
 * error handling, accessibility, and responsive design
 * as specified in Task 5.2.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class GoalDisplayUIIntegrationTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testGoals = DailyGoals(
        userId = "ui-test-user",
        stepsGoal = 10000,
        caloriesGoal = 2000,
        heartPointsGoal = 30,
        calculatedAt = LocalDateTime.now(),
        calculationSource = CalculationSource.WHO_STANDARD
    )

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testGoalDisplayComponentsWithRealData() {
        composeTestRule.setContent {
            VibeHealthTheme {
                GoalDisplayCard(
                    goals = testGoals,
                    onLearnMoreClick = {},
                    onRecalculateClick = {}
                )
            }
        }

        // Verify goal values are displayed
        composeTestRule.onNodeWithText("10,000").assertIsDisplayed()
        composeTestRule.onNodeWithText("2,000").assertIsDisplayed()
        composeTestRule.onNodeWithText("30").assertIsDisplayed()

        // Verify WHO source attribution
        composeTestRule.onNodeWithText("Calculated from WHO standards").assertIsDisplayed()

        // Verify interactive elements
        composeTestRule.onNodeWithContentDescription("More info").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recalculate Goals").assertIsDisplayed()
    }

    @Test
    fun testLoadingStatesAndErrorHandlingInUI() {
        var isLoading = true
        var hasError = false

        composeTestRule.setContent {
            VibeHealthTheme {
                when {
                    isLoading -> {
                        // Loading state component would go here
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                    hasError -> {
                        // Error state component would go here
                        androidx.compose.material3.Text("Failed to load goals")
                    }
                    else -> {
                        GoalDisplayCard(
                            goals = testGoals,
                            onLearnMoreClick = {},
                            onRecalculateClick = {}
                        )
                    }
                }
            }
        }

        // Test loading state
        composeTestRule.onNode(hasProgressBarSemantics()).assertIsDisplayed()

        // Switch to error state
        composeTestRule.runOnUiThread {
            isLoading = false
            hasError = true
        }

        composeTestRule.onNodeWithText("Failed to load goals").assertIsDisplayed()

        // Switch to success state
        composeTestRule.runOnUiThread {
            hasError = false
        }

        composeTestRule.onNodeWithText("10,000").assertIsDisplayed()
    }

    @Test
    fun testAccessibilityFeaturesAndScreenReaderSupport() {
        composeTestRule.setContent {
            VibeHealthTheme {
                GoalDisplayCard(
                    goals = testGoals,
                    onLearnMoreClick = {},
                    onRecalculateClick = {}
                )
            }
        }

        // Test content descriptions
        composeTestRule.onNodeWithContentDescription("Steps icon").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Calories icon").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Heart points icon").assertIsDisplayed()

        // Test semantic properties for screen readers
        composeTestRule.onNodeWithText("Steps").assertHasClickAction()
        composeTestRule.onNodeWithText("Calories").assertHasClickAction()
        composeTestRule.onNodeWithText("Heart Points").assertHasClickAction()

        // Test button accessibility
        composeTestRule.onNodeWithText("Recalculate Goals")
            .assertIsDisplayed()
            .assertHasClickAction()

        // Test info button accessibility
        composeTestRule.onNodeWithContentDescription("More info")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun testResponsiveDesignAcrossDifferentScreenSizes() {
        // Test compact screen size
        composeTestRule.setContent {
            VibeHealthTheme {
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier.size(
                        width = 360.dp,
                        height = 640.dp
                    )
                ) {
                    GoalDisplayCard(
                        goals = testGoals,
                        onLearnMoreClick = {},
                        onRecalculateClick = {}
                    )
                }
            }
        }

        // Verify content is visible on compact screen
        composeTestRule.onNodeWithText("Daily Wellness Goals").assertIsDisplayed()
        composeTestRule.onNodeWithText("10,000").assertIsDisplayed()

        // Test expanded screen size
        composeTestRule.setContent {
            VibeHealthTheme {
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier.size(
                        width = 800.dp,
                        height = 1200.dp
                    )
                ) {
                    GoalDisplayCard(
                        goals = testGoals,
                        onLearnMoreClick = {},
                        onRecalculateClick = {}
                    )
                }
            }
        }

        // Verify content adapts to larger screen
        composeTestRule.onNodeWithText("Daily Wellness Goals").assertIsDisplayed()
        composeTestRule.onNodeWithText("10,000").assertIsDisplayed()
    }

    @Test
    fun testGoalExplanationDialogInteraction() {
        var showDialog = false

        composeTestRule.setContent {
            VibeHealthTheme {
                GoalDisplayCard(
                    goals = testGoals,
                    onLearnMoreClick = { showDialog = true },
                    onRecalculateClick = {}
                )

                if (showDialog) {
                    GoalExplanationDialog(
                        goals = testGoals,
                        onDismiss = { showDialog = false }
                    )
                }
            }
        }

        // Click learn more button
        composeTestRule.onNodeWithContentDescription("More info").performClick()

        // Verify dialog is displayed
        composeTestRule.onNodeWithText("How Your Goals Are Calculated").assertIsDisplayed()
        composeTestRule.onNodeWithText("WHO Standards").assertIsDisplayed()

        // Test dialog dismissal
        composeTestRule.onNodeWithText("Got it").performClick()

        // Verify dialog is dismissed
        composeTestRule.onNodeWithText("How Your Goals Are Calculated").assertDoesNotExist()
    }

    @Test
    fun testGoalRecalculationFeedback() {
        var isRecalculating = false
        var recalculationComplete = false

        composeTestRule.setContent {
            VibeHealthTheme {
                androidx.compose.foundation.layout.Column {
                    GoalDisplayCard(
                        goals = testGoals,
                        onLearnMoreClick = {},
                        onRecalculateClick = { 
                            isRecalculating = true
                            // Simulate recalculation
                            kotlinx.coroutines.GlobalScope.launch {
                                kotlinx.coroutines.delay(1000)
                                isRecalculating = false
                                recalculationComplete = true
                            }
                        }
                    )

                    if (isRecalculating) {
                        androidx.compose.material3.Text("Recalculating goals...")
                    }

                    if (recalculationComplete) {
                        androidx.compose.material3.Text("Goals updated successfully")
                    }
                }
            }
        }

        // Click recalculate button
        composeTestRule.onNodeWithText("Recalculate Goals").performClick()

        // Verify loading feedback
        composeTestRule.onNodeWithText("Recalculating goals...").assertIsDisplayed()

        // Wait for completion
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            try {
                composeTestRule.onNodeWithText("Goals updated successfully").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Verify success feedback
        composeTestRule.onNodeWithText("Goals updated successfully").assertIsDisplayed()
    }

    @Test
    fun testDarkThemeSupport() {
        composeTestRule.setContent {
            VibeHealthTheme(darkTheme = true) {
                GoalDisplayCard(
                    goals = testGoals,
                    onLearnMoreClick = {},
                    onRecalculateClick = {}
                )
            }
        }

        // Verify content is visible in dark theme
        composeTestRule.onNodeWithText("Daily Wellness Goals").assertIsDisplayed()
        composeTestRule.onNodeWithText("10,000").assertIsDisplayed()
        composeTestRule.onNodeWithText("Calculated from WHO standards").assertIsDisplayed()

        // Test theme switching
        composeTestRule.setContent {
            VibeHealthTheme(darkTheme = false) {
                GoalDisplayCard(
                    goals = testGoals,
                    onLearnMoreClick = {},
                    onRecalculateClick = {}
                )
            }
        }

        // Verify content is still visible in light theme
        composeTestRule.onNodeWithText("Daily Wellness Goals").assertIsDisplayed()
    }

    @Test
    fun testGoalValueFormatting() {
        val largeGoals = testGoals.copy(
            stepsGoal = 15000,
            caloriesGoal = 2500,
            heartPointsGoal = 45
        )

        composeTestRule.setContent {
            VibeHealthTheme {
                GoalDisplayCard(
                    goals = largeGoals,
                    onLearnMoreClick = {},
                    onRecalculateClick = {}
                )
            }
        }

        // Verify proper number formatting
        composeTestRule.onNodeWithText("15,000").assertIsDisplayed()
        composeTestRule.onNodeWithText("2,500").assertIsDisplayed()
        composeTestRule.onNodeWithText("45").assertIsDisplayed()
    }

    @Test
    fun testFallbackGoalDisplay() {
        val fallbackGoals = testGoals.copy(
            calculationSource = CalculationSource.FALLBACK_DEFAULT,
            stepsGoal = 7500,
            caloriesGoal = 1800,
            heartPointsGoal = 21
        )

        composeTestRule.setContent {
            VibeHealthTheme {
                GoalDisplayCard(
                    goals = fallbackGoals,
                    onLearnMoreClick = {},
                    onRecalculateClick = {}
                )
            }
        }

        // Verify fallback goals are displayed
        composeTestRule.onNodeWithText("7,500").assertIsDisplayed()
        composeTestRule.onNodeWithText("1,800").assertIsDisplayed()
        composeTestRule.onNodeWithText("21").assertIsDisplayed()

        // Verify fallback source indication
        composeTestRule.onNodeWithText("Default goals").assertIsDisplayed()
    }

    @Test
    fun testGoalUpdateAnimations() {
        var currentGoals = testGoals

        composeTestRule.setContent {
            VibeHealthTheme {
                GoalDisplayCard(
                    goals = currentGoals,
                    onLearnMoreClick = {},
                    onRecalculateClick = {}
                )
            }
        }

        // Initial state
        composeTestRule.onNodeWithText("10,000").assertIsDisplayed()

        // Update goals
        composeTestRule.runOnUiThread {
            currentGoals = testGoals.copy(stepsGoal = 12000)
        }

        // Verify updated value is displayed
        composeTestRule.onNodeWithText("12,000").assertIsDisplayed()
        composeTestRule.onNodeWithText("10,000").assertDoesNotExist()
    }

    private fun hasProgressBarSemantics() = SemanticsMatcher("ProgressBar") { node ->
        node.config.contains(androidx.compose.ui.semantics.SemanticsProperties.ProgressBarRangeInfo)
    }
}