package com.samourai.whirlpool.client.wallet.data.walletState;

import com.fasterxml.jackson.core.type.TypeReference;
import com.samourai.whirlpool.client.wallet.data.supplier.AbstractPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class WalletStatePersister extends AbstractPersister<WalletStateData, Map<String, Integer>> {
  private static final Logger log = LoggerFactory.getLogger(WalletStatePersister.class);

  public WalletStatePersister(String fileName) {
    super(fileName, new TypeReference<Map<String, Integer>>() {}, log);
  }

  @Override
  protected WalletStateData getInitialValue() {
    return new WalletStateData();
  }

  @Override
  protected WalletStateData fromPersisted(Map<String, Integer> persisted) {
    Map<String, Integer> items = new LinkedHashMap<String, Integer>();
    items.putAll(persisted);
    return new WalletStateData(items);
  }

  @Override
  protected Map<String, Integer> toPersisted(WalletStateData data) {
    Map<String, Integer> mapPersisted = new LinkedHashMap<String, Integer>();
    mapPersisted.putAll(data.getItems());
    return mapPersisted;
  }
}
