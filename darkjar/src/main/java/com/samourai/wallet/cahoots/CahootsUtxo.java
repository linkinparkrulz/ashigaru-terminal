package com.samourai.wallet.cahoots;

import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.UTXO;
import org.bitcoinj.core.ECKey;

import java.util.Arrays;
import java.util.LinkedList;

public class CahootsUtxo extends UTXO {
    private MyTransactionOutPoint outpoint;
    private ECKey key;

    public CahootsUtxo(MyTransactionOutPoint cahootsOutpoint, String path, ECKey key) {
        super(new LinkedList<MyTransactionOutPoint>(Arrays.asList(new MyTransactionOutPoint[]{cahootsOutpoint})), path);
        this.outpoint = cahootsOutpoint;
        this.key = key;
    }

    public MyTransactionOutPoint getOutpoint() {
        return outpoint;
    }

    public ECKey getKey() {
        return key;
    }
}
