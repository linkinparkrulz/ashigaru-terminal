package com.samourai.whirlpool.client.wallet.data.dataPersister;

public interface PersistableSupplier {
  void load() throws Exception;

  boolean persist(boolean force) throws Exception;
}
