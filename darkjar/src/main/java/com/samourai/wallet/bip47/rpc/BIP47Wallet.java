package com.samourai.wallet.bip47.rpc;

import com.samourai.wallet.hd.HD_Wallet;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;

/**
 *
 * BIP47Wallet.java : BIP47 wallet
 *
 */
public class BIP47Wallet extends HD_Wallet {

    /**
     * Constructor for wallet.
     *
     * @param int purpose
     * @param MnemonicCode mc mnemonic code object
     * @param NetworkParameters params
     * @param byte[] seed seed for this wallet
     * @param String passphrase optional BIP39 passphrase
     *
     */
    public BIP47Wallet(int purpose, MnemonicCode mc, NetworkParameters params, byte[] seed, String passphrase) throws MnemonicException.MnemonicLengthException {
        super(purpose, mc, params, seed, passphrase);
    }

    /**
     * Constructor for wallet.
     * @param hdWallet
     */
    public BIP47Wallet(HD_Wallet hdWallet) {
        super(47, hdWallet);
    }

    /**
     * Return account for submitted account id.
     *
     * @param int accountId
     *
     * @return Account
     *
     */
    @Override
    public BIP47Account getAccount(int accountId) {
        return new BIP47Account(mParams, mRoot, accountId);
    }

}
