package com.samourai.wallet.cahoots;

import com.samourai.wallet.hd.HD_Wallet;
import org.bitcoinj.core.NetworkParameters;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SimpleCahootsWallet extends CahootsWallet {
    private int postChangeIndex;
    private long feePerB;
    private Map<Integer,List<CahootsUtxo>> utxosByAccount;

    public SimpleCahootsWallet(HD_Wallet bip84w, NetworkParameters params, int postChangeIndex, long feePerB) throws Exception {
        super(bip84w, params);
        this.postChangeIndex = postChangeIndex;
        this.feePerB = feePerB;
        this.utxosByAccount = new HashMap<Integer, List<CahootsUtxo>>();
    }

    @Override
    public long fetchFeePerB() {
        return feePerB;
    }

    @Override
    public int fetchPostChangeIndex() {
        return postChangeIndex;
    }

    @Override
    protected List<CahootsUtxo> fetchUtxos(int account) {
        return utxosByAccount.get(account);
    }

    public void addUtxo(int account, CahootsUtxo utxo) {
        if (!utxosByAccount.containsKey(account)) {
            utxosByAccount.put(account, new LinkedList<CahootsUtxo>());
        }
        utxosByAccount.get(account).add(utxo);
    }
}
