package com.vibehealth.android.data.discover

import android.util.Log
import com.vibehealth.android.ui.discover.models.HealthContent
import com.vibehealth.android.ui.discover.models.ContentCategory
import com.vibehealth.android.domain.user.UserProfile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HealthContentCurator - Content filtering and quality assurance
 * Ensures content aligns with Companion Principle and app values
 */
@Singleton
class HealthContentCurator @Inject constructor(
    private val contentSecurityValidator: ContentSecurityValidator
) {
    
    companion object {
        private const val TAG = "DISCOVER_CONTENT"
    }
    
    init {
        Log.d(TAG, "Implementing content curation and filtering")
    }
    
    fun filterContent(
        rawContent: List<HealthContent>,
        userProfile: UserProfile?
    ): List<HealthContent> {
        Log.d(TAG, "🔍 Starting content curation: ${rawContent.size} items")
        
        // Log content types before filtering
        val contentTypes = rawContent.groupBy { it::class.simpleName }
        contentTypes.forEach { (type, items) ->
            Log.d(TAG, "📋 Input content type: $type = ${items.size} items")
        }
        
        val step1 = rawContent.filter { content -> 
            val isValid = contentSecurityValidator.validateContent(content)
            if (!isValid) Log.d(TAG, "❌ Security filter rejected: ${content.title}")
            isValid
        }
        Log.d(TAG, "🔒 After security validation: ${step1.size} items")
        
        val step2 = step1.filter { content -> 
            val isWellness = isWellnessFocused(content)
            if (!isWellness) Log.d(TAG, "❌ Wellness filter rejected: ${content.title}")
            isWellness
        }
        Log.d(TAG, "🌿 After wellness filter: ${step2.size} items")
        
        val step3 = step2.filter { content -> 
            val isEvidence = isEvidenceBased(content)
            if (!isEvidence) Log.d(TAG, "❌ Evidence filter rejected: ${content.title}")
            isEvidence
        }
        Log.d(TAG, "📚 After evidence filter: ${step3.size} items")
        
        val step4 = step3.filter { content -> 
            val isAppropriate = isAppropriateForGeneralAudience(content)
            if (!isAppropriate) Log.d(TAG, "❌ Audience filter rejected: ${content.title}")
            isAppropriate
        }
        Log.d(TAG, "👥 After audience filter: ${step4.size} items")
        
        val step5 = step4.filter { content -> 
            val aligns = alignsWithCompanionPrinciple(content)
            if (!aligns) Log.d(TAG, "❌ Companion filter rejected: ${content.title}")
            aligns
        }
        Log.d(TAG, "💚 After companion filter: ${step5.size} items")
        
        val finalContent = step5
            .map { content -> enhanceWithSupportiveContext(content, userProfile) }
            .sortedBy { content -> calculateRelevanceScore(content, userProfile) }
        
        // Log final content types
        val finalTypes = finalContent.groupBy { it::class.simpleName }
        finalTypes.forEach { (type, items) ->
            Log.d(TAG, "📋 Final content type: $type = ${items.size} items")
        }
        
        Log.d(TAG, "✅ Content curation complete: ${finalContent.size} items passed all filters")
        return finalContent
    }
    
    private fun isWellnessFocused(content: HealthContent): Boolean {
        // Much more permissive approach - since we're already querying health-focused APIs
        val wellnessKeywords = listOf(
            "wellness", "health", "nutrition", "fitness", "mindfulness",
            "sleep", "stress", "mental", "physical", "activity",
            "eating", "self-care", "balance", "prevention", "wellbeing",
            "medical", "study", "research", "doctor", "exercise", "diet",
            "food", "medicine", "therapy", "treatment", "care", "body",
            "brain", "heart", "weight", "energy", "lifestyle", "habit",
            "yoga", "meditation", "walking", "running", "strength", "cardio",
            "vitamin", "protein", "fiber", "water", "hydration", "recovery",
            "immune", "disease", "condition", "symptom", "healing", "cure",
            "supplement", "organic", "natural", "holistic", "alternative"
        )
        
        // For news content, be very permissive since News API already filters by health topics
        val isWellnessFocused = if (content is HealthContent.News) {
            // Since we're querying News API with health keywords, assume most content is relevant
            // Only filter out obviously non-health content
            val hasHealthKeyword = wellnessKeywords.any { keyword ->
                content.title.contains(keyword, ignoreCase = true) ||
                content.description.contains(keyword, ignoreCase = true)
            }
            
            // Very permissive - if it has any health keyword OR is longer than 15 chars, keep it
            hasHealthKeyword || content.title.length > 15
        } else {
            // For other content types, still be permissive but require at least one keyword
            wellnessKeywords.any { keyword ->
                content.title.contains(keyword, ignoreCase = true) ||
                content.description.contains(keyword, ignoreCase = true)
            }
        }
        
        Log.d(TAG, "Wellness focus check for '${content.title}': $isWellnessFocused")
        return isWellnessFocused
    }
    
    private fun isEvidenceBased(content: HealthContent): Boolean {
        // Very permissive check - assume content from our curated APIs is evidence-based
        val evidenceIndicators = listOf(
            "health", "wellness", "tips", "guide", "information", "study", "research",
            "expert", "doctor", "medical", "science", "clinical", "trial", "data",
            "evidence", "proven", "effective", "benefit", "result", "finding",
            "recommendation", "guideline", "advice", "professional", "specialist"
        )
        
        // For news content, be extremely permissive since it comes from News API health queries
        val hasEvidence = if (content is HealthContent.News) {
            // News from health-focused queries is assumed to be informational/evidence-based
            true // Very permissive for news content
        } else {
            // For other content, check for evidence indicators or trusted sources
            evidenceIndicators.any { indicator ->
                content.description.contains(indicator, ignoreCase = true) ||
                content.title.contains(indicator, ignoreCase = true)
            } || content.sourceUrl.contains("gov") || 
            content.sourceUrl.contains("edu") ||
            content.sourceUrl.contains("youtube.com") // YouTube health content is informational
        }
        
        Log.d(TAG, "Evidence-based check for '${content.title}': $hasEvidence")
        return hasEvidence
    }
    
    private fun isAppropriateForGeneralAudience(content: HealthContent): Boolean {
        // Very restrictive list - only filter out truly inappropriate or graphic content
        val inappropriateKeywords = listOf(
            "autopsy", "graphic surgery", "blood", "gore", "death", "dying",
            "suicide", "self-harm", "overdose", "addiction crisis", "terminal illness"
        )
        
        // Be very permissive - only filter out truly inappropriate content
        val isAppropriate = inappropriateKeywords.none { keyword ->
            content.title.contains(keyword, ignoreCase = true) ||
            content.description.contains(keyword, ignoreCase = true)
        }
        
        Log.d(TAG, "General audience check for '${content.title}': $isAppropriate")
        return isAppropriate
    }
    
    private fun alignsWithCompanionPrinciple(content: HealthContent): Boolean {
        // Very minimal filtering - only reject extremely negative content
        val extremelyNegativeLanguage = listOf(
            "you're failing", "you suck", "worthless", "pathetic", "disgusting",
            "hate yourself", "you're fat", "you're ugly", "give up"
        )
        
        val hasExtremelyNegativeLanguage = extremelyNegativeLanguage.any { phrase ->
            content.description.contains(phrase, ignoreCase = true) ||
            content.title.contains(phrase, ignoreCase = true)
        }
        
        val aligns = !hasExtremelyNegativeLanguage // Very permissive
        Log.d(TAG, "Companion Principle alignment for '${content.title}': $aligns")
        return aligns
    }
    
    private fun enhanceWithSupportiveContext(
        content: HealthContent,
        userProfile: UserProfile?
    ): HealthContent {
        Log.d(TAG, "Enhancing content with supportive context: ${content.title}")
        
        val enhancedContext = when (content.category) {
            ContentCategory.NUTRITION -> "Nourish your body with this gentle nutrition guidance 🥗"
            ContentCategory.FITNESS -> "Discover joyful movement that celebrates your body 🏃‍♀️"
            ContentCategory.MINDFULNESS -> "Find moments of peace in your busy day 🧘‍♂️"
            ContentCategory.SLEEP -> "Support your body's natural rest and recovery 😴"
            ContentCategory.STRESS_MANAGEMENT -> "Gentle tools for navigating life's challenges 🌸"
            ContentCategory.GENERAL_WELLNESS -> "Supporting your wellness journey with caring guidance ✨"
        }
        
        return when (content) {
            is HealthContent.Article -> content.copy(supportiveContext = enhancedContext)
            is HealthContent.Video -> content.copy(supportiveContext = enhancedContext)
            is HealthContent.News -> content.copy(supportiveContext = enhancedContext)
        }
    }
    
    private fun calculateRelevanceScore(content: HealthContent, userProfile: UserProfile?): Int {
        var score = 0
        
        // Base score for all content
        score += 10
        
        // Bonus for content categories relevant to urban professionals
        when (content.category) {
            ContentCategory.STRESS_MANAGEMENT -> score += 20 // High priority for busy professionals
            ContentCategory.MINDFULNESS -> score += 15
            ContentCategory.FITNESS -> score += 10 // Desk workers need movement
            ContentCategory.NUTRITION -> score += 10
            ContentCategory.SLEEP -> score += 15 // Important for busy schedules
            ContentCategory.GENERAL_WELLNESS -> score += 5
        }
        
        // Bonus for shorter content (busy professionals prefer quick reads)
        when (content) {
            is HealthContent.Article -> {
                if (content.readingTimeMinutes <= 5) score += 10
            }
            is HealthContent.Video -> {
                if (content.durationMinutes <= 10) score += 10
            }
            is HealthContent.News -> {
                // Recent news gets higher score
                val daysSincePublished = (System.currentTimeMillis() - content.publishedDate.time) / (1000 * 60 * 60 * 24)
                if (daysSincePublished <= 1) score += 15
                if (content.isBreaking) score += 20
            }
        }
        
        Log.d(TAG, "Relevance score for '${content.title}': $score")
        return score
    }
}