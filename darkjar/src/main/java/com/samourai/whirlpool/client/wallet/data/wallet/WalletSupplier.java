package com.samourai.whirlpool.client.wallet.data.wallet;

import com.samourai.wallet.client.BipWalletAndAddressType;
import com.samourai.wallet.hd.AddressType;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;

import java.util.Collection;

public interface WalletSupplier {
  Collection<BipWalletAndAddressType> getWallets();

  BipWalletAndAddressType getWallet(WhirlpoolAccount account, AddressType addressType);

  BipWalletAndAddressType getWalletByPub(String pub);

  String[] getPubs(boolean withIgnoredAccounts);

  String[] getPubs(boolean withIgnoredAccounts, AddressType... addressTypes);
}
