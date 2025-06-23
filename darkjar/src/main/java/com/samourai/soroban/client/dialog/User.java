package com.samourai.soroban.client.dialog;

import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.segwit.SegwitAddress;
import java.security.Provider;
import org.bitcoinj.core.NetworkParameters;

public class User {
  private BIP47UtilGeneric bip47Util;
  private BIP47Wallet bip47Wallet;
  private int bip47Account;
  private NetworkParameters params;
  private Provider provider;

  public User(
      BIP47UtilGeneric bip47Util,
      BIP47Wallet bip47Wallet,
      int bip47Account,
      NetworkParameters params,
      Provider provider) {
    this.bip47Util = bip47Util;
    this.bip47Wallet = bip47Wallet;
    this.bip47Account = bip47Account;
    this.params = params;
    this.provider = provider;
  }

  public SegwitAddress getMeeetingAddressReceive(
      PaymentCode paymentCodeCounterparty, NetworkParameters params) throws Exception {
    SegwitAddress receiveAddress =
        bip47Util
            .getReceiveAddress(bip47Wallet, bip47Account, paymentCodeCounterparty, 0, params)
            .getSegwitAddressReceive();
    return receiveAddress;
  }

  public SegwitAddress getMeeetingAddressSend(
      PaymentCode paymentCodeInitiator, NetworkParameters params) throws Exception {
    SegwitAddress sendAddress =
        bip47Util
            .getSendAddress(bip47Wallet, bip47Account, paymentCodeInitiator, 0, params)
            .getSegwitAddressSend();
    return sendAddress;
  }

  public Encrypter getEncrypter(PaymentCode paymentCodePartner) {
    return new PaynymEncrypter(bip47Wallet, bip47Account, paymentCodePartner, bip47Util, params, provider);
  }

  public PaymentCode getPaymentCode() {
    return bip47Util.getPaymentCode(bip47Wallet, bip47Account);
  }
}
