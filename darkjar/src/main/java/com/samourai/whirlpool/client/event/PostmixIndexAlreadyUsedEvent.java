package com.samourai.whirlpool.client.event;

import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;

public class PostmixIndexAlreadyUsedEvent extends WhirlpoolWalletEvent {

  public PostmixIndexAlreadyUsedEvent(WhirlpoolWallet whirlpoolWallet) {
    super(whirlpoolWallet);
  }
}
