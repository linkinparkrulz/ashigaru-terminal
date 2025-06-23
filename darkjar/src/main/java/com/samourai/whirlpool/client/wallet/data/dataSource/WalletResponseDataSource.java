package com.samourai.whirlpool.client.wallet.data.dataSource;

import com.samourai.wallet.api.backend.MinerFee;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.client.BipWalletAndAddressType;
import com.samourai.wallet.hd.Chain;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.util.AbstractOrchestrator;
import com.samourai.whirlpool.client.tx0.Tx0ParamService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.data.chain.BasicChainSupplier;
import com.samourai.whirlpool.client.wallet.data.chain.ChainData;
import com.samourai.whirlpool.client.wallet.data.chain.ChainSupplier;
import com.samourai.whirlpool.client.wallet.data.dataPersister.DataPersister;
import com.samourai.whirlpool.client.wallet.data.minerFee.BasicMinerFeeSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.ExpirablePoolSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.BasicUtxoSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoData;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplier;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.wallet.WalletSupplier;
import com.samourai.whirlpool.client.wallet.data.wallet.WalletSupplierImpl;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/** DataSource based on WalletResponse. */
public abstract class WalletResponseDataSource implements DataSource {
  private static final Logger log = LoggerFactory.getLogger(WalletResponseDataSource.class);

  private AbstractOrchestrator dataOrchestrator;

  private final WhirlpoolWallet whirlpoolWallet;
  private final DataPersister dataPersister;
  private final WalletResponseSupplier walletResponseSupplier;

  private final WalletSupplier walletSupplier;
  private final BasicMinerFeeSupplier minerFeeSupplier;
  protected final Tx0ParamService tx0ParamService;
  protected final ExpirablePoolSupplier poolSupplier;
  private final BasicChainSupplier chainSupplier;
  private final BasicUtxoSupplier utxoSupplier;

  public WalletResponseDataSource(
          WhirlpoolWallet whirlpoolWallet, HD_Wallet bip44w, DataPersister dataPersister)
      throws Exception {
    this.whirlpoolWallet = whirlpoolWallet;
    this.dataPersister = dataPersister;
    this.walletResponseSupplier = new WalletResponseSupplier(whirlpoolWallet, this);

    this.walletSupplier =
        computeWalletSupplier(whirlpoolWallet, bip44w, dataPersister.getWalletStateSupplier());
    this.minerFeeSupplier = computeMinerFeeSupplier(whirlpoolWallet);
    this.tx0ParamService = new Tx0ParamService(minerFeeSupplier, whirlpoolWallet.getConfig());
    this.poolSupplier = computePoolSupplier(whirlpoolWallet, tx0ParamService);
    this.chainSupplier = computeChainSupplier();
    this.utxoSupplier =
        computeUtxoSupplier(
            whirlpoolWallet,
            walletSupplier,
            dataPersister.getUtxoConfigSupplier(),
            chainSupplier,
            poolSupplier,
            tx0ParamService);
  }

  protected WalletSupplierImpl computeWalletSupplier(
          WhirlpoolWallet whirlpoolWallet, HD_Wallet bip44w, WalletStateSupplier walletStateSupplier)
      throws Exception {
    return new WalletSupplierImpl(bip44w, walletStateSupplier);
  }

  protected BasicMinerFeeSupplier computeMinerFeeSupplier(WhirlpoolWallet whirlpoolWallet)
      throws Exception {
    WhirlpoolWalletConfig config = whirlpoolWallet.getConfig();
    BasicMinerFeeSupplier minerFeeSupplier =
        new BasicMinerFeeSupplier(config.getFeeMin(), config.getFeeMax());
    minerFeeSupplier.setValue(config.getFeeFallback());
    return minerFeeSupplier;
  }

  protected ExpirablePoolSupplier computePoolSupplier(
          WhirlpoolWallet whirlpoolWallet, Tx0ParamService tx0ParamService) throws Exception {
    WhirlpoolWalletConfig config = whirlpoolWallet.getConfig();
    return new ExpirablePoolSupplier(
        config.getRefreshPoolsDelay(), config.getServerApi(), tx0ParamService);
  }

  protected BasicChainSupplier computeChainSupplier() throws Exception {
    return new BasicChainSupplier();
  }

  protected BasicUtxoSupplier computeUtxoSupplier(
      final WhirlpoolWallet whirlpoolWallet,
      WalletSupplier walletSupplier,
      UtxoConfigSupplier utxoConfigSupplier,
      ChainSupplier chainSupplier,
      PoolSupplier poolSupplier,
      Tx0ParamService tx0ParamService)
      throws Exception {
    return new BasicUtxoSupplier(
        walletSupplier,
        utxoConfigSupplier,
        chainSupplier,
        poolSupplier,
        tx0ParamService,
        whirlpoolWallet.getConfig().getNetworkParameters()) {
      @Override
      public void refresh() throws Exception {
        WalletResponseDataSource.this.refresh();
      }

      @Override
      protected void onUtxoChanges(UtxoData utxoData) {
        super.onUtxoChanges(utxoData);
        whirlpoolWallet.onUtxoChanges(utxoData);
      }
    };
  }

