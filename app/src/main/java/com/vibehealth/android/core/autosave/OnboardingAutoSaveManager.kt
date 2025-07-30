package com.vibehealth.android.core.autosave

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.vibehealth.android.core.persistence.OnboardingProgressManager
import com.vibehealth.android.core.persistence.ProgressField
import com.vibehealth.android.domain.user.Gender
import com.vibehealth.android.domain.common.UnitSystem
import kotlinx.coroutines.*
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Auto-save manager for onboarding form data
 * Prevents data loss during interruptions by automatically saving form input
 */
@Singleton
class OnboardingAutoSaveManager @Inject constructor(
    private val progressManager: OnboardingProgressManager
) {

    private val autoSaveScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val autoSaveJobs = mutableMapOf<String, Job>()
    
    companion object {
        private const val AUTO_SAVE_DELAY_MS = 2000L // 2 seconds delay
    }

    /**
     * Set up auto-save for a text input field
     */
    fun setupAutoSave(editText: EditText, field: ProgressField) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                scheduleAutoSave(field.name, s?.toString() ?: "") {
                    progressManager.updateField(field, s?.toString() ?: "")
                }
            }
        })
    }

    /**
     * Auto-save birthday selection
     */
    fun autoSaveBirthday(birthday: Date?) {
        scheduleAutoSave("birthday", birthday?.toString() ?: "") {
            progressManager.updateBirthday(birthday)
        }
    }

    /**
     * Auto-save gender selection
     */
    fun autoSaveGender(gender: Gender) {
        scheduleAutoSave("gender", gender.name) {
            progressManager.updateField(ProgressField.GENDER, gender.name)
        }
    }

    /**
     * Auto-save unit system selection
     */
    fun autoSaveUnitSystem(unitSystem: UnitSystem) {
        scheduleAutoSave("unit_system", unitSystem.name) {
            progressManager.updateField(ProgressField.UNIT_SYSTEM, unitSystem.name)
        }
    }

    /**
     * Schedule auto-save operation with debouncing
     */
    private fun scheduleAutoSave(key: String, value: String, saveOperation: suspend () -> Unit) {
        // Cancel previous auto-save job for this key
        autoSaveJobs[key]?.cancel()
        
        // Schedule new auto-save job
        autoSaveJobs[key] = autoSaveScope.launch {
            delay(AUTO_SAVE_DELAY_MS)
            
            try {
                saveOperation()
                android.util.Log.d("OnboardingAutoSave", "Auto-saved $key: ${sanitizeValue(value)}")
            } catch (e: Exception) {
                android.util.Log.w("OnboardingAutoSave", "Failed to auto-save $key", e)
            }
        }
    }

    /**
     * Force save all pending auto-save operations
     */
    suspend fun forceSaveAll() {
        // Wait for all pending auto-save operations to complete
        autoSaveJobs.values.forEach { job ->
            try {
                job.join()
            } catch (e: Exception) {
                // Ignore cancellation exceptions
            }
        }
    }

    /**
     * Clear all auto-save operations
     */
    fun clearAutoSave() {
        autoSaveJobs.values.forEach { it.cancel() }
        autoSaveJobs.clear()
    }

    /**
     * Set up auto-save for multiple fields at once
     */
    fun setupBulkAutoSave(fieldMappings: Map<EditText, ProgressField>) {
        fieldMappings.forEach { (editText, field) ->
            setupAutoSave(editText, field)
        }
    }

    /**
     * Get auto-save status for debugging
     */
    fun getAutoSaveStatus(): AutoSaveStatus {
        val activeJobs = autoSaveJobs.values.count { it.isActive }
        val completedJobs = autoSaveJobs.values.count { it.isCompleted }
        val cancelledJobs = autoSaveJobs.values.count { it.isCancelled }
        
        return AutoSaveStatus(
            activeJobs = activeJobs,
            completedJobs = completedJobs,
            cancelledJobs = cancelledJobs,
            totalJobs = autoSaveJobs.size
        )
    }

    /**
     * Enable/disable auto-save based on user preference or system state
     */
    fun setAutoSaveEnabled(enabled: Boolean) {
        if (!enabled) {
            clearAutoSave()
        }
        // If enabling, auto-save will be set up when fields are configured
    }

    /**
     * Sanitize value for logging (remove PII)
     */
    private fun sanitizeValue(value: String): String {
        return when {
            value.length > 20 -> "[LONG_VALUE_${value.length}_CHARS]"
            value.contains("@") -> "[EMAIL_VALUE]"
            value.matches(Regex("\\d+")) -> "[NUMERIC_VALUE]"
            else -> "[TEXT_VALUE]"
        }
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        clearAutoSave()
        autoSaveScope.cancel()
    }
}

/**
 * Data class representing auto-save status
 */
data class AutoSaveStatus(
    val activeJobs: Int,
    val completedJobs: Int,
    val cancelledJobs: Int,
    val totalJobs: Int
) {
    val isIdle: Boolean
        get() = activeJobs == 0
    
    val hasFailures: Boolean
        get() = cancelledJobs > 0
}