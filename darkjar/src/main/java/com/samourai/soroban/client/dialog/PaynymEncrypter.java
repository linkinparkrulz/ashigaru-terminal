package com.samourai.soroban.client.dialog;

import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.crypto.CryptoUtil;
import com.samourai.wallet.crypto.impl.ECDHKeySet;
import java.security.Provider;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaynymEncrypter implements Encrypter {
  private static final Logger log = LoggerFactory.getLogger(PaynymEncrypter.class);

  private CryptoUtil cryptoUtil;
  private ECKey myKey;
  private ECKey partnerKey;

  public PaynymEncrypter(
      BIP47Wallet bip47Wallet,
      int bip47Account,
      PaymentCode paymentCodePartner,
      BIP47UtilGeneric bip47Util,
      NetworkParameters params,
      Provider provider) {
    this.myKey = bip47Util.getNotificationAddress(bip47Wallet, bip47Account).getECKey();
    this.partnerKey = paymentCodePartner.notificationAddress(params).getECKey();
    this.cryptoUtil = CryptoUtil.getInstance(provider);
  }

  @Override
  public String decrypt(byte[] encrypted) throws Exception {
    ECDHKeySet sharedSecret = cryptoUtil.getSharedSecret(myKey, partnerKey);
    return cryptoUtil.decryptString(encrypted, sharedSecret);
  }

  @Override
  public byte[] encrypt(String payload) throws Exception {
    ECDHKeySet sharedSecret = cryptoUtil.getSharedSecret(myKey, partnerKey);
    return cryptoUtil.encrypt(payload.getBytes(), sharedSecret);
  }
}