  public void refresh() throws Exception {
    walletResponseSupplier.refresh();
  }

  protected abstract WalletResponse fetchWalletResponse() throws Exception;

  protected void setValue(WalletResponse walletResponse) throws Exception {
    // update minerFeeSupplier
    try {
      if (walletResponse == null
          || walletResponse.info == null
          || walletResponse.info.fees == null) {
        throw new Exception("Invalid walletResponse.info.fees");
      }
      minerFeeSupplier.setValue(new MinerFee(walletResponse.info.fees));
    } catch (Exception e) {
      // keep previous fee value as fallback
      log.error("minerFeeSupplier.setValue failed", e);
    }

    // update chainSupplier (before utxoSupplier)
    if (walletResponse == null
        || walletResponse.info == null
        || walletResponse.info.latest_block == null) {
      throw new Exception("Invalid walletResponse.info.latest_block");
    }
    chainSupplier.setValue(new ChainData(walletResponse.info.latest_block));

    // update utxoSupplier
    if (walletResponse == null
        || walletResponse.unspent_outputs == null
        || walletResponse.txs == null) {
      throw new Exception("Invalid walletResponse.unspent_outputs/txs");
    }
    UtxoData utxoData = new UtxoData(walletResponse.unspent_outputs, walletResponse.txs);
    utxoSupplier.setValue(utxoData);

    // update walletStateSupplier
    setWalletStateValue(walletResponse.getAddressesMap());
  }

  private void setWalletStateValue(Map<String, WalletResponse.Address> addressesMap) {
    // update indexs from wallet backend
    WalletStateSupplier walletStateSupplier = dataPersister.getWalletStateSupplier();
    for (String pub : addressesMap.keySet()) {
      WalletResponse.Address address = addressesMap.get(pub);
      BipWalletAndAddressType bipWallet = walletSupplier.getWalletByPub(pub);
      if (bipWallet != null) {
        walletStateSupplier
            .getIndexHandlerWallet(
                bipWallet.getAccount(), bipWallet.getAddressType(), Chain.RECEIVE)
            .set(address.account_index, false);
        walletStateSupplier
            .getIndexHandlerWallet(bipWallet.getAccount(), bipWallet.getAddressType(), Chain.CHANGE)
            .set(address.change_index, false);
      } else {
        log.error("No wallet found for: " + pub);
      }
    }
  }

  protected void load(boolean initial) throws Exception {
    // load pools
    poolSupplier.load();

    // load data
    walletResponseSupplier.load();
  }

  @Override
  public void open() throws Exception {
    // load initial data (or fail)
    load(true);

    // data orchestrator
    runDataOrchestrator();
  }

  protected void runDataOrchestrator() {
    WhirlpoolWalletConfig config = whirlpoolWallet.getConfig();
    int dataOrchestratorDelay =
        NumberUtils.min(config.getRefreshUtxoDelay(), config.getRefreshPoolsDelay());
    dataOrchestrator =
        new AbstractOrchestrator(dataOrchestratorDelay * 1000) {
          @Override
          protected void runOrchestrator() {
            if (log.isDebugEnabled()) {
              log.debug("Refreshing data...");
            }
            try {
              load(false);
            } catch (Exception e) {
              log.error("", e);
            }
          }
        };
    dataOrchestrator.start(true);
  }

  @Override
  public void close() throws Exception {
    dataOrchestrator.stop();
  }

  protected WhirlpoolWallet getWhirlpoolWallet() {
    return whirlpoolWallet;
  }

  protected DataPersister getDataPersister() {
    return dataPersister;
  }

  @Override
  public WalletSupplier getWalletSupplier() {
    return walletSupplier;
  }

  @Override
  public MinerFeeSupplier getMinerFeeSupplier() {
    return minerFeeSupplier;
  }

  @Override
  public Tx0ParamService getTx0ParamService() {
    return tx0ParamService;
  }

  @Override
  public PoolSupplier getPoolSupplier() {
    return poolSupplier;
  }

  @Override
  public ChainSupplier getChainSupplier() {
    return chainSupplier;
  }

  @Override
  public UtxoSupplier getUtxoSupplier() {
    return utxoSupplier;
  }

  protected WalletResponseSupplier getWalletResponseSupplier() {
    return walletResponseSupplier;
  }
}
