package com.samourai.wallet.cahoots;

import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.wallet.whirlpool.WhirlpoolConst;
import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.NetworkParameters;
import org.bouncycastle.util.encoders.Hex;

import java.util.LinkedList;
import java.util.List;

public abstract class CahootsWallet {
    private static final Bech32UtilGeneric bech32Util = Bech32UtilGeneric.getInstance();

    private HD_Wallet bip84Wallet;
    private BIP47Wallet bip47Wallet;
    private NetworkParameters params;

    public CahootsWallet(HD_Wallet bip84Wallet, NetworkParameters params) {
        this.bip84Wallet = bip84Wallet;
        this.bip47Wallet = new BIP47Wallet(bip84Wallet);
        this.params = params;
    }

    public abstract long fetchFeePerB();

    protected abstract int fetchPostChangeIndex();

    public Pair<Integer,Integer> fetchReceiveIndex(int account) throws Exception {
        int idx;
        int chain;
        if (account == 0) {
            idx = bip84Wallet.getAccount(0).getReceive().getAddrIdx();
            chain = 0;
        }
        else if (account == WhirlpoolConst.WHIRLPOOL_POSTMIX_ACCOUNT) {
            // force change chain
            idx = fetchPostChangeIndex();
            chain = 1;
        }
        else {
            throw new Exception("Invalid account: "+account);
        }
        return Pair.of(idx,chain);
    }

    public Pair<Integer,Integer> fetchChangeIndex(int account) throws Exception {
        int idx;
        int chain;
        if (account == 0) {
            idx = bip84Wallet.getAccount(0).getChange().getAddrIdx();
            chain = 1;
        }
        else if (account == WhirlpoolConst.WHIRLPOOL_POSTMIX_ACCOUNT) {
            idx = fetchPostChangeIndex();
            chain = 1;
        }
        else {
            throw new Exception("Invalid account: "+account);
        }
        return Pair.of(idx,chain);
    }

    protected abstract List<CahootsUtxo> fetchUtxos(int account);

    public NetworkParameters getParams() {
        return params;
    }

    public HD_Wallet getBip84Wallet() {
        return bip84Wallet;
    }

    public BIP47Wallet getBip47Wallet() {
        return bip47Wallet;
    }

    public int getBip47Account() {
        return 0;
    }

    public List<CahootsUtxo> getUtxosWpkhByAccount(int account) {
        return filterUtxosWpkh(fetchUtxos(account));
    }

    protected static List<CahootsUtxo> filterUtxosWpkh(List<CahootsUtxo> utxos) {
        List<CahootsUtxo> filteredUtxos = new LinkedList<CahootsUtxo>();
        for(CahootsUtxo utxo : utxos)   {
            // filter wpkh
            String script = Hex.toHexString(utxo.getOutpoint().getScriptBytes());
            if (bech32Util.isP2WPKHScript(script)) {
                filteredUtxos.add(utxo);
            }
        }
        return filteredUtxos;
    }
}
