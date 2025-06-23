package com.samourai.whirlpool.client.wallet.data.minerFee;

import com.samourai.wallet.api.backend.MinerFeeTarget;

public interface MinerFeeSupplier {
  int getFee(MinerFeeTarget feeTarget);
}
