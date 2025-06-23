package com.samourai.whirlpool.client.wallet.data.walletState;

import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.hd.AddressType;
import com.samourai.wallet.hd.Chain;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.data.dataPersister.PersistableSupplier;

public interface WalletStateSupplier extends PersistableSupplier {
  IIndexHandler getIndexHandlerExternal();

  IIndexHandler getIndexHandlerWallet(
          WhirlpoolAccount account, AddressType addressType, Chain chain);

  boolean isInitialized();

  void setInitialized(boolean initialized);
}
