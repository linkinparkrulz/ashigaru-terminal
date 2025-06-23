package com.samourai.whirlpool.client.wallet.data.chain;

import com.samourai.wallet.api.backend.beans.WalletResponse;

public class ChainData {
  private WalletResponse.InfoBlock latestBlock;

  public ChainData(WalletResponse.InfoBlock latestBlock) {
    this.latestBlock = latestBlock;
  }

  public WalletResponse.InfoBlock getLatestBlock() {
    return latestBlock;
  }
}
