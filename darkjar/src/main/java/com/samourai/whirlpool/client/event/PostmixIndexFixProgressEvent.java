package com.samourai.whirlpool.client.event;

import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;

public class PostmixIndexFixProgressEvent extends WhirlpoolWalletEvent {

  public PostmixIndexFixProgressEvent(WhirlpoolWallet whirlpoolWallet) {
    super(whirlpoolWallet);
  }
}
