package com.samourai.whirlpool.client.wallet.data.utxoConfig;

import com.samourai.whirlpool.client.wallet.data.utxo.UtxoData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UtxoConfigUtil {
  private static final Logger log = LoggerFactory.getLogger(UtxoConfigUtil.class);

  public static void onUtxoChanges(UtxoConfigSupplier utxoConfigSupplier, UtxoData utxoData) {
    // cleanup utxoConfigs
    if (!utxoData.getUtxos().isEmpty() && utxoData.getUtxoChanges().getUtxosRemoved().size() > 0) {
      utxoConfigSupplier.clean(utxoData.getUtxos().values());
    }
  }
}
