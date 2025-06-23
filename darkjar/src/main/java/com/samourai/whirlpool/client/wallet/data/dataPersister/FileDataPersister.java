package com.samourai.whirlpool.client.wallet.data.dataPersister;

import com.samourai.wallet.util.AbstractOrchestrator;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** DataPersister based on filesystem. */
public class FileDataPersister implements DataPersister {
  private static final Logger log = LoggerFactory.getLogger(FileDataPersister.class);

  private AbstractOrchestrator persistOrchestrator;

  private final WhirlpoolWallet whirlpoolWallet;
  private int persistDelaySeconds;

  private final WalletStateSupplier walletStateSupplier;
  private final UtxoConfigSupplier utxoConfigSupplier;

  public FileDataPersister(
      WhirlpoolWallet whirlpoolWallet,
      int persistDelaySeconds,
      WalletStateSupplier walletStateSupplier,
      UtxoConfigSupplier utxoConfigSupplier)
      throws Exception {
    this.whirlpoolWallet = whirlpoolWallet;
    this.persistDelaySeconds = persistDelaySeconds;

    this.walletStateSupplier = walletStateSupplier;
    this.utxoConfigSupplier = utxoConfigSupplier;
  }

  @Override
  public void load() throws Exception {
    // load persistance once
    utxoConfigSupplier.load();
    walletStateSupplier.load();
  }

  @Override
  public void open() throws Exception {
    // start persist orchestrator
    startPersistOrchestrator();
  }

  protected void startPersistOrchestrator() {
    persistOrchestrator =
        new AbstractOrchestrator(persistDelaySeconds * 1000) {
          @Override
          protected void runOrchestrator() {
            try {
              persist(false);
            } catch (Exception e) {
              log.error("", e);
            }
          }
        };
    persistOrchestrator.start(true);
  }

  @Override
  public void close() throws Exception {
    persistOrchestrator.stop();
  }

  @Override
  public void persist(boolean force) throws Exception {
    utxoConfigSupplier.persist(force);
    walletStateSupplier.persist(force);
  }

  protected WhirlpoolWallet getWhirlpoolWallet() {
    return whirlpoolWallet;
  }

  @Override
  public WalletStateSupplier getWalletStateSupplier() {
    return walletStateSupplier;
  }

  @Override
  public UtxoConfigSupplier getUtxoConfigSupplier() {
    return utxoConfigSupplier;
  }
}
