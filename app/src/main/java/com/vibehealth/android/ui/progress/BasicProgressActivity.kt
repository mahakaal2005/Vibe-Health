package com.vibehealth.android.ui.progress

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vibehealth.android.R

class BasicProgressActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basic_progress)
        
        // Handle supportive message and celebration context from intent
        val supportiveMessage = intent.getStringExtra(EXTRA_SUPPORTIVE_MESSAGE)
        val celebrationContext = intent.getStringExtra(EXTRA_CELEBRATION_CONTEXT)
        
        // TODO: Use these messages in the UI
    }
    
    companion object {
        private const val EXTRA_SUPPORTIVE_MESSAGE = "supportive_message"
        private const val EXTRA_CELEBRATION_CONTEXT = "celebration_context"
        
        fun createIntent(
            context: Context,
            supportiveMessage: String? = null,
            celebrationContext: String? = null
        ): Intent {
            return Intent(context, BasicProgressActivity::class.java).apply {
                supportiveMessage?.let { putExtra(EXTRA_SUPPORTIVE_MESSAGE, it) }
                celebrationContext?.let { putExtra(EXTRA_CELEBRATION_CONTEXT, it) }
            }
        }
    }
}