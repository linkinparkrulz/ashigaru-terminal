package com.samourai.whirlpool.client.tx0;

import org.bitcoinj.core.NetworkParameters;

public interface ITx0ParamServiceConfig {
  NetworkParameters getNetworkParameters();

  Long getOverspend(String poolId);
}
