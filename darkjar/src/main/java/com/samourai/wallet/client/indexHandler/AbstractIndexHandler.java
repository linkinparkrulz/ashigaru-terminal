package com.samourai.wallet.client.indexHandler;

import com.google.common.primitives.Ints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public abstract class AbstractIndexHandler implements IIndexHandler {
  private Logger log = LoggerFactory.getLogger(AbstractIndexHandler.class);

  private Set<Integer> unconfirmedIndexs;

  public AbstractIndexHandler() {
    unconfirmedIndexs = new HashSet<Integer>();
  }

  protected synchronized int getUnconfirmed() {
    int current = get();

    if (unconfirmedIndexs.isEmpty()) {
      return current;
    }
    int currentUnconfirmed = Collections.max(unconfirmedIndexs) + 1;
    return Ints.max(current, currentUnconfirmed);
  }

  @Override
  public synchronized int getAndIncrementUnconfirmed() {
    int nextUnconfirmed = getUnconfirmed();
    unconfirmedIndexs.add(nextUnconfirmed);
    return nextUnconfirmed;
  }

  @Override
  public synchronized void confirmUnconfirmed(final int confirmed) {
    if (confirmed >= get()) {
      set(confirmed + 1, false);
    }

    Iterator<Integer> it = unconfirmedIndexs.iterator();
    while (it.hasNext()) {
      int unconfirmedIndex = it.next();
      if (unconfirmedIndex <= confirmed) {
        it.remove();
      }
    }
    if (log.isTraceEnabled()) {
      log.trace(
          "confirmUnconfirmed("
              + confirmed
              + ") => get()="
              + get()
              + ", unconfirmedIndexs="
              + unconfirmedIndexs);
    }
  }

  @Override
  public synchronized void cancelUnconfirmed(int unconfirmed) {
    unconfirmedIndexs.remove(unconfirmed);
  }

  @Override
  public void set(int value, boolean allowDecrement) {
    if (!allowDecrement && value <= get()) {
      return; // deny decrement
    }
    set(value);
  }

  protected abstract void set(int value);
}
