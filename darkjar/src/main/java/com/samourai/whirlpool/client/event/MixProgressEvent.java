package com.samourai.whirlpool.client.event;

import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;

public class MixProgressEvent extends AbstractMixEvent {
  public MixProgressEvent(WhirlpoolWallet whirlpoolWallet, MixParams mixParams) {
    super(whirlpoolWallet, mixParams);
  }
}
