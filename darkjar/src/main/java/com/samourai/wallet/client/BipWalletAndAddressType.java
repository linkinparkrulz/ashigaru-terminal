package com.samourai.wallet.client;

import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.hd.AddressType;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BipWalletAndAddressType extends BipWallet {
  private static final Logger log = LoggerFactory.getLogger(BipWalletAndAddressType.class);

  private AddressType addressType;

  public BipWalletAndAddressType(
      HD_Wallet bip44w,
      WhirlpoolAccount account,
      IIndexHandler indexHandler,
      IIndexHandler indexChangeHandler,
      AddressType addressType) {
    super(bip44w, account, indexHandler, indexChangeHandler, addressType);
    this.addressType = addressType;
  }

  public AddressType getAddressType() {
    return addressType;
  }

  public String getPub() {
    return getPub(addressType);
  }
}
