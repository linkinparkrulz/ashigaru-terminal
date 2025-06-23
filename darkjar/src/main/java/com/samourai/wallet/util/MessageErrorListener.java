package com.samourai.wallet.util;

public interface MessageErrorListener<S, E> extends MessageListener<S> {
  void onError(E error);
}
