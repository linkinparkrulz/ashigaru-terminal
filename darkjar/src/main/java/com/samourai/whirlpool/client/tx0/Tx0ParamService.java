package com.samourai.whirlpool.client.tx0;

import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Tx0ParamService {
  private Logger log = LoggerFactory.getLogger(Tx0ParamService.class);

  private MinerFeeSupplier minerFeeSupplier;
  private ITx0ParamServiceConfig config;

  public Tx0ParamService(MinerFeeSupplier minerFeeSupplier, ITx0ParamServiceConfig config) {
    this.minerFeeSupplier = minerFeeSupplier;
    this.config = config;
  }

  public Tx0Param getTx0Param(Pool pool, Tx0FeeTarget tx0FeeTarget, Tx0FeeTarget mixFeeTarget) {
    int feeTx0 = minerFeeSupplier.getFee(tx0FeeTarget.getFeeTarget());
    int feePremix = Math.max(2, minerFeeSupplier.getFee(mixFeeTarget.getFeeTarget()));
    Long overspendOrNull = config.getOverspend(pool.getPoolId());
    Tx0Param tx0Param =
        new Tx0Param(config.getNetworkParameters(), feeTx0, feePremix, pool, overspendOrNull);
    return tx0Param;
  }

  public Collection<Pool> findPools(Collection<Pool> poolsByPreference, long utxoValue) {
    List<Pool> eligiblePools = new LinkedList<Pool>();
    for (Pool pool : poolsByPreference) {
      Tx0Param tx0Param = getTx0Param(pool, Tx0FeeTarget.MAX, Tx0FeeTarget.MAX);
      boolean eligible = tx0Param.isTx0Possible(utxoValue);
      if (eligible) {
        eligiblePools.add(pool);
      }
    }
    return eligiblePools;
  }

  public boolean isTx0Possible(
          Pool pool, Tx0FeeTarget tx0FeeTarget, Tx0FeeTarget mixFeeTarget, long utxoValue) {
    Tx0Param tx0Param = getTx0Param(pool, tx0FeeTarget, mixFeeTarget);
    return tx0Param.isTx0Possible(utxoValue);
  }

  public boolean isPoolApplicable(Pool pool, WhirlpoolUtxo whirlpoolUtxo) {
    long utxoValue = whirlpoolUtxo.getUtxo().value;
    if (whirlpoolUtxo.isAccountDeposit()) {
      return isTx0Possible(pool, Tx0FeeTarget.MAX, Tx0FeeTarget.MAX, utxoValue);
    }
    if (whirlpoolUtxo.isAccountPremix()) {
      return pool.checkInputBalance(utxoValue, false);
    }
    if (whirlpoolUtxo.isAccountPostmix()) {
      return utxoValue == pool.getDenomination();
    }
    log.error("Unknown account for whirlpoolUtxo:" + whirlpoolUtxo);
    return false;
  }

  public ITx0ParamServiceConfig getConfig() {
    return config;
  }
}
