/*
* Copyright 2020 Matthew Nelson
*
* https://github.com/05nelsonm
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
* */
package com.samourai.wallet.crypto

import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.params.KeyParameter
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.bouncycastle.util.encoders.Base64
import org.bouncycastle.util.encoders.DecoderException
import org.bouncycastle.util.encoders.EncoderException
import java.nio.charset.CharacterCodingException

/**
 * Methods for encrypting/decrypting data in a manner compliant with OpenSSL such that
 * they can be used interchangeably.
 *
 * OpenSSL Encryption from command line:
 *
 *   echo "Hello World!" | openssl aes-256-cbc -e -a -p -salt -pbkdf2 -iter 25000 -k password
 *
 * Terminal output (-p shows the following):
 *   salt=F71F01EC4171ACDF
 *   key=AF9344D72520323D210C440BA015526DABA0D22AD6247DFACF7D3F5B0F724A23
 *   iv =1125D64C744EDE615CE3B8AD55C1581C
 *   U2FsdGVkX1/3HwHsQXGs37PixswoJihPWATaxph4OVQ=
 *
 * OpenSSL Decryption from command line:
 *
 *   echo "U2FsdGVkX1/3HwHsQXGs37PixswoJihPWATaxph4OVQ=" | openssl aes-256-cbc -d -a -p -salt -pbkdf2 -iter 25000 -k password
 * */
class KOpenSSL {

    companion object {
        const val SALTED = "Salted__"

        @JvmStatic
        fun isSalted(chars: CharSequence): Boolean {
            return try {
                Base64
                        .decode(chars.lines().joinToString(""))
                        .copyOfRange(0, 8)
                        .contentEquals(SALTED.toByteArray())
            } catch (e: Exception) {
                false
            }
        }

        @JvmStatic
        fun isValidUTF8(input: ByteArray): Boolean {
            return try {
                Charset.forName("UTF-8").newDecoder().decode(ByteBuffer.wrap(input))
                true
            } catch (e: CharacterCodingException) {
                false
            }
        }
    }

    /**
     * Decrypts a string value.
     *
     * @throws [ArrayIndexOutOfBoundsException] IvParameterSpec argument passed has negative length
     * @throws [BadPaddingException]
     * @throws [CancellationException] if coroutine is cancelled
     * @throws [CharacterCodingException] if decryption failed and bytes were not valid UTF-8,
     *   most probably due to a bad password or incorrect hash iterations
     * @throws [IllegalArgumentException] if the input is not a valid Base64 encoded string,
     *   there is an error copying byte ranges, parameters for obtaining the KeySpec were not met,
     *   parameters for obtaining the IvParameterSpec were not met, parameters for obtaining the
     *   SecretKeySpec were not met
     * @throws [IllegalBlockSizeException]
     * @throws [IndexOutOfBoundsException] if error when copying byte ranges
     * @throws [InvalidAlgorithmParameterException]
     * @throws [InvalidKeyException] if the SecretKey used in Cipher.init is invalid
     * @throws [InvalidKeySpecException] if the SecretKey could not be generated
     * @throws [NoSuchAlgorithmException] if PBKDF2WithHmacSHA256, or AES/CBC/NoPadding algo could
     *   not be retrieved
     * @throws [NoSuchPaddingException] if AES/CBC/NoPadding could not be retrieved
     * */
    @Throws(
            ArrayIndexOutOfBoundsException::class,
            BadPaddingException::class,
            CharacterCodingException::class,
            DecoderException::class,
            IllegalArgumentException::class,
            IllegalBlockSizeException::class,
            IndexOutOfBoundsException::class,
            InvalidAlgorithmParameterException::class,
            InvalidKeyException::class,
            InvalidKeySpecException::class,
            NoSuchAlgorithmException::class,
            NoSuchPaddingException::class
    )
    @JvmOverloads
    fun decrypt_AES256CBC_PBKDF2_HMAC_SHA256(
            password: String,
            hashIterations: Int,
            stringToDecrypt: String,
            printDetails: Boolean = false
    ): String {
        val encryptedBytes = Base64.decode(stringToDecrypt.lines().joinToString(""))

        // Salt is bytes 8 - 15
        val salt = encryptedBytes.copyOfRange(8, 16)
        if (printDetails) {
            println("Salt: ${salt.joinToString("") { "%02X".format(it) }}")
        }

        // Derive 48 byte key
        val components = getSecretKeyComponents(password, salt, hashIterations)
        if (printDetails) {
            println("Key: ${components.key.joinToString("") { "%02X".format(it) }}")
            println("IV: ${components.iv.joinToString("") { "%02X".format(it) }}")
        }

        // Cipher Text is bytes 16 - end of the encrypted bytes
        val cipherText = encryptedBytes.copyOfRange(16, encryptedBytes.size)

        // Decrypt the Cipher Text and manually remove padding after
        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, components.getSecretKeySpec(), components.getIvParameterSpec())
        val decrypted = try {
            cipher.doFinal(cipherText)
        } finally {
            components.clearValues()
        }

