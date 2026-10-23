package com.example.security

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoBackupManager {

    private const val HEADER_MAGIC = "PFIN_ENC_V1"
    private const val ITERATION_COUNT = 65536
    private const val KEY_LENGTH = 256
    private const val GCM_TAG_LENGTH = 128
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val DEFAULT_PASSPHRASE = "FinanceTrackerSecureLocalVaultKey2026"

    /**
     * Encrypts plaintext JSON and writes to a file in the app's cache directory.
     */
    fun createEncryptedBackupFile(
        context: Context,
        jsonPayload: String,
        userPassword: String? = null
    ): File {
        val passwordToUse = if (!userPassword.isNullOrBlank()) userPassword else DEFAULT_PASSPHRASE

        val secureRandom = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        secureRandom.nextBytes(salt)

        val iv = ByteArray(IV_LENGTH)
        secureRandom.nextBytes(iv)

        val keySpec = PBEKeySpec(passwordToUse.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val ciphertext = cipher.doFinal(jsonPayload.toByteArray(Charsets.UTF_8))

        val headerBytes = HEADER_MAGIC.toByteArray(Charsets.UTF_8)
        val totalSize = 4 + headerBytes.size + SALT_LENGTH + IV_LENGTH + ciphertext.size
        val byteBuffer = ByteBuffer.allocate(totalSize)

        byteBuffer.putInt(headerBytes.size)
        byteBuffer.put(headerBytes)
        byteBuffer.put(salt)
        byteBuffer.put(iv)
        byteBuffer.put(ciphertext)

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Personal_Finance_Backup_$timeStamp.enc"
        val outputFile = File(context.cacheDir, fileName)

        FileOutputStream(outputFile).use { fos ->
            fos.write(byteBuffer.array())
        }

        return outputFile
    }

    /**
     * Decrypts an encrypted backup file and returns the plain JSON string.
     */
    fun decryptBackupFile(
        backupBytes: ByteArray,
        userPassword: String? = null
    ): String {
        val passwordToUse = if (!userPassword.isNullOrBlank()) userPassword else DEFAULT_PASSPHRASE
        val buffer = ByteBuffer.wrap(backupBytes)

        val headerLen = buffer.int
        if (headerLen <= 0 || headerLen > 64) {
            throw IllegalArgumentException("Invalid backup file header format")
        }

        val headerBytes = ByteArray(headerLen)
        buffer.get(headerBytes)
        val header = String(headerBytes, Charsets.UTF_8)
        if (header != HEADER_MAGIC) {
            throw IllegalArgumentException("Unrecognized backup file signature: $header")
        }

        val salt = ByteArray(SALT_LENGTH)
        buffer.get(salt)

        val iv = ByteArray(IV_LENGTH)
        buffer.get(iv)

        val cipherText = ByteArray(buffer.remaining())
        buffer.get(cipherText)

        val keySpec = PBEKeySpec(passwordToUse.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        val decryptedBytes = cipher.doFinal(cipherText)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}
