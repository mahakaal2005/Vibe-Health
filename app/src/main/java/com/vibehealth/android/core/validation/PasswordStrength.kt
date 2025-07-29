package com.vibehealth.android.core.validation

/**
 * Password strength evaluation utility
 */
data class PasswordStrength(
    val level: Level,
    val score: Int,
    val feedback: List<String>
) {
    
    enum class Level {
        NONE,
        VERY_WEAK,
        WEAK,
        MEDIUM,
        STRONG,
        VERY_STRONG
    }
    
    companion object {
        /**
         * Evaluate password strength
         */
        fun evaluate(password: String): PasswordStrength {
            if (password.isEmpty()) {
                return PasswordStrength(Level.NONE, 0, emptyList())
            }
            
            var score = 0
            val feedback = mutableListOf<String>()
            
            // Length check
            when {
                password.length >= 12 -> score += 25
                password.length >= 8 -> score += 15
                password.length >= 6 -> score += 10
                else -> {
                    score += 0
                    feedback.add("Use at least 8 characters")
                }
            }
            
            // Character variety checks
            if (password.any { it.isLowerCase() }) {
                score += 10
            } else {
                feedback.add("Add lowercase letters")
            }
            
            if (password.any { it.isUpperCase() }) {
                score += 15
            } else {
                feedback.add("Add uppercase letters")
            }
            
            if (password.any { it.isDigit() }) {
                score += 15
            } else {
                feedback.add("Add numbers")
            }
            
            if (password.any { !it.isLetterOrDigit() }) {
                score += 20
            } else {
                feedback.add("Add special characters")
            }
            
            // Pattern checks
            if (!hasRepeatingCharacters(password)) {
                score += 10
            } else {
                feedback.add("Avoid repeating characters")
            }
            
            if (!hasSequentialCharacters(password)) {
                score += 5
            } else {
                feedback.add("Avoid sequential characters")
            }
            
            // Determine level based on score
            val level = when {
                score >= 85 -> Level.VERY_STRONG
                score >= 70 -> Level.STRONG
                score >= 50 -> Level.MEDIUM
                score >= 30 -> Level.WEAK
                else -> Level.VERY_WEAK
            }
            
            return PasswordStrength(level, score, feedback)
        }
        
        private fun hasRepeatingCharacters(password: String): Boolean {
            return password.zipWithNext().any { (a, b) -> a == b }
        }
        
        private fun hasSequentialCharacters(password: String): Boolean {
            return password.zipWithNext().any { (a, b) -> 
                Math.abs(a.code - b.code) == 1
            }
        }
    }
}