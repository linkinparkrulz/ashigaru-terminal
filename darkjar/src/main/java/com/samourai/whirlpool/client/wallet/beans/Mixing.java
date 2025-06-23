package com.samourai.whirlpool.client.wallet.beans;

import com.samourai.whirlpool.client.WhirlpoolClient;

public class Mixing {
  private WhirlpoolUtxo utxo;
  private WhirlpoolClient whirlpoolClient;
  private long since;

  public Mixing(WhirlpoolUtxo utxo, WhirlpoolClient whirlpoolClient) {
    this.utxo = utxo;
    this.whirlpoolClient = whirlpoolClient;
    this.since = System.currentTimeMillis();
  }

  public WhirlpoolUtxo getUtxo() {
    return utxo;
  }

  public WhirlpoolClient getWhirlpoolClient() {
    return whirlpoolClient;
  }

  public long getSince() {
    return since;
  }

  @Override
  public String toString() {
    return "utxo=[" + utxo + "]";
  }
}
