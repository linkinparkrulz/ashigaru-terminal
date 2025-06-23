package com.samourai.whirlpool.client.wallet.data.pool;

import com.samourai.wallet.api.backend.beans.HttpException;
import com.samourai.whirlpool.client.event.PoolsChangeEvent;
import com.samourai.whirlpool.client.tx0.Tx0ParamService;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolEventService;
import com.samourai.whirlpool.client.wallet.data.supplier.ExpirableSupplier;
import com.samourai.whirlpool.client.whirlpool.ServerApi;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.protocol.rest.PoolsResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpirablePoolSupplier extends ExpirableSupplier<PoolData> implements PoolSupplier {
  private static final Logger log = LoggerFactory.getLogger(ExpirablePoolSupplier.class);

  private final WhirlpoolEventService eventService = WhirlpoolEventService.getInstance();
  private final ServerApi serverApi;
  private final Tx0ParamService tx0ParamService;

  public ExpirablePoolSupplier(
          int refreshPoolsDelay, ServerApi serverApi, Tx0ParamService tx0ParamService) {
    super(refreshPoolsDelay, log);
    this.serverApi = serverApi;
    this.tx0ParamService = tx0ParamService;
  }

  @Override
  protected PoolData fetch() throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("fetching...");
    }
    try {
      PoolsResponse poolsResponse = serverApi.fetchPools();
      return new PoolData(poolsResponse, tx0ParamService);
    } catch (HttpException e) {
      throw ClientUtils.wrapRestError(e);
    }
  }

  @Override
  protected void setValue(PoolData value) throws Exception {
    super.setValue(value);

    // notify
    eventService.post(new PoolsChangeEvent());
  }

  @Override
  public Collection<Pool> getPools() {
    return getValue().getPools();
  }

  @Override
  public Pool findPoolById(String poolId) {
    return getValue().findPoolById(poolId);
  }

  @Override
  public Collection<Pool> findPoolsForPremix(long utxoValue, boolean liquidity) {
    // find eligible pools
    List<Pool> poolsAccepted = new ArrayList<Pool>();
    for (Pool pool : getPools()) {
      if (pool.checkInputBalance(utxoValue, liquidity)) {
        poolsAccepted.add(pool);
      }
    }
    return poolsAccepted;
  }
}
