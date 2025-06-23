package com.samourai.wallet.send;

import com.samourai.wallet.api.backend.beans.UnspentOutput;

import java.util.*;

//import org.apache.commons.lang3.tuple.Pair;

public class UTXO {

    private String path = null;

    private List<MyTransactionOutPoint> outpoints = null;

    public UTXO() {
        this(new ArrayList<MyTransactionOutPoint>(), null);
    }

    public UTXO(List<MyTransactionOutPoint> outpoints, String path) {
        this.outpoints = outpoints;
        this.path = path;
    }

    public Collection<UnspentOutput> toUnspentOutputs(String xpub) {
        List<UnspentOutput> unspentOutputs = new LinkedList<>();
        for (MyTransactionOutPoint outPoint : outpoints) {
            unspentOutputs.add(new UnspentOutput(outPoint, null, path, xpub));
        }
        return unspentOutputs;
    }

    public List<MyTransactionOutPoint> getOutpoints() {
        return outpoints;
    }

    public void setOutpoints(List<MyTransactionOutPoint> outpoints) {
        this.outpoints = outpoints;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getValue() {

        long value = 0L;

        for (MyTransactionOutPoint out : outpoints) {
            value += out.getValue().longValue();
        }

        return value;
    }

    public static long sumValue(Collection<UTXO> utxos) {
        long sum = 0L;
        for (UTXO utxo : utxos) {
            sum += utxo.getValue();
        }
        return sum;
    }

    // sorts in descending order by amount
    public static class UTXOComparator implements Comparator<UTXO> {

        public int compare(UTXO o1, UTXO o2) {

            final int BEFORE = -1;
            final int EQUAL = 0;
            final int AFTER = 1;

            if (o1.getValue() > o2.getValue()) {
                return BEFORE;
            } else if (o1.getValue() < o2.getValue()) {
                return AFTER;
            } else {
                return EQUAL;
            }

        }

    }

    // sorts in descending order by amount
    public static class OutpointComparator implements Comparator<MyTransactionOutPoint> {

        public int compare(MyTransactionOutPoint o1, MyTransactionOutPoint o2) {

            final int BEFORE = -1;
            final int EQUAL = 0;
            final int AFTER = 1;

            if (o1.getValue().longValue() > o2.getValue().longValue()) {
                return BEFORE;
            } else if (o1.getValue().longValue() < o2.getValue().longValue()) {
                return AFTER;
            } else {
                return EQUAL;
            }

        }

    }

    public static int countOutpoints(Collection<UTXO> utxos) {
        int ret = 0;
        for (UTXO utxo : utxos) {
            ret += utxo.getOutpoints().size();
        }
        return ret;
    }

}
