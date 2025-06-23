package com.samourai.soroban.client;

import com.samourai.wallet.util.FormatsUtilGeneric;
import java8.util.Optional;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;

public enum SorobanServer {
  TESTNET(
          "https://soroban.samouraiwallet.com/test",
          "http://sorob4sg7yiopktgz4eom7hl5mcodr6quvhmdpljl5qqhmt6po7oebid.onion/test",
          TestNet3Params.get()),
  MAINNET(
          "https://soroban.samouraiwallet.com",
          "http://sorob4sg7yiopktgz4eom7hl5mcodr6quvhmdpljl5qqhmt6po7oebid.onion",
          MainNetParams.get());

  private String serverUrlClear;
  private String serverUrlOnion;
  private NetworkParameters params;

  SorobanServer(String serverUrlClear, String serverUrlOnion, NetworkParameters params) {
    this.serverUrlClear = serverUrlClear;
    this.serverUrlOnion = serverUrlOnion;
    this.params = params;
  }

  public String getServerUrlClear() {
    return serverUrlClear;
  }

  public String getServerUrlOnion() {
    return serverUrlOnion;
  }

  public String getServerUrl(boolean onion) {
    String serverUrl = onion ? getServerUrlOnion() : getServerUrlClear();
    return serverUrl;
  }

  public NetworkParameters getParams() {
    return params;
  }

  public static Optional<SorobanServer> find(String value) {
    try {
      return Optional.of(valueOf(value));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  public static SorobanServer get(NetworkParameters params) {
    if (FormatsUtilGeneric.getInstance().isTestNet(params)) {
      return TESTNET;
    }
    return MAINNET;
  }
}
