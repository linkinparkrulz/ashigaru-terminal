package com.samourai.whirlpool.client.wallet.data.walletState;

import com.samourai.wallet.client.indexHandler.AbstractIndexHandler;
import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.hd.AddressType;
import com.samourai.wallet.hd.Chain;
import com.samourai.whirlpool.client.wallet.beans.ExternalDestination;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.data.supplier.BasicPersistableSupplier;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistableWalletStateSupplier extends BasicPersistableSupplier<WalletStateData>
        implements WalletStateSupplier {
  private static final Logger log = LoggerFactory.getLogger(PersistableWalletStateSupplier.class);
  private static final String EXTERNAL_INDEX_HANDLER = "external";

  private final IIndexHandler indexHandlerExternal;
  private Map<String, IIndexHandler> indexHandlerWallets;

  public PersistableWalletStateSupplier(
          WalletStatePersister persister, ExternalDestination externalDestination) {
    super(persister, log);

    int externalIndexDefault =
            externalDestination != null ? externalDestination.getStartIndex() : 0;
    this.indexHandlerExternal = createIndexHandler(EXTERNAL_INDEX_HANDLER, externalIndexDefault);
    this.indexHandlerWallets = new LinkedHashMap<String, IIndexHandler>();
  }

  @Override
  public IIndexHandler getIndexHandlerWallet(
          WhirlpoolAccount account, AddressType addressType, Chain chain) {
    String persistKey = computePersistKeyWallet(account, addressType, chain);
    IIndexHandler indexHandlerWallet = indexHandlerWallets.get(persistKey);
    if (indexHandlerWallet == null) {
      indexHandlerWallet = createIndexHandler(persistKey, 0);
      indexHandlerWallets.put(persistKey, indexHandlerWallet);
    }
    return indexHandlerWallet;
  }

  protected IIndexHandler createIndexHandler(final String persistKey, final int defaultValue) {
    return new AbstractIndexHandler() {
      @Override
      public int getAndIncrement() {
        return getValue().getAndIncrement(persistKey, defaultValue);
      }

      @Override
      public int get() {
        return getValue().get(persistKey, defaultValue);
      }

      @Override
      protected void set(int value) {
        getValue().set(persistKey, value);
        if (log.isDebugEnabled()) {
          log.debug("set: [" + persistKey + "]=" + value);
        }
      }
    };
  }

  protected String computePersistKeyWallet(
          WhirlpoolAccount account, AddressType addressType, Chain chain) {
    return account.name() + "_" + addressType.getPurpose() + "_" + chain.getIndex();
  }

  @Override
  public boolean isInitialized() {
    return getValue().isInitialized();
  }

  @Override
  public void setInitialized(boolean initialized) {
    getValue().setInitialized(initialized);
  }

  @Override
  public IIndexHandler getIndexHandlerExternal() {
    return indexHandlerExternal;
  }
}
