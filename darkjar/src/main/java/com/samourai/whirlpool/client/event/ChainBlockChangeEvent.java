package com.samourai.whirlpool.client.event;

import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolEvent;

public class ChainBlockChangeEvent extends WhirlpoolEvent {
  private WalletResponse.InfoBlock block;

  public ChainBlockChangeEvent(WalletResponse.InfoBlock block) {
    super();
    this.block = block;
  }

  public WalletResponse.InfoBlock getBlock() {
    return block;
  }
}
