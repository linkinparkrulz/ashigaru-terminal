package com.samourai.whirlpool.client.wallet.data.chain;

import com.samourai.wallet.api.backend.beans.WalletResponse;

public interface ChainSupplier {
  WalletResponse.InfoBlock getLatestBlock();
}
