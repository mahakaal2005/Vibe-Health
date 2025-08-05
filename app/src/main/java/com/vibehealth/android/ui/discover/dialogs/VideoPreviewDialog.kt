package com.vibehealth.android.ui.discover.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vibehealth.android.ui.discover.models.HealthContent

/**
 * VideoPreviewDialog - Simple dialog for video preview
 * Placeholder implementation for video preview functionality
 */
class VideoPreviewDialog : DialogFragment() {
    
    companion object {
        private const val ARG_VIDEO_TITLE = "video_title"
        private const val ARG_VIDEO_DESCRIPTION = "video_description"
        
        fun newInstance(video: HealthContent.Video): VideoPreviewDialog {
            return VideoPreviewDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_VIDEO_TITLE, video.title)
                    putString(ARG_VIDEO_DESCRIPTION, video.description)
                }
            }
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = arguments?.getString(ARG_VIDEO_TITLE) ?: "Video Preview"
        val description = arguments?.getString(ARG_VIDEO_DESCRIPTION) ?: ""
        
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage("$description\n\nVideo preview functionality will be implemented in a future update.")
            .setPositiveButton("Watch Full Video") { _, _ ->
                // Navigate to full video viewer
                dismiss()
            }
            .setNegativeButton("Close") { _, _ ->
                dismiss()
            }
            .create()
    }
}