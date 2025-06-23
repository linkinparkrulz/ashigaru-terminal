package com.samourai.whirlpool.client.mix.handler;

import com.samourai.wallet.client.indexHandler.IIndexHandler;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPostmixHandler implements IPostmixHandler {
  private static final Logger log = LoggerFactory.getLogger(AbstractPostmixHandler.class);

  protected IIndexHandler indexHandler;
  protected NetworkParameters params;

  protected MixDestination destination;

  public AbstractPostmixHandler(IIndexHandler indexHandler, NetworkParameters params) {
    this.indexHandler = indexHandler;
    this.params = params;
  }

  protected abstract MixDestination computeNextDestination() throws Exception;

  @Override
  public MixDestination getDestination() {
    return destination; // may be NULL
  }

  public final MixDestination computeDestination() throws Exception {
    // use "unconfirmed" index to avoid huge index gaps on multiple mix failures
    this.destination = computeNextDestination();
    return destination;
  }

  @Override
  public void onMixFail() {
    if (destination != null) {
      indexHandler.cancelUnconfirmed(destination.getIndex());
    }
  }

  @Override
  public void onRegisterOutput() {
    // confirm receive address even when REGISTER_OUTPUT fails, to avoid 'ouput already registered'
    indexHandler.confirmUnconfirmed(destination.getIndex());
  }
}
