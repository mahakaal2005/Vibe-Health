package com.vibehealth.android.ui.navigation

import android.content.Context
import android.content.Intent
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.vibehealth.android.R
import com.vibehealth.android.ui.dashboard.DashboardActivity
import com.vibehealth.android.ui.dashboard.models.RingType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Navigation manager for dashboard-related navigation.
 * Handles navigation from Story 1.2 onboarding completion to dashboard
 * and navigation to detailed progress views.
 */
@Singleton
class DashboardNavigationManager @Inject constructor() {
    
    /**
     * Navigates to dashboard from onboarding completion.
     * Clears the onboarding back stack as per Story 1.2 integration.
     */
    fun navigateToDashboardFromOnboarding(context: Context) {
        val intent = Intent(context, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }
    
    /**
     * Navigates to dashboard from any other screen.
     */
    fun navigateToDashboard(context: Context) {
        val intent = Intent(context, DashboardActivity::class.java)
        context.startActivity(intent)
    }
    
    /**
     * Navigates to detailed progress view for a specific ring type.
     * This would be implemented when detailed progress screens are created.
     */
    fun navigateToDetailedProgress(context: Context, ringType: RingType) {
        // For now, this is a placeholder
        // In a full implementation, this would navigate to a detailed progress screen
        // val intent = Intent(context, DetailedProgressActivity::class.java).apply {
        //     putExtra("ringType", ringType.name)
        // }
        // context.startActivity(intent)
    }
    
    /**
     * Navigates to profile setup from dashboard empty state.
     */
    fun navigateToProfileSetup(context: Context) {
        // This would navigate back to onboarding or profile setup
        // For now, this is a placeholder
        // val intent = Intent(context, OnboardingActivity::class.java).apply {
        //     putExtra("resumeFromProfile", true)
        // }
        // context.startActivity(intent)
    }
    
    /**
     * Handles deep linking to dashboard.
     */
    fun handleDashboardDeepLink(context: Context, deepLinkData: String?) {
        val intent = Intent(context, DashboardActivity::class.java).apply {
            deepLinkData?.let { data ->
                putExtra("deepLinkData", data)
            }
        }
        context.startActivity(intent)
    }
}

/**
 * Navigation extensions for Fragment-based navigation using NavController.
 */
class FragmentDashboardNavigationManager @Inject constructor() {
    
    /**
     * Navigates to dashboard fragment from onboarding completion.
     */
    fun navigateToDashboardFromOnboarding(navController: NavController) {
        navController.navigate(
            R.id.action_onboarding_to_dashboard,
            null,
            NavOptions.Builder()
                .setPopUpTo(R.id.onboarding_graph, true)
                .build()
        )
    }
    
    /**
     * Navigates to detailed progress fragment.
     */
    fun navigateToDetailedProgress(navController: NavController, ringType: RingType) {
        val bundle = androidx.core.os.bundleOf("ringType" to ringType.name)
        // navController.navigate(R.id.action_dashboard_to_detailed_progress, bundle)
    }
    
    /**
     * Navigates back to profile setup.
     */
    fun navigateToProfileSetup(navController: NavController) {
        // navController.navigate(R.id.action_dashboard_to_profile_setup)
    }
}