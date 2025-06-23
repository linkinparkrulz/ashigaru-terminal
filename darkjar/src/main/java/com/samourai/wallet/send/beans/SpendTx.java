package com.samourai.wallet.send.beans;

import com.samourai.wallet.hd.AddressType;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.SendFactoryGeneric;
import com.samourai.wallet.send.provider.UtxoKeyProvider;
import com.samourai.wallet.send.exceptions.MakeTxException;
import com.samourai.wallet.send.exceptions.SignTxException;
import com.samourai.wallet.send.exceptions.SpendException;
import com.samourai.wallet.send.spend.SpendSelection;
import com.samourai.wallet.send.spend.SpendSelectionBoltzmann;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class SpendTx {
    private static final Logger log = LoggerFactory.getLogger(SpendTx.class);
    private AddressType changeType;
    private long amount;
    private long fee;
    private long change;
    private SpendSelection spendSelection;
    private Map<String, Long> receivers;
    private boolean rbfOptIn;
    private int vSize;
    private int weight;
    private Transaction tx;

    public SpendTx(AddressType changeType, long amount, long fee, long change, SpendSelection spendSelection, Map<String, Long> receivers, boolean rbfOptIn, UtxoKeyProvider keyProvider, NetworkParameters params) throws SpendException {
        // consistency check
        long totalValueSelected = spendSelection.getTotalValueSelected();
        if((amount+fee+change) > totalValueSelected){
            // should never happen
            log.error("inconsistency detected! amount="+amount+", fee="+fee+", change="+change+", totalValueSelected="+totalValueSelected);
            throw new SpendException(SpendError.INSUFFICIENT_FUNDS);
        }

        this.changeType = changeType;
        this.amount = amount;
        this.fee = fee;
        this.spendSelection = spendSelection;
        this.receivers = receivers;
        this.change = change;
        this.rbfOptIn = rbfOptIn;

        this.tx = this.computeTx(keyProvider, params);
        this.vSize = tx.getVirtualTransactionSize();
        this.weight = tx.getWeight();
    }

    private Transaction computeTx(UtxoKeyProvider keyProvider, NetworkParameters params) throws SpendException {
        // spend tx
        Transaction tx;
        try {
            tx = SendFactoryGeneric.getInstance().makeTransaction(receivers, getSpendFrom(), rbfOptIn, params);
        } catch (MakeTxException e) {
            log.error("MakeTxException", e);
            throw new SpendException(SpendError.MAKING);
        }
        try {
            tx = SendFactoryGeneric.getInstance().signTransaction(tx, keyProvider);
        } catch (SignTxException e) {
            log.error("spendTx failed", e);
            throw new SpendException(SpendError.SIGNING);
        }
        byte[] serialized = tx.bitcoinSerialize();

        // check fee
        if (fee != tx.getFee().value) {
            log.error("fee check failed: "+fee+" vs "+tx.getFee().value);
            throw new SpendException(SpendError.MAKING);
        }
        if ((tx.hasWitness() && (fee < tx.getVirtualTransactionSize())) || (!tx.hasWitness() && (fee < serialized.length))) {
            throw new SpendException(SpendError.INSUFFICIENT_FEE);
        }

        if (log.isDebugEnabled()) {
            log.debug("size:" + serialized.length);
            log.debug("vsize:" + tx.getVirtualTransactionSize());
            log.debug("fee:" + tx.getFee().value);
        }

        /*final RBFSpend rbf;
        if (rbfOptIn) {
            rbf = new RBFSpend();
            for (TransactionInput input : tx.getInputs()) {
                String _addr = TxUtil.getInstance().getToAddress(input.getConnectedOutput());
                AddressType addressType = AddressType.findByAddress(_addr, params);
                String path = APIFactory.getInstance(TxAnimUIActivity.this).getUnspentPaths().get(_addr);
                if (path != null) {
                    if (addressType == AddressType.SEGWIT_NATIVE || addressType == AddressType.SEGWIT_COMPAT) {
                        path += "/"+addressType.getPurpose();
                    }
                    rbf.addKey(input.getOutpoint().toString(), path);
                } else {
                    // TODO zeroleak paymentcodes
                    /*String pcode = BIP47Meta.getInstance().getPCode4Addr(_addr);
                    int idx = BIP47Meta.getInstance().getIdx4Addr(_addr);
                    rbf.addKey(input.getOutpoint().toString(), pcode + "/" + idx);*//*
                }
            }
        } else {
            rbf = null;
        }

        // TODO zeroleak strict mode
        /*
        final List<Integer> strictModeVouts = new ArrayList<Integer>();
        if (SendParams.getInstance().getDestAddress() != null && SendParams.getInstance().getDestAddress().compareTo("") != 0 &&
                PrefsUtil.getInstance(TxAnimUIActivity.this).getValue(PrefsUtil.STRICT_OUTPUTS, true) == true) {
            List<Integer> idxs = SendParams.getInstance().getSpendOutputIndex(tx);
            for(int i = 0; i < tx.getOutputs().size(); i++)   {
                if(!idxs.contains(i))   {
                    strictModeVouts.add(i);
                }
            }
        }*/
        return tx;
    }

    public AddressType getChangeType() {
        return changeType;
    }

    public long getAmount() {
        return amount;
    }

    public long getFee() {
        return fee;
    }

    public long getChange() {
        return change;
    }

    public Map<String, Long> getSpendTo() {
        return receivers;
    }

    public List<MyTransactionOutPoint> getSpendFrom() {
        return spendSelection.getSpendFrom();
    }

    public SpendType getSpendType() {
        return spendSelection.getSpendType();
    }

    public int getvSize() {
        return vSize;
    }

    public int getWeight() {
        return weight;
    }

    public Transaction getTx() {
        return tx;
    }
}
