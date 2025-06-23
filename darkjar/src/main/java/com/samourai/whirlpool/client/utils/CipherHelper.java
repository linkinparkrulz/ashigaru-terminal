package com.samourai.whirlpool.client.utils;

import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;

public class CipherHelper {

    public static RSAKeyParameters loadRSAPublicKey(final InputStream pubKey) throws IOException {
        
        try (final PEMParser parser = new PEMParser(new InputStreamReader(pubKey, StandardCharsets.UTF_8))) {
            Object obj = parser.readObject();
            final JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            final RSAPublicKey publicKey = (RSAPublicKey) converter.getPublicKey((org.bouncycastle.asn1.x509.SubjectPublicKeyInfo) obj);

            final BigInteger modulus = publicKey.getModulus();
            final BigInteger publicExponent = publicKey.getPublicExponent();
            
            return new RSAKeyParameters(false, modulus, publicExponent);
        }
    }
}
