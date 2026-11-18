package com.sudokucat.app.data.prefs

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Cifra la chiave Gemini a riposo con Android Keystore (AES/GCM).
 *
 * Limite noto: nessuna chiave salvata sul dispositivo è estraibile da un
 * dispositivo rooted o da un APK compromesso. Per una produzione ad alto rischio
 * la chiave va custodita su un backend, che qui non è obbligatorio.
 */
class SecureKeyStore {

    private val alias = "sudoku_cat_gemini_key"

    private fun keyStore(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun secretKey(): SecretKey {
        val ks = keyStore()
        (ks.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    fun encrypt(plainText: String): String {
        if (plainText.isBlank()) return ""
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val iv = cipher.iv
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val ivText = Base64.encodeToString(iv, Base64.NO_WRAP)
            val dataText = Base64.encodeToString(encrypted, Base64.NO_WRAP)
            "v1:$ivText:$dataText"
        }.getOrDefault("")
    }

    fun decrypt(encoded: String): String {
        if (encoded.isBlank()) return ""
        return runCatching {
            val parts = encoded.split(':')
            if (parts.size != 3 || parts[0] != "v1") return ""
            val iv = Base64.decode(parts[1], Base64.NO_WRAP)
            val data = Base64.decode(parts[2], Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(data), Charsets.UTF_8)
        }.getOrDefault("")
    }
}
