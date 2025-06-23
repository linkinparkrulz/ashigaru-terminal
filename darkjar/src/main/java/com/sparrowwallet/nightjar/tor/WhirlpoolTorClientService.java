package com.sparrowwallet.nightjar.tor;

import com.samourai.tor.client.TorClientService;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.security.SecureRandom;

public class WhirlpoolTorClientService extends TorClientService {
    private static final SecureRandom secureRandom = new SecureRandom();

    @Override
    public void changeIdentity() {
        Authenticator.setDefault(new Authenticator() {
            public PasswordAuthentication getPasswordAuthentication() {
                return (new PasswordAuthentication("user", Integer.toString(secureRandom.nextInt()).toCharArray()));
            }
        });
    }
}
