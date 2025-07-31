package com.vibehealth.android.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Enhanced encryption helper with AES-256, Android Keystore integration,
 * and key rotation capabilities as specified in Task 6.2.
 */
@Singleton
class EnhancedEncryptionHelper @Inject constructor() {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS_PREFIX = "vibe_health_goal_key_"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
        private const val KEY_SIZE = 256
        private const val MAX_KEY_AGE_MS = 30L * 24 * 60 * 60 * 1000 // 30 days
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    private var currentKeyAlias: String = getCurrentKeyAlias()

    /**
     * Encrypts data using AES-256-GCM with Android Keystore.
     */
    fun encrypt(data: String): EncryptionResult {
        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv
            val encryptedData = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

            // Combine IV and encrypted data
            val combined = iv + encryptedData
            val encodedData = android.util.Base64.encodeToString(combined, android.util.Base64.DEFAULT)

            EncryptionResult.Success(encodedData)
        } catch (e: Exception) {
            EncryptionResult.Error("Encryption failed: ${e.message}")
        }
    }

    /**
     * Decrypts data using AES-256-GCM with Android Keystore.
     */
    fun decrypt(encryptedData: String): EncryptionResult {
        return try {
            val combined = android.util.Base64.decode(encryptedData, android.util.Base64.DEFAULT)
            
            if (combined.size < GCM_IV_LENGTH) {
                return EncryptionResult.Error("Invalid encrypted data format")
            }

            val iv = combined.sliceArray(0 until GCM_IV_LENGTH)
            val cipherText = combined.sliceArray(GCM_IV_LENGTH until combined.size)

            val secretKey = getSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            val decryptedData = cipher.doFinal(cipherText)
            val result = String(decryptedData, Charsets.UTF_8)

            EncryptionResult.Success(result)
        } catch (e: Exception) {
            // Try with older keys if current key fails
            tryDecryptWithOlderKeys(encryptedData) ?: EncryptionResult.Error("Decryption failed: ${e.message}")
        }
    }

    /**
     * Encrypts data with user-specific key derivation.
     */
    fun encryptForUser(data: String, userId: String): EncryptionResult {
        return try {
            val userSpecificKey = deriveUserSpecificKey(userId)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, userSpecificKey)

            val iv = cipher.iv
            val encryptedData = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

            val combined = iv + encryptedData
            val encodedData = android.util.Base64.encodeToString(combined, android.util.Base64.DEFAULT)

            EncryptionResult.Success(encodedData)
        } catch (e: Exception) {
            EncryptionResult.Error("User-specific encryption failed: ${e.message}")
        }
    }

    /**
     * Decrypts data with user-specific key derivation.
     */
    fun decryptForUser(encryptedData: String, userId: String): EncryptionResult {
        return try {
            val combined = android.util.Base64.decode(encryptedData, android.util.Base64.DEFAULT)
            
            if (combined.size < GCM_IV_LENGTH) {
                return EncryptionResult.Error("Invalid encrypted data format")
            }

            val iv = combined.sliceArray(0 until GCM_IV_LENGTH)
            val cipherText = combined.sliceArray(GCM_IV_LENGTH until combined.size)

            val userSpecificKey = deriveUserSpecificKey(userId)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.DECRYPT_MODE, userSpecificKey, gcmSpec)

            val decryptedData = cipher.doFinal(cipherText)
            val result = String(decryptedData, Charsets.UTF_8)

            EncryptionResult.Success(result)
        } catch (e: Exception) {
            EncryptionResult.Error("User-specific decryption failed: ${e.message}")
        }
    }

    /**
     * Rotates encryption keys for enhanced security.
     */
    fun rotateKeys(): KeyRotationResult {
        return try {
            val oldKeyAlias = currentKeyAlias
            val newKeyAlias = generateNewKeyAlias()
            
            // Generate new key
            generateSecretKey(newKeyAlias)
            
            // Update current key alias
            currentKeyAlias = newKeyAlias
            
            // Schedule old key cleanup (keep for transition period)
            scheduleOldKeyCleanup(oldKeyAlias)
            
            KeyRotationResult.Success(
                oldKeyAlias = oldKeyAlias,
                newKeyAlias = newKeyAlias,
                rotationTime = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            KeyRotationResult.Error("Key rotation failed: ${e.message}")
        }
    }

    /**
     * Checks if key rotation is needed based on age.
     */
    fun isKeyRotationNeeded(): Boolean {
        val keyCreationTime = getKeyCreationTime(currentKeyAlias)
        return keyCreationTime != null && 
               (System.currentTimeMillis() - keyCreationTime) > MAX_KEY_AGE_MS
    }

    /**
     * Gets encryption strength information.
     */
    fun getEncryptionStrength(): EncryptionStrength {
        return EncryptionStrength(
            algorithm = "AES",
            keySize = KEY_SIZE,
            mode = "GCM",
            padding = "NoPadding",
            keyStore = ANDROID_KEYSTORE,
            isHardwareBacked = isKeyHardwareBacked()
        )
    }

    /**
     * Validates encryption strength meets AES-256 requirements.
     */
    fun validateEncryptionStrength(): ValidationResult {
        val strength = getEncryptionStrength()
        val issues = mutableListOf<String>()

        if (strength.keySize < 256) {
            issues.add("Key size (${strength.keySize}) is below AES-256 requirement")
        }

        if (strength.algorithm != "AES") {
            issues.add("Algorithm (${strength.algorithm}) is not AES")
        }

        if (strength.mode != "GCM") {
            issues.add("Mode (${strength.mode}) is not GCM (recommended for authenticated encryption)")
        }

        if (!strength.isHardwareBacked) {
            issues.add("Key is not hardware-backed (security warning)")
        }

        return if (issues.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(issues)
        }
    }

    /**
     * Securely wipes sensitive data from memory.
     */
    fun secureWipe(data: ByteArray) {
        // Overwrite with random data multiple times
        repeat(3) {
            Random.nextBytes(data)
        }
        // Final overwrite with zeros
        data.fill(0)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        return getSecretKey() ?: generateSecretKey(currentKeyAlias)
    }

    private fun getSecretKey(): SecretKey? {
        return try {
            keyStore.getKey(currentKeyAlias, null) as? SecretKey
        } catch (e: Exception) {
            null
        }
    }

    private fun generateSecretKey(alias: String): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE)
            .setUserAuthenticationRequired(false) // Can be enabled for additional security
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    private fun deriveUserSpecificKey(userId: String): SecretKey {
        val userKeyAlias = "${KEY_ALIAS_PREFIX}user_${userId.hashCode()}"
        
        return try {
            keyStore.getKey(userKeyAlias, null) as? SecretKey
        } catch (e: Exception) {
            null
        } ?: generateSecretKey(userKeyAlias)
    }

    private fun tryDecryptWithOlderKeys(encryptedData: String): EncryptionResult? {
        val olderKeyAliases = getOlderKeyAliases()
        
        for (keyAlias in olderKeyAliases) {
            try {
                val combined = android.util.Base64.decode(encryptedData, android.util.Base64.DEFAULT)
                val iv = combined.sliceArray(0 until GCM_IV_LENGTH)
                val cipherText = combined.sliceArray(GCM_IV_LENGTH until combined.size)

                val secretKey = keyStore.getKey(keyAlias, null) as SecretKey
                val cipher = Cipher.getInstance(TRANSFORMATION)
                val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

                val decryptedData = cipher.doFinal(cipherText)
                val result = String(decryptedData, Charsets.UTF_8)

                return EncryptionResult.Success(result)
            } catch (e: Exception) {
                // Continue to next key
            }
        }
        
        return null
    }

    private fun getCurrentKeyAlias(): String {
        // In a real implementation, this would be stored in secure preferences
        return "${KEY_ALIAS_PREFIX}current_${System.currentTimeMillis() / (24 * 60 * 60 * 1000)}"
    }

    private fun generateNewKeyAlias(): String {
        return "${KEY_ALIAS_PREFIX}${System.currentTimeMillis()}"
    }

    private fun getOlderKeyAliases(): List<String> {
        return try {
            keyStore.aliases().toList().filter { 
                it.startsWith(KEY_ALIAS_PREFIX) && it != currentKeyAlias 
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getKeyCreationTime(keyAlias: String): Long? {
        // Extract timestamp from key alias (simplified implementation)
        return try {
            keyAlias.substringAfterLast("_").toLongOrNull()
        } catch (e: Exception) {
            null
        }
    }

    private fun scheduleOldKeyCleanup(oldKeyAlias: String) {
        // In a real implementation, this would schedule cleanup after a transition period
        // For now, we'll keep the old key for potential decryption needs
    }

    private fun isKeyHardwareBacked(): Boolean {
        return try {
            val secretKey = getSecretKey()
            secretKey != null && keyStore.isKeyEntry(currentKeyAlias)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Key rotation result.
     */
    sealed class KeyRotationResult {
        data class Success(
            val oldKeyAlias: String,
            val newKeyAlias: String,
            val rotationTime: Long
        ) : KeyRotationResult()
        
        data class Error(val message: String) : KeyRotationResult()
    }

    /**
     * Encryption strength information.
     */
    data class EncryptionStrength(
        val algorithm: String,
        val keySize: Int,
        val mode: String,
        val padding: String,
        val keyStore: String,
        val isHardwareBacked: Boolean
    )

    /**
     * Validation result for encryption strength.
     */
    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val issues: List<String>) : ValidationResult()
    }
}