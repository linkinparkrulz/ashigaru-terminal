package com.samourai.wallet.send;

import org.bitcoinj.core.*;
import org.bitcoinj.script.Script;

import java.math.BigInteger;

public class MyTransactionOutPoint extends TransactionOutPoint {

    private static final long serialVersionUID = 1L;
    private byte[] scriptBytes;
    private BigInteger value;
    private String address;
    private int confirmations;

    public MyTransactionOutPoint(NetworkParameters params, Sha256Hash txHash, int txOutputN, BigInteger value, byte[] scriptBytes, String address, int confirmations) throws ProtocolException {
        super(params, txOutputN, new Sha256Hash(txHash.getBytes()));
        this.scriptBytes = scriptBytes;
        this.value = value;
        this.address = address;
        this.confirmations = confirmations;
    }

    public int getConfirmations() {
        return confirmations;
    }

    public byte[] getScriptBytes() {
        return scriptBytes;
    }

    public Coin getValue() {
        return Coin.valueOf(value.longValue());
    }

    public String getAddress() {
        return address;
    }

    public void setConfirmations(int confirmations) {
        this.confirmations = confirmations;
    }

    public Script computeScript() {
        return new Script(scriptBytes);
    }

    public TransactionInput computeSpendInput() {
        return new TransactionInput(params, null, new byte[]{}, this, getValue());
    }

    @Override
    public TransactionOutput getConnectedOutput() {
        return new TransactionOutput(params, null, Coin.valueOf(value.longValue()), scriptBytes);
    }

    //@Override
    public byte[] getConnectedPubKeyScript() {
        return scriptBytes;
    }

    public int getTxOutputN() {
        return (int)getIndex();
    }

    public Sha256Hash getTxHash() {
        return getHash();
    }
}