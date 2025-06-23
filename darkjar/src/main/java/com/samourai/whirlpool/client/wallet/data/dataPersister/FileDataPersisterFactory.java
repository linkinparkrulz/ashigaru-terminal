package com.samourai.whirlpool.client.wallet.data.dataPersister;

import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.ExternalDestination;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigPersistedSupplier;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigPersister;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.PersistableWalletStateSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStatePersister;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;

import java.io.File;

public class FileDataPersisterFactory implements DataPersisterFactory {

  public FileDataPersisterFactory() {}

  @Override
  public DataPersister createDataPersister(WhirlpoolWallet whirlpoolWallet, HD_Wallet bip44w)
      throws Exception {
    int persistDelaySeconds = whirlpoolWallet.getConfig().getPersistDelaySeconds();
    WalletStateSupplier walletStateSupplier = computeWalletStateSupplier(whirlpoolWallet);
    UtxoConfigSupplier utxoConfigSupplier = computeUtxoConfigSupplier(whirlpoolWallet);
    return new FileDataPersister(
        whirlpoolWallet, persistDelaySeconds, walletStateSupplier, utxoConfigSupplier);
  }

  protected WalletStateSupplier computeWalletStateSupplier(WhirlpoolWallet whirlpoolWallet)
      throws Exception {
    WalletStatePersister persister = computeWalletStatePersister(whirlpoolWallet);
    ExternalDestination externalDestination = whirlpoolWallet.getConfig().getExternalDestination();
    return new PersistableWalletStateSupplier(persister, externalDestination);
  }

  protected UtxoConfigSupplier computeUtxoConfigSupplier(WhirlpoolWallet whirlpoolWallet)
      throws Exception {
    UtxoConfigPersister persister = computeUtxoConfigPersister(whirlpoolWallet);
    return new UtxoConfigPersistedSupplier(persister);
  }

  protected WalletStatePersister computeWalletStatePersister(WhirlpoolWallet whirlpoolWallet)
      throws Exception {
    String walletStateFileName =
        computeFileIndex(whirlpoolWallet.getWalletIdentifier()).getAbsolutePath();
    return new WalletStatePersister(walletStateFileName);
  }

  protected UtxoConfigPersister computeUtxoConfigPersister(WhirlpoolWallet whirlpoolWallet)
      throws Exception {
    String utxoConfigFileName =
        computeFileUtxos(whirlpoolWallet.getWalletIdentifier()).getAbsolutePath();
    return new UtxoConfigPersister(utxoConfigFileName);
  }

  protected File computeFileIndex(String walletIdentifier) throws NotifiableException {
    String fileName = "whirlpool-cli-state-" + walletIdentifier + ".json";
    return computeFile(fileName);
  }

  protected File computeFileUtxos(String walletIdentifier) throws NotifiableException {
    String fileName = "whirlpool-cli-utxos-" + walletIdentifier + ".json";
    return computeFile(fileName);
  }

  protected File computeFile(String fileName) throws NotifiableException {
    return ClientUtils.createFile(fileName); // use current directory
  }
}
