package com.samourai.whirlpool.client.wallet.data.dataSource;

import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.whirlpool.client.event.UtxosRequestEvent;
import com.samourai.whirlpool.client.event.UtxosResponseEvent;
import com.samourai.whirlpool.client.wallet.WhirlpoolEventService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.data.supplier.ExpirableSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalletResponseSupplier extends ExpirableSupplier<WalletResponse> {
  private static final Logger log = LoggerFactory.getLogger(WalletResponseSupplier.class);

  private WhirlpoolWallet whirlpoolWallet;
  private WalletResponseDataSource dataSource;

  public WalletResponseSupplier(
          WhirlpoolWallet whirlpoolWallet, WalletResponseDataSource dataSource) throws Exception {
    super(whirlpoolWallet.getConfig().getRefreshUtxoDelay(), log);
    this.whirlpoolWallet = whirlpoolWallet;
    this.dataSource = dataSource;
  }

  @Override
  protected WalletResponse fetch() throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("fetching...");
    }

    // notify
    WhirlpoolEventService.getInstance().post(new UtxosRequestEvent(whirlpoolWallet));
    WalletResponse walletResponse = dataSource.fetchWalletResponse();
    WhirlpoolEventService.getInstance().post(new UtxosResponseEvent(whirlpoolWallet));
    return walletResponse;
  }

  @Override
  protected void setValue(WalletResponse value) throws Exception {
    super.setValue(value);

    // notify
    dataSource.setValue(value);
  }
}
