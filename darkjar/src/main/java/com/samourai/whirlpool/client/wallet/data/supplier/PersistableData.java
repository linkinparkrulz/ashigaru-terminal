package com.samourai.whirlpool.client.wallet.data.supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PersistableData {
  private static final Logger log = LoggerFactory.getLogger(PersistableData.class);

  private long lastChange;

  protected PersistableData() {
    this.lastChange = 0;
  }

  protected void setLastChange() {
    lastChange = System.currentTimeMillis();
  }

  public long getLastChange() {
    return lastChange;
  }
}
