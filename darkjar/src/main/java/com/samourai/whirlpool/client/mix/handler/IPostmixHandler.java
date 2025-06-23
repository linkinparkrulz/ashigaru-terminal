package com.samourai.whirlpool.client.mix.handler;

public interface IPostmixHandler {

  MixDestination getDestination();

  MixDestination computeDestination() throws Exception;

  void onRegisterOutput();

  void onMixFail();
}
