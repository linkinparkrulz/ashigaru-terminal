package com.samourai.whirlpool.client.event;

import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;

public class WalletOpenEvent extends WhirlpoolWalletEvent {

  public WalletOpenEvent(WhirlpoolWallet whirlpoolWallet) {
    super(whirlpoolWallet);
  }
}
