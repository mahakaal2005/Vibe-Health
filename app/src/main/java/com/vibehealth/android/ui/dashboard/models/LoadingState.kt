package com.vibehealth.android.ui.dashboard.models

/**
 * Represents the loading state of the dashboard.
 * Used to show appropriate UI states (loading, loaded, error, empty).
 */
enum class LoadingState {
    /**
     * Dashboard is currently loading data.
     */
    LOADING,
    
    /**
     * Dashboard has successfully loaded data.
     */
    LOADED,
    
    /**
     * Dashboard encountered an error while loading.
     */
    ERROR,
    
    /**
     * Dashboard has no data to display (e.g., no goals calculated).
     */
    EMPTY
}