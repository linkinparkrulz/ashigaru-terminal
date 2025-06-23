package com.samourai.wallet.crypto.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class ECDHKeySet {
    public byte[] masterKey;
    public byte[] encryptionKey;
    public byte[] hmacKey;

    public ECDHKeySet (byte[] masterKey, String provider) throws NoSuchProviderException, NoSuchAlgorithmException {
        this.masterKey = masterKey;

        MessageDigest hash = MessageDigest.getInstance("SHA256", provider);
        byte[] t = new byte[masterKey.length + 1];
        System.arraycopy(masterKey, 0, t, 0, masterKey.length);

        // encryptionKey
        t[t.length - 1] = 0x00;
        hash.update(t);
        encryptionKey = hash.digest();

        // hmacKey
        t[t.length - 1] = 0x01;
        hash.update(t);
        hmacKey = hash.digest();
    }

}