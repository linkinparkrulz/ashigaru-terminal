package com.samourai.whirlpool.client.event;

import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;

public class PostmixIndexFixFailEvent extends WhirlpoolWalletEvent {

  public PostmixIndexFixFailEvent(WhirlpoolWallet whirlpoolWallet) {
    super(whirlpoolWallet);
  }
}
