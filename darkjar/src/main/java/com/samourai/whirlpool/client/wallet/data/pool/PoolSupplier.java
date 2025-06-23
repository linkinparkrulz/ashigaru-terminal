package com.samourai.whirlpool.client.wallet.data.pool;

import com.samourai.whirlpool.client.whirlpool.beans.Pool;

import java.util.Collection;

public interface PoolSupplier {
  Collection<Pool> getPools();

  Pool findPoolById(String poolId);

  Collection<Pool> findPoolsForPremix(long utxoValue, boolean liquidity);
}
