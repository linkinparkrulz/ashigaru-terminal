package com.samourai.whirlpool.client.wallet.beans;

import com.samourai.wallet.util.RandomUtil;
import com.samourai.whirlpool.client.mix.handler.IPostmixHandler;
import com.samourai.whirlpool.client.utils.ClientUtils;

public class ExternalDestination {
  private String xpub;
  private IPostmixHandler postmixHandler;
  private int chain;
  private int startIndex;
  private int mixs;
  private int mixsRandomFactor;

  public ExternalDestination(
      String xpub, int chain, int startIndex, int mixs, int mixsRandomFactor) {
    this.xpub = xpub;
    this.chain = chain;
    this.startIndex = startIndex;
    this.mixs = mixs;
    this.mixsRandomFactor = mixsRandomFactor;
  }

  public ExternalDestination(
      IPostmixHandler postmixHandler, int chain, int startIndex, int mixs, int mixsRandomFactor) {
    this.postmixHandler = postmixHandler;
    this.chain = chain;
    this.startIndex = startIndex;
    this.mixs = mixs;
    this.mixsRandomFactor = mixsRandomFactor;
  }

  public String getXpub() {
    return xpub;
  }

  public IPostmixHandler getPostmixHandler() {
    return postmixHandler;
  }

  public int getChain() {
    return chain;
  }

  public int getStartIndex() {
    return startIndex;
  }

  public int getMixs() {
    return mixs;
  }

  public boolean useRandomDelay() {
    // 0 => never
    if (mixsRandomFactor == 0) {
      return false;
    }
    // random
    return RandomUtil.getInstance().random(1, mixsRandomFactor) == 1;
  }

  @Override
  public String toString() {
    return "xpub="
        + ClientUtils.maskString(xpub)
        + ", chain="
        + chain
        + ", startIndex="
        + startIndex
        + ", mixs="
        + mixs
        + ", mixsRandomFactor="
        + mixsRandomFactor;
  }
}
