package com.samourai.wallet.crypto.impl;

import org.bouncycastle.util.Arrays;

public class EncryptedMessage {
    private static final int IV_LENGTH = 16;
    private static final int HMAC_LENGTH = 64;

    public byte[] hmac;
    public byte[] payload;
    public byte[] iv;

    public EncryptedMessage (byte[] iv, byte[] hmac, byte[] payload) {
        this.iv = iv;
        this.hmac = hmac;
        this.payload = payload;
    }

    public byte[] serialize() throws Exception {
        if (iv.length != IV_LENGTH) {
            throw new Exception("Invalid IV length");
        }
        if (hmac.length != HMAC_LENGTH) {
            throw new Exception("Invalid HMAC length");
        }
        return Arrays.concatenate(iv, hmac, payload);
    }

    public static EncryptedMessage unserialize(byte[] serialized) {
        byte[] iv = Arrays.copyOfRange(serialized, 0, IV_LENGTH);
        byte[] hmac = Arrays.copyOfRange(serialized, IV_LENGTH, IV_LENGTH+HMAC_LENGTH);
        byte[] payload = Arrays.copyOfRange(serialized, IV_LENGTH+HMAC_LENGTH, serialized.length);
        return new EncryptedMessage(iv, hmac, payload);
    }
}