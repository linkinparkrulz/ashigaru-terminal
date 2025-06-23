package com.samourai.wallet.crypto

import org.bouncycastle.crypto.CipherParameters
import org.bouncycastle.crypto.Digest
import org.bouncycastle.crypto.Mac
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.bouncycastle.crypto.macs.HMac
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import org.bouncycastle.jcajce.provider.util.DigestFactory
import kotlin.experimental.xor

class PKCS5S2ParametersGeneratorKtx @JvmOverloads constructor(digest: Digest? = DigestFactory.getDigest("sha1")) :

    PKCS5S2ParametersGenerator() {
    private val hMac: Mac
    private val state: ByteArray
    private  fun FKtx(
        S: ByteArray?,
        c: Int,
        iBuf: ByteArray,
        out: ByteArray,
        outOff: Int
    ) {
        require(c != 0) { "iteration count must be at least 1." }
        if (S != null) {
            hMac.update(S, 0, S.size)
        }
        hMac.update(iBuf, 0, iBuf.size)
        hMac.doFinal(state, 0)
        System.arraycopy(state, 0, out, outOff, state.size)
        for (count in 1 until c) {
            hMac.update(state, 0, state.size)
            hMac.doFinal(state, 0)
            for (j in state.indices) {
                out[outOff + j] = out[outOff + j] xor state[j]
            }
        }
    }

    private  fun generateDerivedKeyKtx(
        dkLen: Int
    ): ByteArray {
        val hLen = hMac.macSize
        val l = (dkLen + hLen - 1) / hLen
        val iBuf = ByteArray(4)
        val outBytes = ByteArray(l * hLen)
        var outPos = 0
        val param: CipherParameters = KeyParameter(password)
        hMac.init(param)
        for (i in 1..l) {
            // Increment the value in 'iBuf'
            var pos = 3
            while (++iBuf[pos] == 0.toByte()) {
                --pos
            }
            FKtx(salt, iterationCount, iBuf, outBytes, outPos)
            outPos += hLen
        }
        return outBytes
    }

    /**
     * Generate a key parameter derived from the password, salt, and iteration
     * count we are currently initialised with.
     *
     * @param keySize the size of the key we want (in bits)
     * @return a KeyParameter object.
     */
     fun generateDerivedParametersKtx(
        keySize: Int
    ): CipherParameters {
        var keySize = keySize
        keySize = keySize / 8
        val dKey = generateDerivedKeyKtx(keySize)
        return KeyParameter(dKey, 0, keySize)
    }

    /**
     * Generate a key with initialisation vector parameter derived from
     * the password, salt, and iteration count we are currently initialised
     * with.
     *
     * @param keySize the size of the key we want (in bits)
     * @param ivSize the size of the iv we want (in bits)
     * @return a ParametersWithIV object.
     */
     fun generateDerivedParametersKtx(
        keySize: Int,
        ivSize: Int
    ): CipherParameters {
        var keySize = keySize
        var ivSize = ivSize
        keySize = keySize / 8
        ivSize = ivSize / 8
        val dKey = generateDerivedKeyKtx(keySize + ivSize)
        return ParametersWithIV(KeyParameter(dKey, 0, keySize), dKey, keySize, ivSize)
    }

    /**
     * Generate a key parameter for use with a MAC derived from the password,
     * salt, and iteration count we are currently initialised with.
     *
     * @param keySize the size of the key we want (in bits)
     * @return a KeyParameter object.
     */
     fun generateDerivedMacParametersKtx(
        keySize: Int
    ): CipherParameters {
        return generateDerivedParametersKtx(keySize)
    }

    /**
     * construct a PKCS5 Scheme 2 Parameters generator.
     */
    init {
        hMac = HMac(digest)
        state = ByteArray(hMac.macSize)
    }
}
