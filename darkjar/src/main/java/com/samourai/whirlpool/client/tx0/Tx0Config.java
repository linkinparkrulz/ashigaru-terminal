package com.samourai.whirlpool.client.tx0;

import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;

public class Tx0Config {
  private Tx0ParamService tx0ParamService;
  private PoolSupplier poolSupplier;

  private Tx0FeeTarget tx0FeeTarget;
  private Tx0FeeTarget mixFeeTarget;
  private WhirlpoolAccount changeWallet;

  public Tx0Config(
      Tx0ParamService tx0ParamService,
      PoolSupplier poolSupplier,
      Tx0FeeTarget tx0FeeTarget,
      Tx0FeeTarget mixFeeTarget,
      WhirlpoolAccount changeWallet) {
    this.tx0ParamService = tx0ParamService;
    this.poolSupplier = poolSupplier;
    this.tx0FeeTarget = tx0FeeTarget;
    this.mixFeeTarget = mixFeeTarget;
    this.changeWallet = changeWallet;
  }

  public Tx0ParamService getTx0ParamService() {
    return tx0ParamService;
  }

  public PoolSupplier getPoolSupplier() {
    return poolSupplier;
  }

  public Tx0FeeTarget getTx0FeeTarget() {
    return tx0FeeTarget;
  }

  public void setTx0FeeTarget(Tx0FeeTarget tx0FeeTarget) {
    this.tx0FeeTarget = tx0FeeTarget;
  }

  public Tx0FeeTarget getMixFeeTarget() {
    return mixFeeTarget;
  }

  public void setMixFeeTarget(Tx0FeeTarget mixFeeTarget) {
    this.mixFeeTarget = mixFeeTarget;
  }

  public WhirlpoolAccount getChangeWallet() {
    return changeWallet;
  }

  public Tx0Config setChangeWallet(WhirlpoolAccount changeWallet) {
    this.changeWallet = changeWallet;
    return this;
  }
}
