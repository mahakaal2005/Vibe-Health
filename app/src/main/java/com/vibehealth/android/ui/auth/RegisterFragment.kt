package com.vibehealth.android.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputLayout
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import com.vibehealth.android.MainActivity
import com.vibehealth.android.R
import com.vibehealth.android.core.accessibility.AccessibilityHelper
import com.vibehealth.android.core.responsive.ResponsiveLayoutHelper
import com.vibehealth.android.databinding.FragmentRegisterBinding
import com.vibehealth.android.domain.auth.AuthState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Register fragment implementing exact design specifications from design document
 */
@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()
    
    // Flag to prevent duplicate success messages
    private var hasShownSuccess = false

    @Inject
    lateinit var accessibilityHelper: AccessibilityHelper

    @Inject
    lateinit var responsiveLayoutHelper: ResponsiveLayoutHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAccessibility()
        setupResponsiveLayout()
        setupUI()
        setupObservers()
        setupClickListeners()
    }

    /**
     * Set up UI components and form validation
     */
    private fun setupUI() {
        // Ensure password toggle is always visible
        forcePasswordToggleVisibility()
        
        // Set up real-time email validation
        binding.emailEditText.addTextChangedListener { text ->
            val email = text.toString()
            viewModel.updateEmail(email)
        }

        // Set up real-time password validation and strength indicator
        binding.passwordEditText.addTextChangedListener { text ->
            val password = text.toString()
            viewModel.updatePassword(password)
            
            // Show/hide and update password strength indicator
            if (password.isNotEmpty()) {
                val passwordStrength = com.vibehealth.android.core.validation.PasswordStrength.evaluate(password)
                binding.passwordStrengthView.updatePasswordStrength(passwordStrength)
            } else {
                binding.passwordStrengthView.visibility = View.GONE
            }

            // Also validate confirm password if it has content
            val confirmPassword = binding.confirmPasswordEditText.text.toString()
            if (confirmPassword.isNotEmpty()) {
                viewModel.updateConfirmPassword(confirmPassword)
            }
        }

        // Set up real-time confirm password validation
        binding.confirmPasswordEditText.addTextChangedListener { text ->
            val confirmPassword = text.toString()
            viewModel.updateConfirmPassword(confirmPassword)
        }

        // Set up keyboard actions
        binding.emailEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_NEXT) {
                binding.passwordEditText.requestFocus()
                true
            } else {
                false
            }
        }

        binding.passwordEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_NEXT) {
                binding.confirmPasswordEditText.requestFocus()
                true
            } else {
                false
            }
        }

        binding.confirmPasswordEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                if (binding.termsCheckbox.isChecked) {
                    binding.createAccountButton.performClick()
                } else {
                    binding.termsCheckbox.requestFocus()
                }
                true
            } else {
                false
            }
        }
    }

    /**
     * Set up observers for ViewModel LiveData
     */
    private fun setupObservers() {
        // Observe authentication state
        viewModel.authState.observe(viewLifecycleOwner) { authState ->
            when (authState) {
                is AuthState.Authenticated -> {
                    // Prevent duplicate success messages
                    if (!hasShownSuccess) {
                        hasShownSuccess = true
                        // Show success feedback before navigating
                        _binding?.let { binding ->
                            binding.successFeedbackView.visibility = View.VISIBLE
                            binding.successFeedbackView.showSuccess(
                                message = "Sign up successful!",
                                autoHide = false
                            )
                        }
                        // Show "Redirecting..." message after success
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            _binding?.let { binding ->
                                binding.successFeedbackView.showSuccess(
                                    message = "Redirecting to dashboard...",
                                    autoHide = false
                                )
                            }
                        }, 1200)
                        
                        // Navigate to main app after success animation - use Handler for safety
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            if (_binding != null && isAdded) {
                                navigateToMain()
                            }
                        }, 1800)
                    }
                }
                is AuthState.Error -> {
                    // Error handling is done through UI state
                }
                is AuthState.Loading -> {
                    // Loading state is handled through UI state
                }
                is AuthState.NotAuthenticated -> {
                    // Stay on register screen
                }
            }
        }

        // Observe UI state for form validation and loading
        viewLifecycleOwner.lifecycleScope.launch { 
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    // Update loading state
                    binding.createAccountButton.isEnabled = !uiState.isLoading
                    binding.progressBar.visibility = if (uiState.isLoading) View.VISIBLE else View.GONE
                    binding.createAccountButton.visibility = if (uiState.isLoading) View.INVISIBLE else View.VISIBLE

                    // Update email validation
                    binding.emailInputLayout.error = uiState.emailError
                    binding.emailInputLayout.isErrorEnabled = uiState.emailError != null

                    // Update password validation - CRITICAL: Set error AFTER ensuring toggle visibility
                    // First, force the password toggle to be visible
                    binding.passwordInputLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
                    binding.confirmPasswordInputLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
                    
                    // Then set the error state
                    binding.passwordInputLayout.error = uiState.passwordError
                    binding.passwordInputLayout.isErrorEnabled = uiState.passwordError != null
                    
                    // Force toggle visibility again after error is set (this is the key fix)
                    binding.passwordInputLayout.post {
                        binding.passwordInputLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
                        binding.passwordInputLayout.isEndIconVisible = true
                        binding.passwordInputLayout.setEndIconTintList(
                            androidx.core.content.ContextCompat.getColorStateList(requireContext(), R.color.text_input_end_icon_tint)
                        )
                    }
                    
                    binding.confirmPasswordInputLayout.post {
                        binding.confirmPasswordInputLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
                        binding.confirmPasswordInputLayout.isEndIconVisible = true
                        binding.confirmPasswordInputLayout.setEndIconTintList(
                            androidx.core.content.ContextCompat.getColorStateList(requireContext(), R.color.text_input_end_icon_tint)
                        )
                    }

                    // Show general error message
                    if (uiState.errorMessage != null && binding.errorMessage.visibility != View.VISIBLE) {
                        binding.errorMessage.text = uiState.errorMessage
                        binding.errorMessage.visibility = View.VISIBLE
                        binding.errorMessage.startAnimation(
                            android.view.animation.AnimationUtils.loadAnimation(context, R.anim.error_fade_in)
                        )
                        // Auto-hide error after 5 seconds
                        binding.errorMessage.postDelayed({
                            binding.errorMessage.visibility = View.GONE
                            viewModel.clearError()
                        }, 5000)
                    } else if (uiState.errorMessage == null) {
                        binding.errorMessage.visibility = View.GONE
                    }
                }
            }
        }
    }

    /**
     * Set up click listeners for buttons and links
     */
    private fun setupClickListeners() {
        // Create account button with micro-interactions
        binding.createAccountButton.setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    view.startAnimation(android.view.animation.AnimationUtils.loadAnimation(context, R.anim.button_press))
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    view.startAnimation(android.view.animation.AnimationUtils.loadAnimation(context, R.anim.button_release))
                }
            }
            false
        }

        binding.createAccountButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()

            if (binding.termsCheckbox.isChecked) {
                viewModel.signUp(email, password, confirmPassword)
            } else {
                binding.termsError.visibility = View.VISIBLE
                binding.termsError.text = "Please accept the Terms & Conditions"
                binding.termsError.startAnimation(
                    android.view.animation.AnimationUtils.loadAnimation(context, R.anim.error_fade_in)
                )
            }
        }

        // Set up sign in link with clickable span
        setupSignInLink()

        // Terms checkbox listener
        binding.termsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.termsError.visibility = View.GONE
            }
        }
    }

    /**
     * Set up sign in link with clickable span for only "Sign In" text
     */
    private fun setupSignInLink() {
        val fullText = getString(R.string.already_have_account)
        val spannableString = SpannableString(fullText)
        
        // Find the "Sign In" part in the text
        val signInText = "Sign In"
        val startIndex = fullText.indexOf(signInText)
        val endIndex = startIndex + signInText.length
        
        if (startIndex != -1) {
            // Create clickable span for "Sign In" only
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: android.view.View) {
                    try {
                        findNavController().navigate(R.id.action_register_to_login)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                override fun updateDrawState(ds: android.text.TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false // Remove underline
                }
            }
            
            // Apply clickable span and color to "Sign In" text only
            spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannableString.setSpan(
                ForegroundColorSpan(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.soft_coral)),
                startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        
        binding.signInLink.text = spannableString
        binding.signInLink.movementMethod = LinkMovementMethod.getInstance()
    }

    /**
     * Force password toggle visibility - workaround for Material Design bug
     */
    private fun forcePasswordToggleVisibility() {
        binding.passwordInputLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
        binding.confirmPasswordInputLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
        
        // Additional workaround - set the toggle drawable directly
        binding.passwordInputLayout.post {
            binding.passwordInputLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
            binding.passwordInputLayout.isEndIconVisible = true
        }
        
        binding.confirmPasswordInputLayout.post {
            binding.confirmPasswordInputLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
            binding.confirmPasswordInputLayout.isEndIconVisible = true
        }
    }

    /**
     * Navigate to main application with smooth transition
     */
    private fun navigateToMain() {
        // Add a subtle loading state during transition
        _binding?.let { binding ->
            binding.successFeedbackView.animate()
                .alpha(0.8f)
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(200)
                .start()
        }
        
        // Navigate with smooth animation after a brief moment
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (isAdded && _binding != null) {
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                
                // Add smooth slide up transition
                startActivity(intent)
                requireActivity().finish()
                requireActivity().overridePendingTransition(R.anim.slide_up_fade_in, R.anim.slide_down_fade_out)
            }
        }, 300)
    }

    /**
     * Set up accessibility features for WCAG 2.1 Level AA compliance
     */
    private fun setupAccessibility() {
        // Set content descriptions
        accessibilityHelper.setContentDescription(
            binding.logo,
            "Vibe Health logo",
            "Welcome to Vibe Health"
        )

        accessibilityHelper.setContentDescription(
            binding.emailEditText,
            "Email address input field",
            "Enter your email address"
        )

        accessibilityHelper.setContentDescription(
            binding.passwordEditText,
            "Password input field",
            "Enter your password"
        )

        accessibilityHelper.setContentDescription(
            binding.confirmPasswordEditText,
            "Confirm password input field",
            "Re-enter your password"
        )

        accessibilityHelper.setContentDescription(
            binding.createAccountButton,
            "Create account button",
            "Tap to create your account"
        )

        accessibilityHelper.setContentDescription(
            binding.signInLink,
            "Sign in link",
            "Tap to sign in to your account"
        )

        // Ensure minimum touch targets
        accessibilityHelper.ensureMinimumTouchTarget(binding.createAccountButton)
        accessibilityHelper.ensureMinimumTouchTarget(binding.signInLink)

        // Set up keyboard navigation
        accessibilityHelper.setupKeyboardNavigation(binding.root as ViewGroup)

        // Set up focus indicators
        accessibilityHelper.setupFocusIndicators(binding.emailEditText)
        accessibilityHelper.setupFocusIndicators(binding.passwordEditText)
        accessibilityHelper.setupFocusIndicators(binding.confirmPasswordEditText)
        accessibilityHelper.setupFocusIndicators(binding.createAccountButton)

        // Apply dynamic font scaling
        accessibilityHelper.applyDynamicFontScaling(binding.createAccountHeading, 24f)
        accessibilityHelper.applyDynamicFontScaling(binding.subtitle, 16f)

        // Apply high contrast if needed
        accessibilityHelper.applyHighContrastIfNeeded(binding.createAccountHeading)
        accessibilityHelper.applyHighContrastIfNeeded(binding.subtitle)
    }

    /**
     * Set up responsive layout for different screen sizes
     */
    private fun setupResponsiveLayout() {
        // Apply safe area insets
        responsiveLayoutHelper.applySafeAreaInsets(binding.root)

        // Adjust layout for screen size
        responsiveLayoutHelper.adjustLayoutForScreenSize(binding.root)

        // Handle keyboard visibility
        responsiveLayoutHelper.handleKeyboardVisibility(binding.root) { isVisible ->
            if (isVisible) {
                // Scroll to focused input when keyboard appears
                val focusedView = binding.root.findFocus()
                focusedView?.let { view ->
                    binding.root.post {
                        binding.root.scrollTo(0, view.bottom)
                    }
                }
            }
        }

        // Handle display cutout
        if (responsiveLayoutHelper.supportsEdgeToEdge()) {
            responsiveLayoutHelper.handleDisplayCutout(binding.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hasShownSuccess = false
        _binding = null
    }
}
