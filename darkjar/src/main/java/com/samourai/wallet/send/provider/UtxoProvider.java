package com.samourai.wallet.send.provider;

import com.samourai.wallet.hd.AddressType;
import com.samourai.wallet.send.UTXO;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;

import java.util.Collection;

public interface UtxoProvider extends UtxoKeyProvider {

    String getChangeAddress(WhirlpoolAccount account, AddressType addressType);

    Collection<UTXO> getUtxos(WhirlpoolAccount account);

    Collection<UTXO> getUtxos(WhirlpoolAccount account, AddressType addressType);
}
