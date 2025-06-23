package com.samourai.wallet.client;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.hd.*;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BipWallet {
  private static final Logger log = LoggerFactory.getLogger(BipWallet.class);

  private WhirlpoolAccount account;
  private HD_Wallet bipWallet;
  private IIndexHandler indexHandler;
  private IIndexHandler indexChangeHandler;

  public BipWallet(
      HD_Wallet bip44w,
      WhirlpoolAccount account,
      IIndexHandler indexHandler,
      IIndexHandler indexChangeHandler,
      AddressType addressType) {
    this.account = account;
    this.bipWallet = new HD_Wallet(addressType.getPurpose(), bip44w);
    this.indexHandler = indexHandler;
    this.indexChangeHandler = indexChangeHandler;
  }

  public String getPub(AddressType addressType) {
    HD_Account hdAccount = bipWallet.getAccount(account.getAccountIndex());
    switch (addressType) {
      case LEGACY:
        return hdAccount.xpubstr();
      case SEGWIT_COMPAT:
        return hdAccount.ypubstr();
      case SEGWIT_NATIVE:
        return hdAccount.zpubstr();
    }
    log.error("Unknown addressType: " + addressType);
    return null;
  }

  public HD_Address getNextAddress() {
    return getNextAddress(true);
  }

  public HD_Address getNextAddress(boolean increment) {
    int nextAddressIndex = increment ? indexHandler.getAndIncrement() : indexHandler.get();
    return getAddressAt(Chain.RECEIVE.getIndex(), nextAddressIndex);
  }

  public HD_Address getNextChangeAddress() {
    return getNextChangeAddress(true);
  }

  public HD_Address getNextChangeAddress(boolean increment) {
    int nextAddressIndex =
        increment ? indexChangeHandler.getAndIncrement() : indexChangeHandler.get();
    return getAddressAt(Chain.CHANGE.getIndex(), nextAddressIndex);
  }

  public HD_Address getAddressAt(int chainIndex, int addressIndex) {
    return bipWallet.getAddressAt(account.getAccountIndex(), chainIndex, addressIndex);
  }

  public HD_Address getAddressAt(UnspentOutput utxo) {
    return getAddressAt(utxo.computePathChainIndex(), utxo.computePathAddressIndex());
  }

  public WhirlpoolAccount getAccount() {
    return account;
  }

  public IIndexHandler getIndexHandler() {
    return indexHandler;
  }

  public IIndexHandler getIndexChangeHandler() {
    return indexChangeHandler;
  }
}
