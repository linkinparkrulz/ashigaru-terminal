package com.samourai.wallet.send.provider;

import org.bitcoinj.core.ECKey;

public interface UtxoKeyProvider {

    ECKey _getPrivKey(String utxoHash, int utxoIndex) throws Exception;
}
