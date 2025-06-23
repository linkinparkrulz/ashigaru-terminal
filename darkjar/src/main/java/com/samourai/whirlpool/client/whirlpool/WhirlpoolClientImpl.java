package com.samourai.whirlpool.client.whirlpool;

import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.mix.MixClient;
import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.listener.MixFailReason;
import com.samourai.whirlpool.client.mix.listener.MixStep;
import com.samourai.whirlpool.client.whirlpool.listener.WhirlpoolClientListener;
import com.samourai.whirlpool.protocol.beans.Utxo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhirlpoolClientImpl implements WhirlpoolClient {
  private Logger log;

  private WhirlpoolClientConfig config;

  private boolean done;
  private String logPrefix;

  private MixClient mixClient;
  private Thread mixThread;
  private WhirlpoolClientListener listener;

  public WhirlpoolClientImpl(WhirlpoolClientConfig config) {
    this(config, Long.toString(System.currentTimeMillis()));
  }

  public WhirlpoolClientImpl(WhirlpoolClientConfig config, String logPrefix) {
    this.log = LoggerFactory.getLogger(WhirlpoolClientImpl.class + "[" + logPrefix + "]");
    this.config = config;
    this.logPrefix = logPrefix;
    if (log.isDebugEnabled()) {
      log.debug("+whirlpoolClient");
    }
  }

  @Override
  public void whirlpool(final MixParams mixParams, WhirlpoolClientListener listener) {
    this.listener = listener;

    this.mixThread =
        new Thread(
            new Runnable() {
              @Override
              public synchronized void run() {
                runClient(mixParams);
                while (!done) {
                  try {
                    synchronized (mixThread) {
                      mixThread.wait();
                    }
                  } catch (Exception e) {
                  }
                }
              }
            },
            "whirlpoolClient-" + logPrefix);
    this.mixThread.setDaemon(true);
    this.mixThread.start();
  }

  private void runClient(MixParams mixParams) {
    WhirlpoolClientListener mixListener = computeMixListener();

    mixClient = new MixClient(config, logPrefix);
    mixClient.whirlpool(mixParams, mixListener);
  }

  private WhirlpoolClientListener computeMixListener() {
    return new WhirlpoolClientListener() {

      @Override
      public void success(Utxo receiveUtxo) {
        // done
        listener.success(receiveUtxo);
        disconnect();
      }

      @Override
      public void fail(MixFailReason reason, String notifiableError) {
        listener.fail(reason, notifiableError);
        disconnect();
      }

      @Override
      public void progress(MixStep mixStep) {
        listener.progress(mixStep);
      }
    };
  }

  @Override
  public void stop(boolean cancel) {
    mixClient.stop(cancel);
  }

  private void disconnect() {
    if (!done) {
      if (log.isDebugEnabled()) {
        log.debug("--whirlpoolClient");
      }
      done = true;
      if (mixClient != null) {
        mixClient.disconnect();
      }
      if (mixThread != null) {
        synchronized (mixThread) {
          mixThread.notify();
        }
      }
    }
  }

  public void _setListener(WhirlpoolClientListener listener) {
    this.listener = listener;
  }

  public WhirlpoolClientListener getListener() {
    return listener;
  }
}
