package com.samourai.whirlpool.client.utils;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.bip69.BIP69InputComparatorGeneric;

public class BIP69InputComparatorUnspentOutput extends BIP69InputComparatorGeneric<UnspentOutput> {
  @Override
  protected long getIndex(UnspentOutput i) {
    return i.tx_output_n;
  }

  @Override
  protected byte[] getHash(UnspentOutput i) {
    return i.tx_hash.getBytes();
  }
}
