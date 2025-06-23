package com.samourai.whirlpool.client.event;

import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolEvent;

public abstract class WhirlpoolWalletEvent extends WhirlpoolEvent {
  private WhirlpoolWallet whirlpoolWallet;

  public WhirlpoolWalletEvent(WhirlpoolWallet whirlpoolWallet) {
    super();
    this.whirlpoolWallet = whirlpoolWallet;
  }

  public WhirlpoolWallet getWhirlpoolWallet() {
    return whirlpoolWallet;
  }
}
