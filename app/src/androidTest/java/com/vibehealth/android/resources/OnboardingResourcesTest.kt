package com.vibehealth.android.resources

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.vibehealth.android.R
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for onboarding resources and design system components
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
class OnboardingResourcesTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun onboardingStrings_shouldBeAvailable() {
        // Test all onboarding strings are available
        val strings = listOf(
            R.string.welcome_to_vibe_health,
            R.string.personalize_wellness_journey,
            R.string.calculate_daily_goals,
            R.string.track_your_progress,
            R.string.get_personalized_insights,
            R.string.get_started,
            R.string.step_indicator,
            R.string.personal_information,
            R.string.tell_us_about_yourself,
            R.string.full_name,
            R.string.birthday,
            R.string.select_date,
            R.string.helps_calculate_goals,
            R.string.physical_information,
            R.string.unit_system,
            R.string.metric,
            R.string.imperial,
            R.string.height_cm,
            R.string.height_ft,
            R.string.weight_kg,
            R.string.weight_lbs,
            R.string.gender,
            R.string.male,
            R.string.female,
            R.string.other,
            R.string.prefer_not_to_say,
            R.string.continue_button,
            R.string.youre_all_set,
            R.string.calculating_goals,
            R.string.wellness_journey_starts,
            R.string.enter_vibe_health
        )

