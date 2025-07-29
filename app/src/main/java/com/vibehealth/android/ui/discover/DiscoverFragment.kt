package com.vibehealth.android.ui.discover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.vibehealth.android.databinding.FragmentDiscoverBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Discover fragment - placeholder for future health articles and videos
 * This will be implemented in future stories
 */
@AndroidEntryPoint
class DiscoverFragment : Fragment() {
    
    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // TODO: Implement health articles and videos discovery
        // This will be part of future stories
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}