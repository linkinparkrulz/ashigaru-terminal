package com.samourai.whirlpool.client.wallet.data.wallet;

import com.samourai.wallet.client.BipWalletAndAddressType;
import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.hd.AddressType;
import com.samourai.wallet.hd.Chain;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class WalletSupplierImpl implements WalletSupplier {
  private static final Logger log = LoggerFactory.getLogger(WalletSupplierImpl.class);

  private final Map<WhirlpoolAccount, Map<AddressType, BipWalletAndAddressType>> wallets;
  private final Map<String, BipWalletAndAddressType> walletsByPub;

  public WalletSupplierImpl(HD_Wallet bip44w, WalletStateSupplier walletStateSupplier) {
    // instantiate wallets
    this.wallets = new LinkedHashMap<WhirlpoolAccount, Map<AddressType, BipWalletAndAddressType>>();
    this.walletsByPub = new LinkedHashMap<String, BipWalletAndAddressType>();
    for (WhirlpoolAccount account : WhirlpoolAccount.values()) {

      Map<AddressType, BipWalletAndAddressType> walletsByAddressType =
          new LinkedHashMap<AddressType, BipWalletAndAddressType>();
      for (AddressType addressType : account.getAddressTypes()) {
        IIndexHandler indexHandler =
            walletStateSupplier.getIndexHandlerWallet(account, addressType, Chain.RECEIVE);
        IIndexHandler indexChangeHandler =
            walletStateSupplier.getIndexHandlerWallet(account, addressType, Chain.CHANGE);
        BipWalletAndAddressType bipWallet =
            new BipWalletAndAddressType(
                bip44w, account, indexHandler, indexChangeHandler, addressType);
        walletsByAddressType.put(addressType, bipWallet);
        String pub = bipWallet.getPub();
        walletsByPub.put(pub, bipWallet);
      }
      wallets.put(account, walletsByAddressType);
    }
  }

  @Override
  public Collection<BipWalletAndAddressType> getWallets() {
    return walletsByPub.values();
  }

  @Override
  public BipWalletAndAddressType getWallet(WhirlpoolAccount account, AddressType addressType) {
    if (!wallets.containsKey(account) || !wallets.get(account).containsKey(addressType)) {
      log.error("No wallet found for " + account + " / " + addressType);
      return null;
    }
    BipWalletAndAddressType wallet = wallets.get(account).get(addressType);
    return wallet;
  }

  @Override
  public BipWalletAndAddressType getWalletByPub(String pub) {
    BipWalletAndAddressType bipWallet = walletsByPub.get(pub);
    if (bipWallet == null) {
      log.error("No wallet found for: " + pub);
      return null;
    }
    return bipWallet;
  }

  @Override
  public String[] getPubs(boolean withIgnoredAccounts) {
    return getPubs(withIgnoredAccounts, (AddressType[]) null);
  }

  @Override
  public String[] getPubs(boolean withIgnoredAccounts, AddressType... addressTypes) {
    List<String> pubs = new LinkedList<String>();
    for (BipWalletAndAddressType bipWallet : walletsByPub.values()) {
      if (withIgnoredAccounts || bipWallet.getAccount().isActive()) {
        if (addressTypes == null || ArrayUtils.contains(addressTypes, bipWallet.getAddressType())) {
          String pub = bipWallet.getPub();
          pubs.add(pub);
        }
      }
    }
    return pubs.toArray(new String[] {});
  }
}
