package com.samourai.whirlpool.client.event;

import com.samourai.whirlpool.client.tx0.Tx0;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;

public class Tx0Event extends WhirlpoolWalletEvent {
  private Tx0 tx0;

  public Tx0Event(WhirlpoolWallet whirlpoolWallet, Tx0 tx0) {
    super(whirlpoolWallet);
    this.tx0 = tx0;
  }

  public Tx0 getTx0() {
    return tx0;
  }
}
