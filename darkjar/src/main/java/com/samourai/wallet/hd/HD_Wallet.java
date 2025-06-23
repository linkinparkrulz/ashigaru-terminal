package com.samourai.wallet.hd;

import com.google.common.base.Joiner;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.util.FormatsUtilGeneric;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HD_Wallet {
    private byte[] mSeed = null;
    private String strPassphrase = null;
    private List<String> mWordList = null;

    protected DeterministicKey mRoot = null; // null when created from xpub

    protected Map<Integer,HD_Account> mAccounts = null;

    // contains xpub of #account0, or all xpubs from constructor
    protected String[] xpubs = null;

    protected NetworkParameters mParams = null;

    private HD_Wallet() { ; }

    /*
    create from seed+passphrase
     */
    public HD_Wallet(int purpose, MnemonicCode mc, NetworkParameters mParams, byte[] mSeed, String strPassphrase) throws MnemonicException.MnemonicLengthException {
        this(purpose, mc.toMnemonic(mSeed), mParams, mSeed, strPassphrase);
    }

    public HD_Wallet(int purpose, List<String> mWordList, NetworkParameters mParams, byte[] mSeed, String strPassphrase) {
        this.mSeed = mSeed;
        this.strPassphrase = strPassphrase;
        this.mWordList = mWordList;
        this.mParams = mParams;

        // compute rootKey for accounts
        this.mRoot = computeRootKey(purpose, mWordList, strPassphrase, mParams);

        // initialize mAccounts with account #0
        mAccounts = new LinkedHashMap<>();
        HD_Account hdAccount = getAccount(0);

        // xpubs will only contain account #0 (even if mAccounts contains more accounts)
        xpubs = new String[]{hdAccount.xpubstr()};
    }

    public HD_Wallet(int purpose, HD_Wallet inputWallet) {
        this(purpose, inputWallet.mWordList, inputWallet.mParams, inputWallet.mSeed, inputWallet.strPassphrase);
    }

    /*
    create from account xpub key(s)
     */
    public HD_Wallet(NetworkParameters params, String[] xpub) throws AddressFormatException {
        mParams = params;

        // initialize mAccounts and xpubs
        mAccounts = new LinkedHashMap<>();
        xpubs = new String[xpub.length];
        for(int i = 0; i < xpub.length; i++) {
            HD_Account account = new HD_Account(mParams, xpub[i], i);
            mAccounts.put(i, account);
            xpubs[i] = account.xpubstr();
        }
    }

    private static DeterministicKey computeRootKey(int purpose, List<String> mWordList, String strPassphrase, NetworkParameters params) {
        byte[] hd_seed = MnemonicCode.toSeed(mWordList, strPassphrase);
        DeterministicKey mKey = HDKeyDerivation.createMasterPrivateKey(hd_seed);
        DeterministicKey t1 = HDKeyDerivation.deriveChildKey(mKey, purpose|ChildNumber.HARDENED_BIT);
        int coin = FormatsUtilGeneric.getInstance().isTestNet(params) ? (1 | ChildNumber.HARDENED_BIT) : ChildNumber.HARDENED_BIT;
        DeterministicKey rootKey = HDKeyDerivation.deriveChildKey(t1, coin);
        return rootKey;
    }

    public byte[] getSeed() {
        return mSeed;
    }

    public String getSeedHex() {
        return org.bouncycastle.util.encoders.Hex.toHexString(mSeed);
    }

    public String getMnemonic() {
        return Joiner.on(" ").join(mWordList);
    }

    public String getPassphrase() {
        return strPassphrase;
    }

    public NetworkParameters getParams() {
        return mParams;
    }

    public HD_Account getAccount(int accountIdx) {
        HD_Account hdAccount = mAccounts.get(accountIdx);
        if (hdAccount == null) {
            hdAccount = new HD_Account(mParams, mRoot, accountIdx);
            mAccounts.put(accountIdx, hdAccount);
        }
        return hdAccount;
    }

    public String[] getXPUBs() {
        return xpubs;
    }

    public byte[] getFingerprint() {

        List<String> wordList = Arrays.asList(getMnemonic().split("\\s+"));
        String passphrase = getPassphrase();

        byte[] hd_seed = MnemonicCode.toSeed(wordList, passphrase.toString());
        DeterministicKey mKey = HDKeyDerivation.createMasterPrivateKey(hd_seed);
        int fp = mKey.getFingerprint();

        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(fp);
        byte[] buf = bb.array();

        return buf;

    }

    public HD_Address getAddressAt(int account, int chain, int idx) {
        return getAccount(account).getChain(chain).getAddressAt(idx);
    }

    public SegwitAddress getSegwitAddressAt(int account, int chain, int idx) {
        HD_Address addr = getAddressAt(account, chain, idx);
        SegwitAddress segwitAddress = new SegwitAddress(addr.getPubKey(), mParams);
        return segwitAddress;
    }

    public HD_Address getAddressAt(int account, UnspentOutput utxo) {
        return getAddressAt(account, utxo.computePathChainIndex(), utxo.computePathAddressIndex());
    }

    public void wipe() {
        for(HD_Account hdAccount : mAccounts.values())	{
            hdAccount.getReceive().setAddrIdx(0);
            hdAccount.getChange().setAddrIdx(0);
        }
    }
}
