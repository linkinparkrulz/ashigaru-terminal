package com.samourai.wallet.api.backend;

public enum BackendServer {
  /*****************************************
  THESE SERVER ENDPOINTS ARE NO LONGER NEEDED
   *******************************************/

  MAINNET(
      "",
      ""),
  TESTNET(
      "",
      "");

  private String backendUrlClear;
  private String backendUrlOnion;

  BackendServer(String backendUrlClear, String backendUrlOnion) {
    this.backendUrlClear = backendUrlClear;
    this.backendUrlOnion = backendUrlOnion;
  }

  public String getBackendUrl(boolean onion) {
    return onion ? backendUrlOnion : backendUrlClear;
  }

  public String getBackendUrlClear() {
    return backendUrlClear;
  }

  public String getBackendUrlOnion() {
    return backendUrlOnion;
  }

  public static BackendServer get(boolean isTestnet) {
    return isTestnet ? BackendServer.TESTNET : BackendServer.MAINNET;
  }
}
