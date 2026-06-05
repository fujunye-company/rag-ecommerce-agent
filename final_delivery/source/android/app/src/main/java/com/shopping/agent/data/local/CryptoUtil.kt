package com.shopping.agent.data.local

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * 密码加密工具 — 使用 AES-256-CBC + PBKDF2 密钥派生，安全存储登录密码和支付密码。
 *
 * 加密流程：PBKDF2(SHA-256, salt, 10000 迭代) → AES/CBC/PKCS5Padding → Base64(salt + iv + ciphertext)
 * 注：此方案仅用于本地演示，生产环境应使用 Android Keystore。
 */
object CryptoUtil {

    /** PBKDF2 密钥派生迭代次数 */
    private const val PBKDF2_ITERATIONS = 10_000
    /** AES 密钥长度（位） */
    private const val KEY_LENGTH = 256
    /** AES 初始向量长度（字节） */
    private const val IV_LENGTH = 16
    /** 随机盐长度（字节） */
    private const val SALT_LENGTH = 16
    /** 固定口令种子（用于派生 AES 密钥，生产环境应使用 Keystore） */
    private const val SECRET_SEED = "hermes-shopping-agent-2025-v1.0-secure-key"

    /** 懒加载：从固定种子 + 硬编码盐派生出 AES 密钥 */
    private val keySpec: SecretKeySpec by lazy {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(SECRET_SEED.toCharArray(), "hermes-fixed-salt".toByteArray(), PBKDF2_ITERATIONS, KEY_LENGTH)
        val tmp = factory.generateSecret(spec)
        SecretKeySpec(tmp.encoded, "AES")
    }

    /**
     * 加密明文密码，返回 Base64 编码的密文（包含 salt + IV + ciphertext）。
     * @param plainText 明文密码
     * @return Base64 编码的密文字符串，若加密失败则返回空字符串
     */
    fun encrypt(plainText: String): String {
        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }
            val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }

            // 使用随机盐重新派生密钥，确保每次加密的密钥不同
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(SECRET_SEED.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH)
            val tmp = factory.generateSecret(spec)
            val dynamicKey = SecretKeySpec(tmp.encoded, "AES")

            cipher.init(Cipher.ENCRYPT_MODE, dynamicKey, IvParameterSpec(iv))
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // 格式：salt(16) + iv(16) + cipherText(variable)
            val combined = ByteArray(SALT_LENGTH + IV_LENGTH + cipherText.size)
            System.arraycopy(salt, 0, combined, 0, SALT_LENGTH)
            System.arraycopy(iv, 0, combined, SALT_LENGTH, IV_LENGTH)
            System.arraycopy(cipherText, 0, combined, SALT_LENGTH + IV_LENGTH, cipherText.size)

            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * 解密密文，返回明文密码。
     * @param encryptedText Base64 编码的密文（由 [encrypt] 生成）
     * @return 明文密码，若解密失败则返回空字符串
     */
    fun decrypt(encryptedText: String): String {
        return try {
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
            if (combined.size < SALT_LENGTH + IV_LENGTH) return ""

            // 提取 salt、iv、cipherText
            val salt = combined.copyOfRange(0, SALT_LENGTH)
            val iv = combined.copyOfRange(SALT_LENGTH, SALT_LENGTH + IV_LENGTH)
            val cipherText = combined.copyOfRange(SALT_LENGTH + IV_LENGTH, combined.size)

            // 使用提取的 salt 重新派生密钥
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(SECRET_SEED.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH)
            val tmp = factory.generateSecret(spec)
            val dynamicKey = SecretKeySpec(tmp.encoded, "AES")

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, dynamicKey, IvParameterSpec(iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }
}
