package com.samourai.wallet.crypto;

import com.samourai.wallet.crypto.impl.ECDHKeySet;
import com.samourai.wallet.crypto.impl.EncryptedMessage;
import com.samourai.wallet.util.RandomUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.provider.JCEECPrivateKey;
import org.bouncycastle.jce.provider.JCEECPublicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;

public class CryptoUtil {
    private static final Logger log = LoggerFactory.getLogger(CryptoUtil.class);

    private static final String ALGO_HMAC = "HmacSHA512";
    private static final String ALGO_HASH = "SHA256";
    private static final String ALGO_CRYPTO = "AES";
    private static final String CYPHER = "AES/CTR/NoPadding";

    private static CryptoUtil instance;
    private final RandomUtil randomUtil = RandomUtil.getInstance();
    private String provider;

    public static CryptoUtil getInstance(Provider provider) {
        if (instance == null) {
            try {
                Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            } catch (Exception e) {
                log.error("", e);
            }
            instance = new CryptoUtil(provider.getName());
        }
        return instance;
    }

    protected CryptoUtil(String provider) {
        this.provider = provider;
    }

    public byte[] encrypt (byte[] data, ECDHKeySet keySet) throws Exception {
        byte[] iv = randomUtil.nextBytes(16);
        byte[] enc = encryptAES_CTR(data, keySet.encryptionKey, iv);
        byte[] hmac = getHMAC(enc, keySet.hmacKey, iv);
        return new EncryptedMessage(iv, hmac, enc).serialize();
    }

    public byte[] encrypt (String data, ECDHKeySet keySet) throws Exception {
        return encrypt(data.getBytes("UTF-8"), keySet);
    }

    public byte[] decrypt (byte[] encrypted, ECDHKeySet ecdhKeySet) throws Exception {
        EncryptedMessage message = EncryptedMessage.unserialize(encrypted);
        checkHMAC(message.hmac, message.payload, ecdhKeySet.hmacKey, message.iv);
        byte[] data = decryptAES_CTR(message.payload, ecdhKeySet.encryptionKey, message.iv);
        return data;
    }

    public String decryptString (byte[] encrypted, ECDHKeySet ecdhKeySet) throws Exception {
        byte[] data = decrypt(encrypted, ecdhKeySet);
        return new String(data, "UTF-8");
    }

    public ECDHKeySet getSharedSecret (ECKey keyServer, ECKey keyClient) throws Exception {
        AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC", provider);
        parameters.init(new ECGenParameterSpec("secp256k1"));
        ECParameterSpec ecParameters = parameters.getParameterSpec(ECParameterSpec.class);

        ECPrivateKeySpec specPrivate = new ECPrivateKeySpec(keyServer.getPrivKey(), ecParameters);
        ECPublicKeySpec specPublic = new ECPublicKeySpec(new ECPoint(keyClient.getPubKeyPoint().getXCoord().toBigInteger(), keyClient.getPubKeyPoint()
                .getYCoord().toBigInteger()), ecParameters);

        KeyFactory kf = KeyFactory.getInstance("EC", provider);
        ECPrivateKey privateKey = (ECPrivateKey) kf.generatePrivate(specPrivate);
        ECPublicKey publicKey = (ECPublicKey) kf.generatePublic(specPublic);

        JCEECPrivateKey ecPrivKey = new JCEECPrivateKey(privateKey);
        JCEECPublicKey ecPubKey = new JCEECPublicKey(publicKey);

        KeyAgreement aKeyAgree = KeyAgreement.getInstance("ECDH", provider);
        aKeyAgree.init(ecPrivKey);
        aKeyAgree.doPhase(ecPubKey, true);

        return new ECDHKeySet(aKeyAgree.generateSecret(), provider);
    }

    public byte[] createSignature (ECKey pubkey, byte[] data) throws NoSuchProviderException, NoSuchAlgorithmException {
        return pubkey.sign(Sha256Hash.of(data)).encodeToDER();
    }

    public boolean verifySignature (ECKey pubkey, byte[] data, byte[] signature) throws NoSuchProviderException, NoSuchAlgorithmException {
        try {
            MessageDigest hashHandler = MessageDigest.getInstance(ALGO_HASH, provider);
            hashHandler.update(data);
            byte[] hash = hashHandler.digest();
            return pubkey.verify(hash, signature);
        } catch(Exception e) {
            log.error("", e);
            return false;
        }
    }

    //

    private byte[] decryptAES_CTR (byte[] data, byte[] keyBytes, byte[] ivBytes) throws Exception {
        SecretKeySpec key = new SecretKeySpec(keyBytes, ALGO_CRYPTO);
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

        Cipher cipher = Cipher.getInstance(CYPHER, provider);
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        return cipher.doFinal(data);
    }

    private byte[] encryptAES_CTR (byte[] data, byte[] keyBytes, byte[] ivBytes) throws Exception {
        SecretKeySpec key = new SecretKeySpec(keyBytes, ALGO_CRYPTO);
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

        Cipher cipher = Cipher.getInstance(CYPHER, provider);
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        return cipher.doFinal(data);
    }

    private byte[] getHMAC (byte[] data, byte[] keyBytes, byte[] iv) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGO_HMAC);
        Mac mac = Mac.getInstance(ALGO_HMAC, provider);
        mac.init(keySpec);
        byte[] ivData = ArrayUtils.addAll(iv, data);
        return mac.doFinal(ivData);
    }

    private void checkHMAC (byte[] hmac, byte[] data, byte[] keyBytes, byte[] iv) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGO_HMAC);
        Mac mac = Mac.getInstance(ALGO_HMAC, provider);
        mac.init(keySpec);
        byte[] ivData = ArrayUtils.addAll(iv, data);
        byte[] result = mac.doFinal(ivData);

        if (!MessageDigest.isEqual(result, hmac)){
            throw new RuntimeException("HMAC does not match..");
        }
    }
}
