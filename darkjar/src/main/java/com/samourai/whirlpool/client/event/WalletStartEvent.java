package com.samourai.whirlpool.client.event;

import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoData;

public class WalletStartEvent extends WhirlpoolWalletEvent {
  private UtxoData utxoData;

  public WalletStartEvent(WhirlpoolWallet whirlpoolWallet, UtxoData utxoData) {
    super(whirlpoolWallet);
    this.utxoData = utxoData;
  }

  public UtxoData getUtxoData() {
    return utxoData;
  }
}
