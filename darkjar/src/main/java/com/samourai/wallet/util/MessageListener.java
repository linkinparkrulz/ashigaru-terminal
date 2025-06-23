package com.samourai.wallet.util;

public interface MessageListener<S> {
  void onMessage(S message);
}
