package com.example.data.security

import android.util.Base64
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object EncryptionManager {
  private const val ALGORITHM = "AES/GCM/NoPadding"
  private const val TAG_LENGTH_BITS = 128
  private const val IV_LENGTH_BYTES = 12
  private const val SALT_LENGTH_BYTES = 16
  private const val ITERATION_COUNT = 65536
  private const val KEY_LENGTH_BITS = 256

  // Default device key fallback if user hasn't set custom passphrase
  private const val DEFAULT_PASSPHRASE = "omni_vault_device_master_key_secure_2025"

  private val secureRandom = SecureRandom()

  fun deriveKey(passphrase: String, salt: ByteArray): SecretKey {
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val spec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH_BITS)
    val tmp = factory.generateSecret(spec)
    return SecretKeySpec(tmp.encoded, "AES")
  }

  fun encrypt(plainText: String, passphrase: String = DEFAULT_PASSPHRASE): String {
    try {
      val salt = ByteArray(SALT_LENGTH_BYTES)
      secureRandom.nextBytes(salt)

      val iv = ByteArray(IV_LENGTH_BYTES)
      secureRandom.nextBytes(iv)

      val secretKey = deriveKey(passphrase, salt)
      val cipher = Cipher.getInstance(ALGORITHM)
      val spec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

      val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

      // Structure: [salt (16)] + [iv (12)] + [cipherText (var)]
      val byteBuffer = ByteBuffer.allocate(salt.size + iv.size + cipherText.size)
      byteBuffer.put(salt)
      byteBuffer.put(iv)
      byteBuffer.put(cipherText)

      return "enc:v1:" + Base64.encodeToString(byteBuffer.array(), Base64.NO_WRAP)
    } catch (e: Exception) {
      // Return original on catastrophic failure to prevent message loss
      return plainText
    }
  }

  fun decrypt(encryptedPayload: String, passphrase: String = DEFAULT_PASSPHRASE): String {
    if (!encryptedPayload.startsWith("enc:v1:")) {
      return encryptedPayload // Plain text
    }

    try {
      val rawBase64 = encryptedPayload.removePrefix("enc:v1:")
      val decoded = Base64.decode(rawBase64, Base64.NO_WRAP)
      val byteBuffer = ByteBuffer.wrap(decoded)

      val salt = ByteArray(SALT_LENGTH_BYTES)
      byteBuffer.get(salt)

      val iv = ByteArray(IV_LENGTH_BYTES)
      byteBuffer.get(iv)

      val cipherText = ByteArray(byteBuffer.remaining())
      byteBuffer.get(cipherText)

      val secretKey = deriveKey(passphrase, salt)
      val cipher = Cipher.getInstance(ALGORITHM)
      val spec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
      cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

      val decrypted = cipher.doFinal(cipherText)
      return String(decrypted, Charsets.UTF_8)
    } catch (e: Exception) {
      return "[🔒 Encrypted Content - Key Mismatch]"
    }
  }

  fun maskKey(key: String): String {
    if (key.isBlank()) return "Not configured"
    if (key.length <= 8) return "••••••••"
    val start = key.take(4)
    val end = key.takeLast(4)
    return "$start••••••••$end"
  }
}
