package com.samourai.whirlpool.client.tx0;

import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.beans.Tx0Data;

public class Tx0Preview {
  private Pool pool;
  private Tx0Data tx0Data;
  private long tx0MinerFee;
  private long mixMinerFee;
  private long premixMinerFee;
  private int tx0MinerFeePrice;
  private int mixMinerFeePrice;
  private long feeValue;
  private long feeChange;
  private int feeDiscountPercent;
  private long premixValue;
  private long changeValue;
  private int nbPremix;

  public Tx0Preview(Tx0Preview tx0Preview) {
    this(
        tx0Preview.pool,
        tx0Preview.tx0Data,
        tx0Preview.tx0MinerFee,
        tx0Preview.mixMinerFee,
        tx0Preview.premixMinerFee,
        tx0Preview.tx0MinerFeePrice,
        tx0Preview.mixMinerFeePrice,
        tx0Preview.premixValue,
        tx0Preview.changeValue,
        tx0Preview.nbPremix);
  }

  public Tx0Preview(
      Pool pool,
      Tx0Data tx0Data,
      long tx0MinerFee,
      long mixMinerFee,
      long premixMinerFee,
      int tx0MinerFeePrice,
      int mixMinerFeePrice,
      long premixValue,
      long changeValue,
      int nbPremix) {
    this.pool = pool;
    this.tx0Data = tx0Data;
    this.tx0MinerFee = tx0MinerFee;
    this.mixMinerFee = mixMinerFee;
    this.premixMinerFee = premixMinerFee;
    this.tx0MinerFeePrice = tx0MinerFeePrice;
    this.mixMinerFeePrice = mixMinerFeePrice;
    this.feeValue = tx0Data.getFeeValue();
    this.feeChange = tx0Data.getFeeChange();
    this.feeDiscountPercent = tx0Data.getFeeDiscountPercent();
    this.premixValue = premixValue;
    this.changeValue = changeValue;
    this.nbPremix = nbPremix;
  }

  public long computeFeeValueOrFeeChange() {
    return tx0Data.computeFeeValueOrFeeChange();
  }

  public Pool getPool() {
    return pool;
  }

  public void setPool(Pool pool) {
    this.pool = pool;
  }

  // used by Sparrow
  public Tx0Data getTx0Data() {
    return tx0Data;
  }

  public long getTx0MinerFee() {
    return tx0MinerFee;
  }

  public long getMixMinerFee() {
    return mixMinerFee;
  }

  public long getPremixMinerFee() {
    return premixMinerFee;
  }

  public int getTx0MinerFeePrice() {
    return tx0MinerFeePrice;
  }

  public int getMixMinerFeePrice() {
    return mixMinerFeePrice;
  }

  public long getFeeValue() {
    return feeValue;
  }

  public long getFeeChange() {
    return feeChange;
  }

  public int getFeeDiscountPercent() {
    return feeDiscountPercent;
  }

  public long getPremixValue() {
    return premixValue;
  }

  public long getChangeValue() {
    return changeValue;
  }

  public int getNbPremix() {
    return nbPremix;
  }

  @Override
  public String toString() {
    return "poolId="
        + pool.getPoolId()
        + ", tx0MinerFee="
        + tx0MinerFee
        + ", mixMinerFee="
        + mixMinerFee
        + ", premixMinerFee="
        + premixMinerFee
        + ", feeValue="
        + feeValue
        + ", feeChange="
        + feeChange
        + ", feeDiscountPercent="
        + feeDiscountPercent
        + ", premixValue="
        + premixValue
        + ", changeValue="
        + changeValue
        + ", nbPremix="
        + nbPremix;
  }
}