        if (!isValidUTF8(decrypted)) {
            throw CharacterCodingException()
        }

        // Last byte of the decrypted text is the number of padding bytes needed to remove
        val plaintext = decrypted.copyOfRange(0, decrypted.size - decrypted.last().toInt())

        return plaintext.toString(Charsets.UTF_8).trim()
                .also {
                    if (printDetails) {
                        println(it)
                    }
                }
    }

    /**
     * Encrypts a string value.
     *
     * @throws [ArrayIndexOutOfBoundsException] IvParameterSpec argument passed has negative length
     * @throws [AssertionError] there is an error encoding to Base64
     * @throws [BadPaddingException]
     * @throws [CancellationException] if coroutine is cancelled
     * @throws [IllegalArgumentException] there is an error encoding to Base64, there is an error
     *   copying byte ranges, parameters for obtaining the KeySpec were not met, parameters for
     *   obtaining the IvParameterSpec were not met, parameters for obtaining the SecretKeySpec
     *   were not met
     * @throws [IllegalBlockSizeException]
     * @throws [IndexOutOfBoundsException] if error when copying byte ranges
     * @throws [InvalidAlgorithmParameterException]
     * @throws [InvalidKeyException] if the SecretKey used in Cipher.init is invalid
     * @throws [InvalidKeySpecException] if the SecretKey could not be generated
     * @throws [NoSuchAlgorithmException] if PBKDF2WithHmacSHA256, or AES/CBC/PKCS5Padding algo
     *   could not be retrieved
     * @throws [NoSuchPaddingException] if AES/CBC/NoPadding could not be retrieved
     * */
    @Throws(
            ArrayIndexOutOfBoundsException::class,
            AssertionError::class,
            BadPaddingException::class,
            EncoderException::class,
            IllegalArgumentException::class,
            IllegalBlockSizeException::class,
            IndexOutOfBoundsException::class,
            InvalidAlgorithmParameterException::class,
            InvalidKeyException::class,
            InvalidKeySpecException::class,
            NoSuchAlgorithmException::class,
            NoSuchPaddingException::class
    )
    @JvmOverloads
    fun encrypt_AES256CBC_PBKDF2_HMAC_SHA256(
            password: String,
            hashIterations: Int,
            stringToEncrypt: String,
            printDetails: Boolean = false
    ): String {
        val salt = SecureRandom().generateSeed(8)
        if (printDetails) {
            println("Salt: ${salt.joinToString("") { "%02X".format(it) }}")
        }

        // Derive 48 byte key
        val components = getSecretKeyComponents(password, salt, hashIterations)
        if (printDetails) {
            println("Key: ${components.key.joinToString("") { "%02X".format(it) }}")
            println("IV: ${components.iv.joinToString("") { "%02X".format(it) }}")
        }

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, components.getSecretKeySpec(), components.getIvParameterSpec())
        val cipherText = try {
            cipher.doFinal(stringToEncrypt.toByteArray())
        } finally {
            components.clearValues()
        }

        return String(Base64.encode(SALTED.toByteArray() + salt + cipherText))
                .replace("(.{64})".toRegex(), "$1\n")
                .also {
                    if (printDetails) {
                        println(it)
                    }
                }
    }

    private class SecretKeyComponents(val key: ByteArray, val iv: ByteArray) {

        fun getSecretKeySpec(): SecretKeySpec =
                SecretKeySpec(key, "AES")

        fun getIvParameterSpec(): IvParameterSpec =
                IvParameterSpec(iv)

        fun clearValues() {
            key.fill('*'.toByte())
            iv.fill('*'.toByte())
        }
    }

    private fun getSecretKeyComponents(
            password: String,
            salt: ByteArray,
            hashIterations: Int
    ): SecretKeyComponents =
            PKCS5S2ParametersGeneratorKtx(SHA256Digest()).let { generator ->
                generator.init(password.toByteArray(), salt, hashIterations)
                (generator.generateDerivedMacParametersKtx(48 * 8) as KeyParameter).key.let { secretKey ->
                    SecretKeyComponents(
                            // Decryption Key is bytes 0 - 31 of the derived secret key
                            key = secretKey.copyOfRange(0, 32),

                            // Input Vector is bytes 32 - 47 of the derived secret key
                            iv = secretKey.copyOfRange(32, secretKey.size)
                    ).also {
                        generator.password.fill('*'.toByte())
                        secretKey.fill('*'.toByte())
                    }
                }
            }

}


