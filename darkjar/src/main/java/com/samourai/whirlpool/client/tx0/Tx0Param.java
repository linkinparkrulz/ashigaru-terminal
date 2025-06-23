package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.util.FeeUtil;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class Tx0Param {
  private static final Logger log = LoggerFactory.getLogger(Tx0Param.class);
  private static final FeeUtil feeUtil = FeeUtil.getInstance();

  private NetworkParameters params;
  private int feeTx0;
  private int feePremix;
  private Pool pool;
  private Long overspendValueOrNull;

  // computed
  private Long premixValue;
  private Long spendFromBalanceMin;

  public Tx0Param(
      NetworkParameters params, int feeTx0, int feePremix, Pool pool, Long overspendValueOrNull) {
    this.params = params;
    this.feeTx0 = feeTx0;
    this.feePremix = feePremix;
    this.pool = pool;
    this.overspendValueOrNull = overspendValueOrNull;

    this.premixValue = null;
    this.spendFromBalanceMin = null;
  }

  private long computePremixValue() {
    long premixOverspend;
    if (overspendValueOrNull != null && overspendValueOrNull > 0) {
      premixOverspend = overspendValueOrNull;
    } else {
      // compute premixOverspend
      long mixFeesEstimate =
          feeUtil.estimatedFeeSegwit(
              0, 0, pool.getMixAnonymitySet(), pool.getMixAnonymitySet(), 0, feePremix);
      premixOverspend = mixFeesEstimate / pool.getMinMustMix();
      if (log.isTraceEnabled()) {
        log.trace(
            "mixFeesEstimate="
                + mixFeesEstimate
                + " => premixOverspend="
                + overspendValueOrNull
                + " for poolId="
                + pool.getPoolId());
      }
    }
    long premixValue = pool.getDenomination() + premixOverspend;

    // make sure destinationValue is acceptable for pool
    long premixBalanceMin = pool.computePremixBalanceMin(false);
    long premixBalanceCap = pool.computePremixBalanceCap(false);
    long premixBalanceMax = pool.computePremixBalanceMax(false);

    long premixValueFinal = premixValue;
    premixValueFinal = min(premixValueFinal, premixBalanceMax);
    premixValueFinal = min(premixValueFinal, premixBalanceCap);
    premixValueFinal = max(premixValueFinal, premixBalanceMin);

    if (log.isDebugEnabled()) {
      log.debug(
          "premixValueFinal="
              + premixValueFinal
              + ", premixValue="
              + premixValue
              + ", premixOverspend="
              + premixOverspend
              + " for poolId="
              + pool.getPoolId());
    }
    return premixValueFinal;
  }

  protected boolean isTx0Possible(long utxoValue) {
    long balanceMin = getSpendFromBalanceMin();
    if (log.isTraceEnabled()) {
      log.trace(
          "isTx0Possible: spendFromBalanceMin="
              + balanceMin
              + " for utxoValue="
              + utxoValue
              + ", tx0Param="
              + this);
    }
    return (utxoValue >= balanceMin);
  }

  private long computeSpendFromBalanceMin() {
    return computeSpendFromBalanceMin(
        getFeeTx0(), getPool().getFeeValue(), getPremixValue(), params);
  }

  public static long computeSpendFromBalanceMin(
      long feeTx0, long poolFee, long premixValue, NetworkParameters params) {
    int nbPremix = 1;
    long tx0MinerFee = ClientUtils.computeTx0MinerFee(nbPremix, feeTx0, null, params);
    return ClientUtils.computeTx0SpendValue(premixValue, nbPremix, poolFee, tx0MinerFee);
  }

  public int getFeeTx0() {
    return feeTx0;
  }

  public int getFeePremix() {
    return feePremix;
  }

  public Pool getPool() {
    return pool;
  }

  public long getPremixValue() {
    if (premixValue == null) {
      premixValue = computePremixValue();
    }
    return premixValue;
  }

  public long getSpendFromBalanceMin() {
    if (spendFromBalanceMin == null) {
      spendFromBalanceMin = computeSpendFromBalanceMin();
    }
    return spendFromBalanceMin;
  }

  @Override
  public String toString() {
    return super.toString() + "pool=" + pool.getPoolId();
  }
}
