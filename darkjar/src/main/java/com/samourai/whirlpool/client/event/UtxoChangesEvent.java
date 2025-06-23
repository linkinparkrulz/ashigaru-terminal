package com.samourai.whirlpool.client.event;

import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoData;

public class UtxoChangesEvent extends WhirlpoolWalletEvent {
  private UtxoData utxoData;

  public UtxoChangesEvent(WhirlpoolWallet whirlpoolWallet, UtxoData utxoData) {
    super(whirlpoolWallet);
    this.utxoData = utxoData;
  }

  public UtxoData getUtxoData() {
    return utxoData;
  }
}
