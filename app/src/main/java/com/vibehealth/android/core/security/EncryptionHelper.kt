package com.vibehealth.android.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

/**
 * Helper class for AES-256 encryption with key rotation support
 */
class EncryptionHelper(private val context: Context) {

    companion object {
        private const val KEYSTORE_ALIAS = "vibe_health_db_key"
        private const val SHARED_PREFS_NAME = "vibe_health_encrypted_prefs"
        private const val DB_KEY_PREF = "database_encryption_key"
        private const val KEY_VERSION_PREF = "encryption_key_version"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_LENGTH = 256
        private const val IV_LENGTH = 12
        private const val TAG_LENGTH = 128
        
        private var instance: EncryptionHelper? = null
        
        fun getInstance(context: Context): EncryptionHelper {
            return instance ?: synchronized(this) {
                instance ?: EncryptionHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedSharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            SHARED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Get or generate database encryption passphrase
     */
    fun getDatabasePassphrase(): String {
        val existingKey = encryptedSharedPreferences.getString(DB_KEY_PREF, null)
        
        return if (existingKey != null) {
            existingKey
        } else {
            val newKey = generateSecureKey()
            encryptedSharedPreferences.edit()
                .putString(DB_KEY_PREF, newKey)
                .putInt(KEY_VERSION_PREF, 1)
                .apply()
            newKey
        }
    }

    /**
     * Generate a secure random key for database encryption
     */
    private fun generateSecureKey(): String {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(KEY_LENGTH)
        val secretKey = keyGenerator.generateKey()
        return Base64.encodeToString(secretKey.encoded, Base64.NO_WRAP)
    }

    /**
     * Encrypt data using AES-256-GCM
     */
    fun encrypt(data: String): EncryptionResult {
        return try {
            val key = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            
            // Generate random IV
            val iv = ByteArray(IV_LENGTH)
            SecureRandom().nextBytes(iv)
            val gcmSpec = GCMParameterSpec(TAG_LENGTH, iv)
            
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)
            val encryptedData = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            
            // Combine IV and encrypted data
            val combined = iv + encryptedData
            val encodedData = Base64.encodeToString(combined, Base64.NO_WRAP)
            
            EncryptionResult.Success(encodedData)
        } catch (e: Exception) {
            EncryptionResult.Error("Encryption failed: ${e.message}")
        }
    }

    /**
     * Decrypt data using AES-256-GCM
     */
    fun decrypt(encryptedData: String): EncryptionResult {
        return try {
            val key = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            
            // Decode and separate IV from encrypted data
            val combined = Base64.decode(encryptedData, Base64.NO_WRAP)
            val iv = combined.sliceArray(0 until IV_LENGTH)
            val encrypted = combined.sliceArray(IV_LENGTH until combined.size)
            
            val gcmSpec = GCMParameterSpec(TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)
            
            val decryptedData = cipher.doFinal(encrypted)
            val result = String(decryptedData, Charsets.UTF_8)
            
            EncryptionResult.Success(result)
        } catch (e: Exception) {
            EncryptionResult.Error("Decryption failed: ${e.message}")
        }
    }

    /**
     * Get or create secret key for encryption
     */
    private fun getOrCreateSecretKey(): SecretKey {
        val keyString = getDatabasePassphrase()
        val keyBytes = Base64.decode(keyString, Base64.NO_WRAP)
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Rotate encryption key (for enhanced security)
     */
    fun rotateKey(): Boolean {
        return try {
            val currentVersion = encryptedSharedPreferences.getInt(KEY_VERSION_PREF, 1)
            val newKey = generateSecureKey()
            
            encryptedSharedPreferences.edit()
                .putString(DB_KEY_PREF, newKey)
                .putInt(KEY_VERSION_PREF, currentVersion + 1)
                .apply()
            
            true
        } catch (e: Exception) {
            android.util.Log.e("EncryptionHelper", "Key rotation failed", e)
            false
        }
    }

    /**
     * Get current key version
     */
    fun getKeyVersion(): Int {
        return encryptedSharedPreferences.getInt(KEY_VERSION_PREF, 1)
    }

    /**
     * Clear all encryption keys (for testing or reset)
     */
    fun clearKeys() {
        encryptedSharedPreferences.edit().clear().apply()
    }

    /**
     * Validate encryption setup
     */
    fun validateEncryption(): Boolean {
        return try {
            val testData = "test_encryption_validation"
            val encrypted = encrypt(testData)
            
            if (encrypted is EncryptionResult.Success) {
                val decrypted = decrypt(encrypted.data)
                decrypted is EncryptionResult.Success && decrypted.data == testData
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("EncryptionHelper", "Encryption validation failed", e)
            false
        }
    }

    /**
     * Generate hash for data integrity checking
     */
    fun generateDataHash(data: String): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(data.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(hashBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            android.util.Log.e("EncryptionHelper", "Hash generation failed", e)
            ""
        }
    }

    /**
     * Verify data integrity using hash
     */
    fun verifyDataIntegrity(data: String, expectedHash: String): Boolean {
        val actualHash = generateDataHash(data)
        return actualHash.isNotEmpty() && actualHash == expectedHash
    }
}

/**
 * Sealed class for encryption results
 */
sealed class EncryptionResult {
    data class Success(val data: String) : EncryptionResult()
    data class Error(val message: String) : EncryptionResult()
    
    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error
    
    fun getDataOrNull(): String? = (this as? Success)?.data
    fun getErrorMessage(): String? = (this as? Error)?.message
}