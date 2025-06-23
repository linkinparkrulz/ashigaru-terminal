package com.samourai.whirlpool.client.event;

import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.MixProgress;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;

public abstract class AbstractMixEvent extends WhirlpoolWalletEvent {
  private WhirlpoolUtxo whirlpoolUtxo;
  private MixProgress mixProgress;

  public AbstractMixEvent(WhirlpoolWallet whirlpoolWallet, MixParams mixParams) {
    super(whirlpoolWallet);
    this.whirlpoolUtxo = mixParams.getWhirlpoolUtxo();
    // keep reference to MixProgress which may change later
    this.mixProgress = whirlpoolUtxo.getUtxoState().getMixProgress();
  }

  public WhirlpoolUtxo getWhirlpoolUtxo() {
    return whirlpoolUtxo;
  }

  public MixProgress getMixProgress() {
    return mixProgress;
  }
}
