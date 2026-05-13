package com.tether.app.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.*
import java.security.spec.ECGenParameterSpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * End-to-End Encryption using X25519 for key exchange and AES-256-GCM for encryption
 */
@Singleton
class EncryptionManager @Inject constructor(
    private val secureStorage: SecureStorage
) {
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "tether_master_key"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        
        private val JSON_SERIALIZER = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }

    private var localKeyPair: KeyPair? = null

    /**
     * Initialize or load existing key pair from Android Keystore
     */
    fun initializeKeyPair() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)

            if (keyStore.containsAlias(KEY_ALIAS)) {
                // Load existing key
                val privateKeyEntry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
                localKeyPair = KeyPair(
                    privateKeyEntry.certificate.publicKey,
                    privateKeyEntry.privateKey
                )
            } else {
                // Generate new key pair using ECDH with Curve25519
                generateKeyPair()
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to initialize key pair", e)
        }
    }

    private fun generateKeyPair() {
        try {
            // Use Bouncy Castle's X25519 implementation
            val keyPairGenerator = X25519KeyPairGenerator()
            keyPairGenerator.init(null)
            val kp = keyPairGenerator.generateKeyPair()
            
            val privateKeyParams = kp.private as X25519PrivateKeyParameters
            val publicKeyParams = kp.public as X25519PublicKeyParameters
            
            // Store private key securely
            secureStorage.savePrivateKey(privateKeyParams.encoded)
            secureStorage.savePublicKey(publicKeyParams.encoded)
            
            localKeyPair = createKeyPairFromBytes(
                publicKeyParams.encoded,
                privateKeyParams.encoded
            )
        } catch (e: Exception) {
            throw RuntimeException("Failed to generate X25519 key pair", e)
        }
    }

    private fun createKeyPairFromBytes(publicKeyBytes: ByteArray, privateKeyBytes: ByteArray): KeyPair {
        // For Android Keystore compatibility, we use ECDH P-256 as fallback
        val keyFactory = KeyFactory.getInstance("EC")
        val spec = ECGenParameterSpec("secp256r1")
        val kg = KeyPairGenerator.getInstance("EC", ANDROID_KEYSTORE)
        
        val parameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_AGREE_KEY or KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setAlgorithmParameterSpec(spec)
            .setUserAuthenticationRequired(false)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        
        kg.init(parameterSpec)
        val kp = kg.generateKeyPair()
        
        return kp
    }

    /**
     * Get local public key for sharing with partner
     */
    fun getLocalPublicKey(): ByteArray? {
        return secureStorage.getPublicKey()
    }

    /**
     * Derive shared secret from local private key and partner's public key
     */
    fun deriveSharedSecret(partnerPublicKey: ByteArray): ByteArray {
        try {
            val localPrivateKeyBytes = secureStorage.getPrivateKey()
                ?: throw IllegalStateException("Local private key not found")

            // Use Bouncy Castle for X25519 key agreement
            val localPrivateKey = X25519PrivateKeyParameters(localPrivateKeyBytes, 0)
            val partnerPubKey = X25519PublicKeyParameters(partnerPublicKey, 0)
            
            val sharedSecret = ByteArray(X25519PrivateKeyParameters.KEY_SIZE)
            localPrivateKey.calculateAgreement(partnerPubKey, sharedSecret, 0)
            
            return sharedSecret
        } catch (e: Exception) {
            throw RuntimeException("Failed to derive shared secret", e)
        }
    }

    /**
     * Encrypt data using AES-256-GCM with the derived shared secret
     */
    fun encrypt(data: String, sharedSecret: ByteArray): EncryptedData {
        try {
            val secretKey = createSecretKey(sharedSecret)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(data.toByteArray(StandardCharsets.UTF_8))
            
            return EncryptedData(
                ciphertext = encryptedBytes,
                iv = iv,
                algorithm = "X25519-AES256-GCM"
            )
        } catch (e: Exception) {
            throw RuntimeException("Encryption failed", e)
        }
    }

    /**
     * Decrypt data using AES-256-GCM with the derived shared secret
     */
    fun decrypt(encryptedData: EncryptedData, sharedSecret: ByteArray): String {
        try {
            val secretKey = createSecretKey(sharedSecret)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, encryptedData.iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            
            val decryptedBytes = cipher.doFinal(encryptedData.ciphertext)
            return String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            throw RuntimeException("Decryption failed", e)
        }
    }

    private fun createSecretKey(sharedSecret: ByteArray): SecretKey {
        return javax.crypto.spec.SecretKeySpec(sharedSecret, 0, 32, "AES")
    }

    /**
     * Encrypt a canvas event (stroke, sticker, etc.)
     */
    inline fun <reified T> encryptEvent(event: T, partnerPublicKey: ByteArray): String {
        val json = JSON_SERIALIZER.encodeToString(event)
        val sharedSecret = deriveSharedSecret(partnerPublicKey)
        val encrypted = encrypt(json, sharedSecret)
        return encrypted.toBase64()
    }

    /**
     * Decrypt a canvas event
     */
    inline fun <reified T> decryptEvent(encryptedData: String, partnerPublicKey: ByteArray): T {
        val decoded = EncryptedData.fromBase64(encryptedData)
        val sharedSecret = deriveSharedSecret(partnerPublicKey)
        val json = decrypt(decoded, sharedSecret)
        return JSON_SERIALIZER.decodeFromString(json)
    }

    /**
     * Clear all keys (for account deletion)
     */
    fun clearKeys() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
            }
            secureStorage.clearKeys()
            localKeyPair = null
        } catch (e: Exception) {
            // Ignore errors during cleanup
        }
    }
}

