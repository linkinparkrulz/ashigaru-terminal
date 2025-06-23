package com.samourai.whirlpool.protocol.rest;

import com.samourai.whirlpool.protocol.websocket.notifications.MixStatus;

public class PoolInfo {
  public String poolId;
  public long denomination; // in satoshis
  public long feeValue;
  public long mustMixBalanceMin;
  public long mustMixBalanceCap;
  public long mustMixBalanceMax;
  public int minAnonymitySet;
  public int minMustMix;
  public int tx0MaxOutputs;

  // mix info

  public int nbRegistered;
  public int mixAnonymitySet;
  public MixStatus mixStatus;
  public long elapsedTime;
  public int nbConfirmed;

  public PoolInfo() {}

  public PoolInfo(
      String poolId,
      long denomination,
      long feeValue,
      long mustMixBalanceMin,
      long mustMixBalanceCap,
      long mustMixBalanceMax,
      int minAnonymitySet,
      int minMustMix,
      int tx0MaxOutputs,
      int nbRegistered,
      int mixAnonymitySet,
      MixStatus mixStatus,
      long elapsedTime,
      int nbConfirmed) {
    this.poolId = poolId;
    this.denomination = denomination;
    this.feeValue = feeValue;
    this.mustMixBalanceMin = mustMixBalanceMin;
    this.mustMixBalanceCap = mustMixBalanceCap;
    this.mustMixBalanceMax = mustMixBalanceMax;
    this.minAnonymitySet = minAnonymitySet;
    this.minMustMix = minMustMix;
    this.tx0MaxOutputs = tx0MaxOutputs;

    this.nbRegistered = nbRegistered;
    this.mixAnonymitySet = mixAnonymitySet;
    this.mixStatus = mixStatus;
    this.elapsedTime = elapsedTime;
    this.nbConfirmed = nbConfirmed;
  }
}
