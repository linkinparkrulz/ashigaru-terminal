package com.samourai.whirlpool.client.wallet.data.supplier;

import org.slf4j.Logger;

/** Supplier with static data. */
public abstract class BasicSupplier<D> {
  protected final Logger log;
  private D value;
  private Long lastUpdate;

  public BasicSupplier(final Logger log) {
    this.log = log;
    this.value = null;
    this.lastUpdate = null;
  }

  protected void setValue(D value) throws Exception {
    if (log.isTraceEnabled()) {
      log.trace("setValue");
    }
    this.value = value;
    this.lastUpdate = System.currentTimeMillis();
  }

  public D getValue() {
    return value;
  }

  public Long getLastUpdate() {
    return lastUpdate;
  }
}
