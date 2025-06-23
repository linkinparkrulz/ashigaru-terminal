package com.samourai.whirlpool.client.wallet.data.dataPersister;

import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;

public interface DataPersister {

  void open() throws Exception;

  void close() throws Exception;

  void load() throws Exception;

  void persist(boolean force) throws Exception;

  WalletStateSupplier getWalletStateSupplier();

  UtxoConfigSupplier getUtxoConfigSupplier();
}
