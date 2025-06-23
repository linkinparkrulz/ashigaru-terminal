package com.samourai.wallet.api.backend.beans;

public class TxDetail {
    public String txid;
    public int size;
    public int vsize;
    public int version;
    public int locktime;
    public TxInput[] inputs;
    public TxOutput[] outputs;
    public long created;
    public InfoBlock block;
    public long fees;
    public int feerate;
    public int vfeerate;

    public static class TxInput {
        public int n;
        public long seq;
        public TxOut outpoint;
        public String sig;
        public String[] witness;
    }

    public static class TxOut {
        public String txid;
        public int vout;
        public long value;
        public String scriptpubkey;
    }

    public static class TxOutput {
        public int n;
        public long value;
        public String scriptpubkey;
        public String type;
        public String address;
    }

    public static class InfoBlock {
        public int height;
        public String hash;
        public long time;
    }
}
