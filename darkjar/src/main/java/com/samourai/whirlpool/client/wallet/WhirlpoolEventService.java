package com.samourai.whirlpool.client.wallet;

import com.google.common.eventbus.EventBus;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhirlpoolEventService {
  private final Logger log = LoggerFactory.getLogger(WhirlpoolEventService.class);
  private static final WhirlpoolEventService instance = new WhirlpoolEventService();

  public static WhirlpoolEventService getInstance() {
    return instance;
  }

  private EventBus eventBus;

  public WhirlpoolEventService() {
    this.eventBus = new EventBus();
  }

  public void post(WhirlpoolEvent event) {
    if (log.isDebugEnabled()) {
      log.debug(" -> " + event.getClass().getSimpleName());
    }
    eventBus.post(event);
  }

  public void register(Object listener) {
    eventBus.register(listener);
  }

  public void unregister(Object listener) {
    eventBus.unregister(listener);
  }
}
