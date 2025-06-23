package com.samourai.soroban.client.dialog;

import com.samourai.soroban.client.SorobanMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSorobanMessage implements SorobanMessage {
  private static final Logger log = LoggerFactory.getLogger(AbstractSorobanMessage.class);

  private boolean done;

  public AbstractSorobanMessage(boolean done) {
    this.done = done;
  }

  @Override
  public boolean isDone() {
    return done;
  }
}
