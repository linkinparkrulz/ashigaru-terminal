package com.samourai.whirlpool.client.mix.handler;

import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum DestinationType {
  DEPOSIT,
  PREMIX,
  POSTMIX,
  BADBANK,
  XPUB;

  private static final Logger log = LoggerFactory.getLogger(DestinationType.class);

  public static DestinationType find(WhirlpoolAccount whirlpoolAccount) {
    switch (whirlpoolAccount) {
      case DEPOSIT:
        return DestinationType.DEPOSIT;
      case PREMIX:
        return DestinationType.PREMIX;
      case POSTMIX:
        return DestinationType.POSTMIX;
      case BADBANK:
        return DestinationType.BADBANK;
    }
    log.error("Unknown DestinationType for WhirlpoolAccount: " + whirlpoolAccount);
    return null;
  }
}
