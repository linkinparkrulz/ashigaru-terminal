package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;

public class WhirlpoolUtxoWithKey extends UnspentOutput {
  private WhirlpoolUtxo whirlpoolUtxo;
  private byte[] key;

  public WhirlpoolUtxoWithKey(WhirlpoolUtxo whirlpoolUtxo, byte[] key) {
    super(whirlpoolUtxo.getUtxo());
    this.whirlpoolUtxo = whirlpoolUtxo;
    this.key = key;
  }

  public WhirlpoolUtxo getWhirlpoolUtxo() {
    return whirlpoolUtxo;
  }

  byte[] getKey() {
    return key;
  }
}
