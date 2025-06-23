package com.samourai.whirlpool.client.event;

import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.protocol.beans.Utxo;

public class MixSuccessEvent extends AbstractMixEvent {
  private Utxo receiveUtxo;

  public MixSuccessEvent(WhirlpoolWallet whirlpoolWallet, MixParams mixParams, Utxo receiveUtxo) {
    super(whirlpoolWallet, mixParams);
    this.receiveUtxo = receiveUtxo;
  }

  public Utxo getReceiveUtxo() {
    return receiveUtxo;
  }
}
