package com.samourai.whirlpool.client.wallet.data.utxoConfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// ignore mixsTarget & poolId for backward-compatibility
@JsonIgnoreProperties(ignoreUnknown = true)
public class UtxoConfigPersisted implements UtxoConfig {
  private int mixsDone;
  private Long expired;

  public UtxoConfigPersisted() {
    this(0, null);
  }

  public UtxoConfigPersisted(int mixsDone, Long expired) {
    this.mixsDone = mixsDone;
    this.expired = expired;
  }

  public UtxoConfigPersisted copy() {
    UtxoConfigPersisted copy = new UtxoConfigPersisted(this.mixsDone, this.expired);
    return copy;
  }

  public int getMixsDone() {
    return mixsDone;
  }

  public void setMixsDone(int mixsDone) {
    this.mixsDone = mixsDone;
  }

  public Long getExpired() {
    return expired;
  }

  public void setExpired(Long expired) {
    this.expired = expired;
  }

  @Override
  public String toString() {
    return "mixsDone=" + mixsDone + ", expired=" + (expired != null ? expired : "null");
  }
}
