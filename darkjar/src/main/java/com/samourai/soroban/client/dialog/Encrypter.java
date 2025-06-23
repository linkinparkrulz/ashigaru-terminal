package com.samourai.soroban.client.dialog;

public interface Encrypter {
  String decrypt(byte[] payload) throws Exception;

  byte[] encrypt(String payload) throws Exception;
}
