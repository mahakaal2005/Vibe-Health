package com.vibehealth.android.core.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for EncryptionHelper
 */
@RunWith(AndroidJUnit4::class)
class EncryptionHelperTest {

    private lateinit var encryptionHelper: EncryptionHelper
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        encryptionHelper = EncryptionHelper.getInstance(context)
        // Clear any existing keys for clean test state
        encryptionHelper.clearKeys()
    }

    @Test
    fun getDatabasePassphrase_shouldGenerateConsistentPassphrase() {
        // When
        val passphrase1 = encryptionHelper.getDatabasePassphrase()
        val passphrase2 = encryptionHelper.getDatabasePassphrase()

        // Then
        assertNotNull(passphrase1)
        assertNotNull(passphrase2)
        assertEquals(passphrase1, passphrase2)
        assertTrue(passphrase1.isNotEmpty())
    }

    @Test
    fun encrypt_shouldEncryptDataSuccessfully() {
        // Given
        val originalData = "This is sensitive user data that needs encryption"

        // When
        val result = encryptionHelper.encrypt(originalData)

        // Then
        assertTrue(result.isSuccess())
        assertNotNull(result.getDataOrNull())
        assertNotEquals(originalData, result.getDataOrNull())
    }

    @Test
    fun decrypt_shouldDecryptDataSuccessfully() {
        // Given
        val originalData = "This is sensitive user data that needs encryption"
        val encryptResult = encryptionHelper.encrypt(originalData)
        assertTrue(encryptResult.isSuccess())

        // When
        val decryptResult = encryptionHelper.decrypt(encryptResult.getDataOrNull()!!)

        // Then
        assertTrue(decryptResult.isSuccess())
        assertEquals(originalData, decryptResult.getDataOrNull())
    }

    @Test
    fun encryptDecrypt_withEmptyString_shouldWorkCorrectly() {
        // Given
        val originalData = ""

        // When
        val encryptResult = encryptionHelper.encrypt(originalData)
        val decryptResult = encryptionHelper.decrypt(encryptResult.getDataOrNull()!!)

        // Then
        assertTrue(encryptResult.isSuccess())
        assertTrue(decryptResult.isSuccess())
        assertEquals(originalData, decryptResult.getDataOrNull())
    }

    @Test
    fun encryptDecrypt_withSpecialCharacters_shouldWorkCorrectly() {
        // Given
        val originalData = "Special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?`~"

        // When
        val encryptResult = encryptionHelper.encrypt(originalData)
        val decryptResult = encryptionHelper.decrypt(encryptResult.getDataOrNull()!!)

        // Then
        assertTrue(encryptResult.isSuccess())
        assertTrue(decryptResult.isSuccess())
        assertEquals(originalData, decryptResult.getDataOrNull())
    }

    @Test
    fun encryptDecrypt_withUnicodeCharacters_shouldWorkCorrectly() {
        // Given
        val originalData = "Unicode: 你好世界 🌍 émojis 🔒 ñáéíóú"

        // When
        val encryptResult = encryptionHelper.encrypt(originalData)
        val decryptResult = encryptionHelper.decrypt(encryptResult.getDataOrNull()!!)

        // Then
        assertTrue(encryptResult.isSuccess())
        assertTrue(decryptResult.isSuccess())
        assertEquals(originalData, decryptResult.getDataOrNull())
    }

    @Test
    fun decrypt_withInvalidData_shouldReturnError() {
        // Given
        val invalidEncryptedData = "invalid_encrypted_data"

        // When
        val result = encryptionHelper.decrypt(invalidEncryptedData)

        // Then
        assertTrue(result.isError())
        assertNotNull(result.getErrorMessage())
    }

    @Test
    fun decrypt_withTamperedData_shouldReturnError() {
        // Given
        val originalData = "This data will be tampered with"
        val encryptResult = encryptionHelper.encrypt(originalData)
        assertTrue(encryptResult.isSuccess())
        
        // Tamper with the encrypted data
        val tamperedData = encryptResult.getDataOrNull()!! + "tampered"

        // When
        val decryptResult = encryptionHelper.decrypt(tamperedData)

        // Then
        assertTrue(decryptResult.isError())
    }

    @Test
    fun rotateKey_shouldGenerateNewKey() {
        // Given
        val originalPassphrase = encryptionHelper.getDatabasePassphrase()
        val originalVersion = encryptionHelper.getKeyVersion()

        // When
        val rotationResult = encryptionHelper.rotateKey()

        // Then
        assertTrue(rotationResult)
        val newPassphrase = encryptionHelper.getDatabasePassphrase()
        val newVersion = encryptionHelper.getKeyVersion()
        
        assertNotEquals(originalPassphrase, newPassphrase)
        assertEquals(originalVersion + 1, newVersion)
    }

    @Test
    fun validateEncryption_shouldReturnTrueForValidSetup() {
        // When
        val isValid = encryptionHelper.validateEncryption()

        // Then
        assertTrue(isValid)
    }

    @Test
    fun generateDataHash_shouldGenerateConsistentHash() {
        // Given
        val data = "Test data for hashing"

        // When
        val hash1 = encryptionHelper.generateDataHash(data)
        val hash2 = encryptionHelper.generateDataHash(data)

        // Then
        assertNotNull(hash1)
        assertNotNull(hash2)
        assertEquals(hash1, hash2)
        assertTrue(hash1.isNotEmpty())
    }

    @Test
    fun generateDataHash_withDifferentData_shouldGenerateDifferentHashes() {
        // Given
        val data1 = "Test data 1"
        val data2 = "Test data 2"

        // When
        val hash1 = encryptionHelper.generateDataHash(data1)
        val hash2 = encryptionHelper.generateDataHash(data2)

        // Then
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun verifyDataIntegrity_withValidHash_shouldReturnTrue() {
        // Given
        val data = "Test data for integrity check"
        val hash = encryptionHelper.generateDataHash(data)

        // When
        val isValid = encryptionHelper.verifyDataIntegrity(data, hash)

        // Then
        assertTrue(isValid)
    }

    @Test
    fun verifyDataIntegrity_withInvalidHash_shouldReturnFalse() {
        // Given
        val data = "Test data for integrity check"
        val invalidHash = "invalid_hash"

        // When
        val isValid = encryptionHelper.verifyDataIntegrity(data, invalidHash)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun verifyDataIntegrity_withTamperedData_shouldReturnFalse() {
        // Given
        val originalData = "Original data"
        val hash = encryptionHelper.generateDataHash(originalData)
        val tamperedData = "Tampered data"

        // When
        val isValid = encryptionHelper.verifyDataIntegrity(tamperedData, hash)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun clearKeys_shouldRemoveAllKeys() {
        // Given
        val originalPassphrase = encryptionHelper.getDatabasePassphrase()
        assertNotNull(originalPassphrase)

        // When
        encryptionHelper.clearKeys()

        // Then
        val newPassphrase = encryptionHelper.getDatabasePassphrase()
        assertNotEquals(originalPassphrase, newPassphrase)
        assertEquals(1, encryptionHelper.getKeyVersion()) // Should reset to version 1
    }

    @Test
    fun encryptionResult_successMethods_shouldWorkCorrectly() {
        // Given
        val successResult = EncryptionResult.Success("test_data")
        val errorResult = EncryptionResult.Error("test_error")

        // Then
        assertTrue(successResult.isSuccess())
        assertFalse(successResult.isError())
        assertEquals("test_data", successResult.getDataOrNull())
        assertEquals(null, successResult.getErrorMessage())

        assertFalse(errorResult.isSuccess())
        assertTrue(errorResult.isError())
        assertEquals(null, errorResult.getDataOrNull())
        assertEquals("test_error", errorResult.getErrorMessage())
    }

    @Test
    fun multipleEncryptions_shouldProduceDifferentResults() {
        // Given
        val data = "Same data encrypted multiple times"

        // When
        val result1 = encryptionHelper.encrypt(data)
        val result2 = encryptionHelper.encrypt(data)

        // Then
        assertTrue(result1.isSuccess())
        assertTrue(result2.isSuccess())
        // Results should be different due to random IV
        assertNotEquals(result1.getDataOrNull(), result2.getDataOrNull())
        
        // But both should decrypt to the same original data
        val decrypt1 = encryptionHelper.decrypt(result1.getDataOrNull()!!)
        val decrypt2 = encryptionHelper.decrypt(result2.getDataOrNull()!!)
        
        assertTrue(decrypt1.isSuccess())
        assertTrue(decrypt2.isSuccess())
        assertEquals(data, decrypt1.getDataOrNull())
        assertEquals(data, decrypt2.getDataOrNull())
    }

    @Test
    fun largeData_shouldEncryptAndDecryptCorrectly() {
        // Given
        val largeData = "A".repeat(10000) // 10KB of data

        // When
        val encryptResult = encryptionHelper.encrypt(largeData)
        val decryptResult = encryptionHelper.decrypt(encryptResult.getDataOrNull()!!)

        // Then
        assertTrue(encryptResult.isSuccess())
        assertTrue(decryptResult.isSuccess())
        assertEquals(largeData, decryptResult.getDataOrNull())
    }
}