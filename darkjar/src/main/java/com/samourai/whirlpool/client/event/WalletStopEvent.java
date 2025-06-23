package com.samourai.whirlpool.client.event;

import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;

public class WalletStopEvent extends WhirlpoolWalletEvent {
  public WalletStopEvent(WhirlpoolWallet whirlpoolWallet) {
    super(whirlpoolWallet);
  }
}