/**
 * Encrypted data container
 */
data class EncryptedData(
    val ciphertext: ByteArray,
    val iv: ByteArray,
    val algorithm: String
) {
    fun toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        outputStream.write(iv.size)
        outputStream.write(iv)
        outputStream.write(ciphertext.size)
        outputStream.write(ciphertext)
        return android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.DEFAULT)
    }

    companion object {
        fun fromBase64(base64: String): EncryptedData {
            val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
            var offset = 0
            val ivLength = bytes[offset++]
            val iv = bytes.copyOfRange(offset, offset + ivLength)
            offset += ivLength
            val ciphertextLength = bytes[offset++]
            val ciphertext = bytes.copyOfRange(offset, offset + ciphertextLength)
            return EncryptedData(ciphertext, iv, "X25519-AES256-GCM")
        }
    }
}

/**
 * Secure storage for cryptographic keys
 */
@Singleton
class SecureStorage @Inject constructor(
    private val dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
) {
    companion object {
        private val PRIVATE_KEY = androidx.datastore.preferences.core.stringPreferencesKey("private_key")
        private val PUBLIC_KEY = androidx.datastore.preferences.core.stringPreferencesKey("public_key")
        private val PARTNER_PUBLIC_KEY = androidx.datastore.preferences.core.stringPreferencesKey("partner_public_key")
    }

    suspend fun savePrivateKey(key: ByteArray) {
        val encoded = android.util.Base64.encodeToString(key, android.util.Base64.DEFAULT)
        dataStore.edit { prefs ->
            prefs[PRIVATE_KEY] = encoded
        }
    }

    suspend fun savePublicKey(key: ByteArray) {
        val encoded = android.util.Base64.encodeToString(key, android.util.Base64.DEFAULT)
        dataStore.edit { prefs ->
            prefs[PUBLIC_KEY] = encoded
        }
    }

    suspend fun savePartnerPublicKey(key: ByteArray) {
        val encoded = android.util.Base64.encodeToString(key, android.util.Base64.DEFAULT)
        dataStore.edit { prefs ->
            prefs[PARTNER_PUBLIC_KEY] = encoded
        }
    }

    fun getPrivateKey(): ByteArray? {
        return try {
            val prefs = dataStore.data.replay().firstOrNull()[PRIVATE_KEY] ?: return null
            android.util.Base64.decode(prefs, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }

    fun getPublicKey(): ByteArray? {
        return try {
            val prefs = dataStore.data.replay().firstOrNull()[PUBLIC_KEY] ?: return null
            android.util.Base64.decode(prefs, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }

    fun getPartnerPublicKey(): ByteArray? {
        return try {
            val prefs = dataStore.data.replay().firstOrNull()[PARTNER_PUBLIC_KEY] ?: return null
            android.util.Base64.decode(prefs, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun clearKeys() {
        dataStore.edit { prefs ->
            prefs.remove(PRIVATE_KEY)
            prefs.remove(PUBLIC_KEY)
            prefs.remove(PARTNER_PUBLIC_KEY)
        }
    }
}
