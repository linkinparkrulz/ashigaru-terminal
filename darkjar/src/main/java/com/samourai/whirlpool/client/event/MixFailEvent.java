package com.samourai.whirlpool.client.event;

import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.listener.MixFailReason;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;

public class MixFailEvent extends AbstractMixEvent {
  private MixFailReason mixFailReason;
  private String error; // may be null

  public MixFailEvent(
      WhirlpoolWallet whirlpoolWallet,
      MixParams mixParams,
      MixFailReason mixFailReason,
      String error) {
    super(whirlpoolWallet, mixParams);
    this.mixFailReason = mixFailReason;
    this.error = error;
  }

  public MixFailReason getMixFailReason() {
    return mixFailReason;
  }

  public String getError() {
    return error;
  }
}
