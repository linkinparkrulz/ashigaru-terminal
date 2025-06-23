package com.samourai.whirlpool.client.wallet.data.utxo;

import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.hd.AddressType;
import com.samourai.wallet.send.provider.UtxoProvider;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;

import java.util.Collection;

public interface UtxoSupplier extends UtxoProvider {

  Collection<WhirlpoolUtxo> findUtxos(final WhirlpoolAccount... whirlpoolAccounts);

  Collection<WhirlpoolUtxo> findUtxos(
          final AddressType addressType, final WhirlpoolAccount... whirlpoolAccounts);

  Collection<WhirlpoolUtxo> findUtxosByAddress(String address);

  Collection<WalletResponse.Tx> findTxs(WhirlpoolAccount whirlpoolAccount);

  long getBalance(WhirlpoolAccount whirlpoolAccount);

  long getBalanceTotal();

  WhirlpoolUtxo findUtxo(String utxoHash, int utxoIndex);

  UtxoData getValue();

  Long getLastUpdate();

  void refresh() throws Exception;
}
