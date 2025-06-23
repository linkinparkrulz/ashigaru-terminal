package com.samourai.wallet.hd;

import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.util.FormatsUtilGeneric;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

public class HD_WalletFactoryGeneric {
  private static HD_WalletFactoryGeneric instance = null;

  public static HD_WalletFactoryGeneric getInstance() {
    if(instance == null) {
      instance = new HD_WalletFactoryGeneric();
    }
    return instance;
  }

  private MnemonicCode mc;

  public HD_WalletFactoryGeneric() {
    this(Bip39English.getInstance().getMnemonicCode());
  }
  public HD_WalletFactoryGeneric(MnemonicCode mc) {
    this.mc = mc;
  }

  public HD_Wallet newWallet(String passphrase, NetworkParameters params) throws Exception {
    return newWallet(12, passphrase, params);
  }

  public HD_Wallet newWallet(int nbWords, String passphrase, NetworkParameters params) throws IOException, MnemonicException.MnemonicLengthException   {

    if((nbWords % 3 != 0) || (nbWords < 12 || nbWords > 24)) {
      nbWords = 12;
    }

    // len == 16 (12 words), len == 24 (18 words), len == 32 (24 words)
    int len = (nbWords / 3) * 4;

    if(passphrase == null) {
      passphrase = "";
    }

    SecureRandom random = new SecureRandom();
    byte seed[] = new byte[len];
    random.nextBytes(seed);

    HD_Wallet hdw = new HD_Wallet(44, mc, params, seed, passphrase);

    return hdw;
  }

  public HD_Wallet restoreWallet(String data, String passphrase, NetworkParameters params)
          throws AddressFormatException, DecoderException,
          MnemonicException.MnemonicLengthException, MnemonicException.MnemonicWordException,
          MnemonicException.MnemonicChecksumException {

    HD_Wallet hdw = null;

    if (passphrase == null) {
      passphrase = "";
    }

    if (data.matches(FormatsUtilGeneric.XPUB)) {
      String[] xpub = data.split(":");
      hdw = new HD_Wallet(params, xpub);
    } else if (data.matches(FormatsUtilGeneric.HEX) && data.length() % 4 == 0) {
      byte[] seed = Hex.decodeHex(data.toCharArray());
      hdw = new HD_Wallet(44, mc, params, seed, passphrase);
    } else {
      byte[] seed = computeSeedFromWords(data);
      hdw = new HD_Wallet(44, mc, params, seed, passphrase);
    }
    return hdw;
  }

  public byte[] computeSeedFromWords(String data) throws AddressFormatException,
      MnemonicException.MnemonicLengthException, MnemonicException.MnemonicWordException,
      MnemonicException.MnemonicChecksumException {
    data = data.toLowerCase().replaceAll("[^a-z]+", " "); // only use for BIP39 English
    List<String> words = Arrays.asList(data.trim().split("\\s+"));
    return computeSeedFromWords(words);
  }

  public byte[] computeSeedFromWords(List<String> words) throws AddressFormatException,
      MnemonicException.MnemonicLengthException, MnemonicException.MnemonicWordException,
      MnemonicException.MnemonicChecksumException {
    byte[] seed = mc.toEntropy(words);
    return seed;
  }

  public HD_Wallet getHD(int purpose, byte[] seed, String passphrase, NetworkParameters params) throws MnemonicException.MnemonicLengthException {
    HD_Wallet hdw = new HD_Wallet(purpose, mc, params, seed, passphrase);
    return hdw;
  }

  public BIP47Wallet getBIP47(String seed, String passphrase, NetworkParameters params) throws MnemonicException.MnemonicLengthException {
    BIP47Wallet hdw47 = new BIP47Wallet(47, mc, params, org.bouncycastle.util.encoders.Hex.decode(seed), passphrase);
    return hdw47;
  }

  public HD_Wallet getBIP49(byte[] seed, String passphrase, NetworkParameters params) throws MnemonicException.MnemonicLengthException {
    return getHD(49, seed, passphrase, params);
  }

  public HD_Wallet getBIP84(byte[] seed, String passphrase, NetworkParameters params) throws MnemonicException.MnemonicLengthException {
    return getHD(84, seed, passphrase, params);
  }

  public HD_Wallet getBIP44(byte[] seed, String passphrase, NetworkParameters params) throws MnemonicException.MnemonicLengthException {
    return getHD(44, seed, passphrase, params);
  }
}
