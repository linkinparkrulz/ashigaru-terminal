package com.samourai.whirlpool.client.event;

import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;

public class PostmixIndexFixSuccessEvent extends WhirlpoolWalletEvent {

  public PostmixIndexFixSuccessEvent(WhirlpoolWallet whirlpoolWallet) {
    super(whirlpoolWallet);
  }
}
