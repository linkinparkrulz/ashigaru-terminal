package com.samourai.wallet.bip69;

import com.samourai.wallet.send.MyTransactionInput;
import org.bouncycastle.util.encoders.Hex;

public class BIP69MyTransactionInputComparator extends com.samourai.wallet.bip69.BIP69InputComparator {

    public int compare(MyTransactionInput i1, MyTransactionInput i2) {

        byte[] h1 = Hex.decode(i1.getTxHash());
        byte[] h2 = Hex.decode(i2.getTxHash());

        int index1 = i1.getTxPos();
        int index2 = i2.getTxPos();

        return super.compare(h1, h2, index1, index2);
    }

}