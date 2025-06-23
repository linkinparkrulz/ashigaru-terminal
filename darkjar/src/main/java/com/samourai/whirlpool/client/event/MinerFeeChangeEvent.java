package com.samourai.whirlpool.client.event;

import com.samourai.wallet.api.backend.MinerFee;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolEvent;

public class MinerFeeChangeEvent extends WhirlpoolEvent {
  private MinerFee minerFee;

  public MinerFeeChangeEvent(MinerFee minerFee) {
    super();
    this.minerFee = minerFee;
  }

  public MinerFee getMinerFee() {
    return minerFee;
  }
}
