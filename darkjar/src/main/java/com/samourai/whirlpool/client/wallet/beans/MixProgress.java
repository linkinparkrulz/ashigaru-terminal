package com.samourai.whirlpool.client.wallet.beans;

import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.handler.MixDestination;
import com.samourai.whirlpool.client.mix.listener.MixStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MixProgress {
  private static final Logger log = LoggerFactory.getLogger(MixProgress.class);
  private MixParams mixParams;
  private MixStep mixStep;
  private long since;

  public MixProgress(MixParams mixParams, MixStep mixStep) {
    this.mixParams = mixParams;
    this.mixStep = mixStep;
    this.since = System.currentTimeMillis();
  }

  public String getPoolId() {
    return mixParams.getPoolId();
  }

  public long getDenomination() {
    return mixParams.getDenomination();
  }

  public MixDestination getDestination() {
    return mixParams.getPostmixHandler().getDestination();
  }

  public MixStep getMixStep() {
    return mixStep;
  }

  public long getSince() {
    return since;
  }

  @Override
  public String toString() {
    return mixStep.getProgressPercent() + "%: " + mixStep;
  }
}
