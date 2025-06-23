package com.samourai.whirlpool.client.mix;

import com.samourai.whirlpool.client.mix.handler.IPostmixHandler;
import com.samourai.whirlpool.client.mix.handler.IPremixHandler;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;

public class MixParams {
  private String poolId;
  private long denomination;
  private WhirlpoolUtxo whirlpoolUtxo;
  private IPremixHandler premixHandler;
  private IPostmixHandler postmixHandler;

  public MixParams(
      String poolId,
      long denomination,
      WhirlpoolUtxo whirlpoolUtxo,
      IPremixHandler premixHandler,
      IPostmixHandler postmixHandler) {
    this.poolId = poolId;
    this.denomination = denomination;
    this.whirlpoolUtxo = whirlpoolUtxo;
    this.premixHandler = premixHandler;
    this.postmixHandler = postmixHandler;
  }

  public MixParams(
      Pool pool,
      WhirlpoolUtxo whirlpoolUtxo,
      IPremixHandler premixHandler,
      IPostmixHandler postmixHandler) {
    this(pool.getPoolId(), pool.getDenomination(), whirlpoolUtxo, premixHandler, postmixHandler);
  }

  public MixParams(MixParams mixParams, IPremixHandler premixHandler) {
    this(
        mixParams.getPoolId(),
        mixParams.getDenomination(),
        mixParams.getWhirlpoolUtxo(),
        premixHandler,
        mixParams.getPostmixHandler());
  }

  public String getPoolId() {
    return poolId;
  }

  public long getDenomination() {
    return denomination;
  }

  public WhirlpoolUtxo getWhirlpoolUtxo() {
    return whirlpoolUtxo;
  }

  public IPremixHandler getPremixHandler() {
    return premixHandler;
  }

  public IPostmixHandler getPostmixHandler() {
    return postmixHandler;
  }
}