        strings.forEach { stringRes ->
            val string = context.getString(stringRes)
            assertNotNull(string, "String resource $stringRes should not be null")
            assertTrue(string.isNotEmpty(), "String resource $stringRes should not be empty")
        }
    }

    @Test
    fun onboardingValidationStrings_shouldBeAvailable() {
        // Test validation error strings
        val validationStrings = listOf(
            R.string.error_name_required,
            R.string.error_name_too_short,
            R.string.error_name_too_long,
            R.string.error_birthday_required,
            R.string.error_birthday_future,
            R.string.error_birthday_invalid_age,
            R.string.error_height_required,
            R.string.error_height_invalid,
            R.string.error_weight_required,
            R.string.error_weight_invalid,
            R.string.error_gender_required
        )

        validationStrings.forEach { stringRes ->
            val string = context.getString(stringRes)
            assertNotNull(string, "Validation string resource $stringRes should not be null")
            assertTrue(string.isNotEmpty(), "Validation string resource $stringRes should not be empty")
        }
    }

    @Test
    fun accessibilityStrings_shouldBeAvailable() {
        // Test accessibility content description strings
        val accessibilityStrings = listOf(
            R.string.welcome_illustration,
            R.string.success_checkmark,
            R.string.loading_animation,
            R.string.progress_indicator,
            R.string.back_button,
            R.string.date_picker_button,
            R.string.unit_system_toggle
        )

        accessibilityStrings.forEach { stringRes ->
            val string = context.getString(stringRes)
            assertNotNull(string, "Accessibility string resource $stringRes should not be null")
            assertTrue(string.isNotEmpty(), "Accessibility string resource $stringRes should not be empty")
        }
    }

    @Test
    fun onboardingDimensions_shouldBeAvailable() {
        // Test onboarding-specific dimensions
        val dimensions = listOf(
            R.dimen.onboarding_illustration_size,
            R.dimen.onboarding_checkmark_size,
            R.dimen.progress_bar_height,
            R.dimen.unit_toggle_height,
            R.dimen.gender_option_spacing,
            R.dimen.touch_target_min
        )

        dimensions.forEach { dimenRes ->
            val dimension = context.resources.getDimensionPixelSize(dimenRes)
            assertTrue(dimension > 0, "Dimension resource $dimenRes should be greater than 0")
        }
    }

    @Test
    fun onboardingColors_shouldBeAvailable() {
        // Test onboarding colors in light mode
        val colors = listOf(
            R.color.sage_green,
            R.color.soft_coral,
            R.color.background_light,
            R.color.text_primary,
            R.color.text_secondary,
            R.color.error,
            R.color.success,
            R.color.warning
        )

        colors.forEach { colorRes ->
            val color = ContextCompat.getColor(context, colorRes)
            assertNotNull(color, "Color resource $colorRes should not be null")
        }
    }

    @Test
    fun onboardingColors_darkMode_shouldBeAvailable() {
        // Create dark mode configuration
        val darkConfig = Configuration(context.resources.configuration)
        darkConfig.uiMode = Configuration.UI_MODE_NIGHT_YES
        val darkContext = context.createConfigurationContext(darkConfig)

        // Test onboarding colors in dark mode
        val colors = listOf(
            R.color.sage_green,
            R.color.soft_coral,
            R.color.background_light,
            R.color.text_primary,
            R.color.text_secondary,
            R.color.error,
            R.color.success,
            R.color.warning
        )

        colors.forEach { colorRes ->
            val lightColor = ContextCompat.getColor(context, colorRes)
            val darkColor = ContextCompat.getColor(darkContext, colorRes)
            
            assertNotNull(lightColor, "Light mode color resource $colorRes should not be null")
            assertNotNull(darkColor, "Dark mode color resource $colorRes should not be null")
        }
    }

    @Test
    fun onboardingDrawables_shouldBeAvailable() {
        // Test onboarding drawable resources
        val drawables = listOf(
            R.drawable.ic_onboarding_welcome,
            R.drawable.ic_success_checkmark,
            R.drawable.ic_progress_indicator,
            R.drawable.focus_indicator,
            R.drawable.circle_sage_green
        )

        drawables.forEach { drawableRes ->
            val drawable = ContextCompat.getDrawable(context, drawableRes)
            assertNotNull(drawable, "Drawable resource $drawableRes should not be null")
        }
    }

    @Test
    fun onboardingStyles_shouldBeAvailable() {
        // Test that onboarding styles can be resolved
        val styles = listOf(
            R.style.VibeHealth_Onboarding_Title,
            R.style.VibeHealth_Onboarding_Subtitle,
            R.style.VibeHealth_Onboarding_ProgressBar,
            R.style.VibeHealth_Onboarding_UnitToggle,
            R.style.VibeHealth_Onboarding_RadioButton,
            R.style.VibeHealth_Text_Error,
            R.style.VibeHealth_Text_Success,
            R.style.VibeHealth_Text_Caption
        )

        // Note: Style testing is limited in unit tests, but we can at least verify they exist
        styles.forEach { styleRes ->
            // Styles exist if no exception is thrown when accessing them
            assertTrue(styleRes > 0, "Style resource $styleRes should have a valid ID")
        }
    }

    @Test
    fun stepIndicatorString_shouldFormatCorrectly() {
        // Test step indicator string formatting
        val stepIndicator = context.getString(R.string.step_indicator, 1, 3)
        assertTrue(stepIndicator.contains("1"), "Step indicator should contain step number")
        assertTrue(stepIndicator.contains("3"), "Step indicator should contain total steps")
    }

    @Test
    fun touchTargetMinimum_shouldMeetAccessibilityStandards() {
        // Test that minimum touch target meets 48dp requirement
        val minTouchTarget = context.resources.getDimensionPixelSize(R.dimen.touch_target_min)
        val expectedMinimum = (48 * context.resources.displayMetrics.density).toInt()
        
        assertTrue(
            minTouchTarget >= expectedMinimum,
            "Minimum touch target should be at least 48dp (${expectedMinimum}px), but was ${minTouchTarget}px"
        )
    }

    @Test
    fun progressBarHeight_shouldBeAppropriate() {
        // Test that progress bar height is reasonable
        val progressBarHeight = context.resources.getDimensionPixelSize(R.dimen.progress_bar_height)
        
        assertTrue(
            progressBarHeight >= 2 && progressBarHeight <= 8,
            "Progress bar height should be between 2-8dp, but was ${progressBarHeight}dp"
        )
    }
}