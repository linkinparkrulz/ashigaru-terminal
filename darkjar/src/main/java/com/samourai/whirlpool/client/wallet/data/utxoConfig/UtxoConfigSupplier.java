package com.samourai.whirlpool.client.wallet.data.utxoConfig;

import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.data.dataPersister.PersistableSupplier;

import java.util.Collection;

public interface UtxoConfigSupplier extends PersistableSupplier {
  UtxoConfig getUtxo(String utxoHash, int utxoIndex);

  void setUtxo(String utxoHash, int utxoIndex, int mixsDone);

  void clean(Collection<WhirlpoolUtxo> existingUtxos);
}
