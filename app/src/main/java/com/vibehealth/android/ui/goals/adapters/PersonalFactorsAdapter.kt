package com.vibehealth.android.ui.goals.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vibehealth.android.databinding.ItemPersonalFactorBinding

/**
 * Adapter for displaying personal factors that influence goal calculations.
 * 
 * This adapter shows the user's personal information (age, gender, activity level)
 * that was used in the goal calculation process for transparency.
 */
class PersonalFactorsAdapter(
    private val personalFactors: List<String>
) : RecyclerView.Adapter<PersonalFactorsAdapter.PersonalFactorViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonalFactorViewHolder {
        val binding = ItemPersonalFactorBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PersonalFactorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PersonalFactorViewHolder, position: Int) {
        holder.bind(personalFactors[position])
    }

    override fun getItemCount(): Int = personalFactors.size

    /**
     * ViewHolder for personal factor items.
     */
    class PersonalFactorViewHolder(
        private val binding: ItemPersonalFactorBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Bind personal factor data to the view.
         * 
         * @param factor Personal factor text to display
         */
        fun bind(factor: String) {
            with(binding) {
                textPersonalFactor.text = factor
                
                // Set appropriate icon based on factor type
                val iconRes = when {
                    factor.contains("Age", ignoreCase = true) -> 
                        com.vibehealth.android.R.drawable.ic_age
                    factor.contains("Gender", ignoreCase = true) -> 
                        com.vibehealth.android.R.drawable.ic_gender
                    factor.contains("Activity", ignoreCase = true) -> 
                        com.vibehealth.android.R.drawable.ic_activity_level
                    factor.contains("Height", ignoreCase = true) -> 
                        com.vibehealth.android.R.drawable.ic_height
                    factor.contains("Weight", ignoreCase = true) -> 
                        com.vibehealth.android.R.drawable.ic_weight
                    else -> com.vibehealth.android.R.drawable.ic_person
                }
                
                iconPersonalFactor.setImageResource(iconRes)
                
                // Set content description for accessibility
                root.contentDescription = "Personal factor: $factor"
            }
        }
    }
}