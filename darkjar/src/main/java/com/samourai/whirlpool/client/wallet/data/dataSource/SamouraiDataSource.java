package com.samourai.whirlpool.client.wallet.data.dataSource;

import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.beans.TxsResponse;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.api.backend.websocket.BackendWsApi;
import com.samourai.wallet.hd.AddressType;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.util.MessageListener;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.MixableStatus;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.data.dataPersister.DataPersister;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;
import java8.util.function.Predicate;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** DataSource for Samourai/Dojo backend. */
public class SamouraiDataSource extends WalletResponseDataSource
    implements DataSourceWithStrictMode {
  private static final Logger log = LoggerFactory.getLogger(SamouraiDataSource.class);

  private static final int INITWALLET_RETRY = 3;
  private static final int INITWALLET_RETRY_TIMEOUT = 3000;
  private static final int FETCH_TXS_PER_PAGE = 300;

  private BackendApi backendApi;
  private BackendWsApi backendWsApi; // may be null

  public SamouraiDataSource(
      WhirlpoolWallet whirlpoolWallet,
      HD_Wallet bip44w,
      DataPersister dataPersister,
      BackendApi backendApi,
      BackendWsApi backendWsApi)
      throws Exception {
    super(whirlpoolWallet, bip44w, dataPersister);

    this.backendApi = backendApi;
    this.backendWsApi = backendWsApi;
  }

  @Override
  protected void load(boolean initial) throws Exception {
    super.load(initial);

    if (initial) {
      WalletStateSupplier walletStateSupplier = getDataPersister().getWalletStateSupplier();
      boolean isInitialized = walletStateSupplier.isInitialized();

      // initialize wallets
      if (!isInitialized) {
        String[] activePubs = getWalletSupplier().getPubs(true, AddressType.SEGWIT_NATIVE);
        for (String pub : activePubs) {
          initWallet(pub);
        }
        walletStateSupplier.setInitialized(true);

        // when wallet is not initialized, counters are not synced
        if (getWhirlpoolWallet().getConfig().isResyncOnFirstRun()) {
          // resync postmix indexs
          resyncMixsDone();
        }
      }
    }
  }

  public void resyncMixsDone() {
    // only resync if we have remixable utxos
    Collection<WhirlpoolUtxo> postmixUtxos = getUtxoSupplier().findUtxos(WhirlpoolAccount.POSTMIX);
    if (!filterRemixableUtxos(postmixUtxos).isEmpty()) {
      // there are remixable postmix utxos
      if (log.isDebugEnabled()) {
        log.debug("First run => resync mixsDone");
      }
      try {
        Map<String, TxsResponse.Tx> postmixTxs = fetchTxsPostmix();
        int adjustCounter =
            1; // first mix only appears in PREMIX txs instead of POSTMIX for SamouraiDataSource
        new MixsDoneResyncManager().resync(postmixUtxos, postmixTxs, adjustCounter);
      } catch (Exception e) {
        log.error("", e);
      }
    }
  }

  private Map<String, TxsResponse.Tx> fetchTxsPostmix() throws Exception {
    String[] zpubs =
        new String[] {
          getWhirlpoolWallet().getWalletPremix().getPub(),
          getWhirlpoolWallet().getWalletPostmix().getPub()
        };

    Map<String, TxsResponse.Tx> txs = new LinkedHashMap<String, TxsResponse.Tx>();
    int page = -1;
    TxsResponse txsResponse;
    do {
      page++;
      txsResponse = backendApi.fetchTxs(zpubs, page, FETCH_TXS_PER_PAGE);
      if (txsResponse == null) {
        log.warn("Resync aborted: fetchTxs() is not available");
        break;
      }

      if (txsResponse.txs != null) {
        for (TxsResponse.Tx tx : txsResponse.txs) {
          txs.put(tx.hash, tx);
        }
      }
      log.info("Resync: fetching postmix history... " + txs.size() + "/" + txsResponse.n_tx);
    } while ((page * FETCH_TXS_PER_PAGE) < txsResponse.n_tx);
    return txs;
  }

  private Collection<WhirlpoolUtxo> filterRemixableUtxos(Collection<WhirlpoolUtxo> whirlpoolUtxos) {
    return StreamSupport.stream(whirlpoolUtxos)
        .filter(
            new Predicate<WhirlpoolUtxo>() {
              @Override
              public boolean test(WhirlpoolUtxo whirlpoolUtxo) {
                return !MixableStatus.NO_POOL.equals(
                    whirlpoolUtxo.getUtxoState().getMixableStatus());
              }
            })
        .collect(Collectors.<WhirlpoolUtxo>toList());
  }

  private void initWallet(String pub) throws Exception {
    for (int i = 0; i < INITWALLET_RETRY; i++) {
      log.info(" • Initializing wallet");
      try {
        backendApi.initBip84(pub);
        return; // success
      } catch (Exception e) {
        if (log.isDebugEnabled()) {
          log.error("", e);
        }
        log.error(
            " x Initializing wallet failed, retrying... ("
                + (i + 1)
                + "/"
                + INITWALLET_RETRY
                + ")");
        Thread.sleep(INITWALLET_RETRY_TIMEOUT);
      }
    }
    throw new NotifiableException("Unable to initialize Bip84 wallet");
  }

  @Override
  public void open() throws Exception {
    super.open();

    if (backendWsApi != null) {
      this.startBackendWsApi();
    }
  }

  protected void startBackendWsApi() throws Exception {
    backendWsApi.connect(
        new MessageListener<Void>() {
          @Override
          public void onMessage(Void foo) {
            try {
              // watch blocks
              backendWsApi.subscribeBlock(
                  new MessageListener() {
                    @Override
                    public void onMessage(Object message) {
                      if (log.isDebugEnabled()) {
                        log.debug("new block received -> refreshing walletData");
                        try {
                          refresh();
                        } catch (Exception e) {
                          log.error("", e);
                        }
                      }
                    }
                  });

              // watch addresses
              String[] pubs = getWalletSupplier().getPubs(true);
              backendWsApi.subscribeAddress(
                  pubs,
                  new MessageListener() {
                    @Override
                    public void onMessage(Object message) {
                      if (log.isDebugEnabled()) {
                        log.debug("new address received -> refreshing walletData");
                        try {
                          refresh();
                        } catch (Exception e) {
                          log.error("", e);
                        }
                      }
                    }
                  });
            } catch (Exception e) {
              log.error("", e);
            }
          }
        },
        true);
  }

  @Override
  public void close() throws Exception {
    super.close();

    // disconnect backend websocket
    if (backendWsApi != null) {
      backendWsApi.disconnect();
    }
  }

  @Override
  protected WalletResponse fetchWalletResponse() throws Exception {
    String[] pubs = getWalletSupplier().getPubs(true);
    return backendApi.fetchWallet(pubs);
  }

  @Override
  public void pushTx(String txHex) throws Exception {
    backendApi.pushTx(txHex);
  }

  @Override
  public void pushTx(String txHex, List<Integer> strictModeVouts) throws Exception {
    backendApi.pushTx(txHex, strictModeVouts);
  }

  public BackendApi getBackendApi() {
    return backendApi;
  }

  public BackendWsApi getBackendWsApi() {
    return backendWsApi;
  }
}
