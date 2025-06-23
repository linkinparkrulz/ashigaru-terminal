package com.samourai.whirlpool.client.wallet.data.dataSource;

import com.samourai.wallet.api.backend.beans.TxsResponse;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

public class MixsDoneResyncManager {
  private static final Logger log = LoggerFactory.getLogger(MixsDoneResyncManager.class);

  public MixsDoneResyncManager() {}

  public void resync(Collection<WhirlpoolUtxo> postmixUtxos, Map<String, TxsResponse.Tx> txs) {
    resync(postmixUtxos, txs, 0);
  }

  public void resync(
      Collection<WhirlpoolUtxo> postmixUtxos, Map<String, TxsResponse.Tx> txs, int adjustCounter) {
    log.info("Resynchronizing mix counters...");

    int fixedUtxos = adjustCounter;
    for (WhirlpoolUtxo whirlpoolUtxo : postmixUtxos) {
      int mixsDone = recountMixsDone(whirlpoolUtxo, txs);
      if (mixsDone != whirlpoolUtxo.getMixsDone()) {
        log.info(
            "Fixed "
                + whirlpoolUtxo.getUtxo().tx_hash
                + ":"
                + whirlpoolUtxo.getUtxo().tx_output_n
                + ": "
                + whirlpoolUtxo.getMixsDone()
                + " => "
                + mixsDone);
        whirlpoolUtxo.setMixsDone(mixsDone);
        fixedUtxos++;
      }
    }
    log.info("Resync success: " + fixedUtxos + "/" + postmixUtxos.size() + " utxos updated.");
  }

  private int recountMixsDone(WhirlpoolUtxo whirlpoolUtxo, Map<String, TxsResponse.Tx> txs) {
    int mixsDone = 0;
    String txid = whirlpoolUtxo.getUtxo().tx_hash;
    while (true) {
      TxsResponse.Tx tx = txs.get(txid);
      if (tx == null || tx.inputs == null || tx.inputs.length == 0) {
        return mixsDone;
      }
      txid = tx.inputs[0].prev_out.txid;
      mixsDone++;
    }
  }
}
