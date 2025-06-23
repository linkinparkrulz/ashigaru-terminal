package com.samourai.wallet.send.provider;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.TransactionOutPoint;

import java.util.LinkedHashMap;
import java.util.Map;

public class SimpleUtxoKeyProvider implements UtxoKeyProvider {
  private Map<String, ECKey> keys = new LinkedHashMap<String, ECKey>();

  public void setKey(TransactionOutPoint outPoint, ECKey key) {
    keys.put(outPoint.toString(), key);
  }

  @Override
  public ECKey _getPrivKey(String utxoHash, int utxoIndex) throws Exception {
    return keys.get(utxoHash + ":" + utxoIndex);
  }
}
