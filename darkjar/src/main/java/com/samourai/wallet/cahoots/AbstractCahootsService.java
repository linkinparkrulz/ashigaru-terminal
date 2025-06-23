package com.samourai.wallet.cahoots;

import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.util.TxUtil;
import com.samourai.wallet.whirlpool.WhirlpoolConst;
import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractCahootsService<T extends Cahoots> {
    private static final Logger log = LoggerFactory.getLogger(AbstractCahootsService.class);

    private static final TxUtil txUtil = TxUtil.getInstance();

    protected NetworkParameters params;

    public AbstractCahootsService(NetworkParameters params) {
        this.params = params;
    }

    public abstract T startCollaborator(CahootsWallet cahootsWallet, int account, T payload0) throws Exception;

    public abstract T reply(CahootsWallet cahootsWallet, T payload) throws Exception;

    protected HashMap<String, ECKey> computeKeyBag(Cahoots cahoots, List<CahootsUtxo> utxos) {
        // utxos by hash
        HashMap<String, CahootsUtxo> utxosByHash = new HashMap<String, CahootsUtxo>();
        for (CahootsUtxo utxo : utxos) {
            MyTransactionOutPoint outpoint = utxo.getOutpoint();
            utxosByHash.put(outpoint.getHash().toString() + "-" + outpoint.getIndex(), utxo);
        }

        Transaction transaction = cahoots.getTransaction();
        HashMap<String, ECKey> keyBag = new HashMap<String, ECKey>();
        for (TransactionInput input : transaction.getInputs()) {
            TransactionOutPoint outpoint = input.getOutpoint();
            String key = outpoint.getHash().toString() + "-" + outpoint.getIndex();
            if (utxosByHash.containsKey(key)) {
                CahootsUtxo utxo = utxosByHash.get(key);
                ECKey eckey = utxo.getKey();
                keyBag.put(outpoint.toString(), eckey);
            }
        }
        return keyBag;
    }

    // verify

    protected long computeSpendAmount(HashMap<String,ECKey> keyBag, CahootsWallet cahootsWallet, Cahoots cahoots, CahootsTypeUser typeUser) throws Exception {
        long spendAmount = 0;

        Transaction transaction = cahoots.getTransaction();
        for(TransactionInput input : transaction.getInputs()) {
            TransactionOutPoint outpoint = input.getOutpoint();
            if (keyBag.containsKey(outpoint.toString())) {
                Long inputValue = cahoots.getOutpoints().get(outpoint.getHash().toString() + "-" + outpoint.getIndex());
                if (inputValue != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("computeSpendAmount: +input "+inputValue);
                    }
                    spendAmount += inputValue;
                }
            }
        }

        int myAccount = typeUser.equals(CahootsTypeUser.SENDER) ? cahoots.getAccount() : getReceiveAccount(cahoots.getCounterpartyAccount());
        List<String> myOutputAddresses = computeMyOutputAddresses(cahootsWallet, myAccount);

        for(TransactionOutput output : transaction.getOutputs()) {
            String outputAddress = txUtil.getToAddress(output);
            if (outputAddress != null && myOutputAddresses.contains(outputAddress)) {
                if (output.getValue() != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("computeSpendAmount: -output " + output.getValue().longValue());
                    }
                    spendAmount -= output.getValue().longValue();
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("computeSpendAmount = " + spendAmount);
        }
        return spendAmount;
    }

    private List<String> computeMyOutputAddresses(CahootsWallet cahootsWallet, int myAccount) throws Exception {
        List<String> addresses = new LinkedList<String>();

        // compute change addresses
        Pair<Integer,Integer> idxAndChain = cahootsWallet.fetchChangeIndex(myAccount);
        int idx = idxAndChain.getLeft();
        int chain = idxAndChain.getRight();
        addresses.addAll(computeMyOutputAddresses(cahootsWallet, myAccount, chain, idx));

        // compute receive addresses
        if (myAccount != WhirlpoolConst.WHIRLPOOL_POSTMIX_ACCOUNT) {
            idxAndChain = cahootsWallet.fetchReceiveIndex(myAccount);
            idx = idxAndChain.getLeft();
            chain = idxAndChain.getRight();
            addresses.addAll(computeMyOutputAddresses(cahootsWallet, myAccount, chain, idx));
        }
        return addresses;
    }

    private List<String> computeMyOutputAddresses(CahootsWallet cahootsWallet, int account, int chain, int idx) throws Exception {
        List<String> addresses = new LinkedList<String>();
        for (int i=0; i<2; i++) {
            SegwitAddress segwitAddress = cahootsWallet.getBip84Wallet().getSegwitAddressAt(account, chain, idx+i);
            addresses.add(segwitAddress.getBech32AsString());
            if (log.isDebugEnabled()) {
                log.debug("myOutputAddress " + account + ":m/" + chain + "/" + (idx + i) + " = " + segwitAddress.getBech32AsString());
            }
        }
        return addresses;
    }

    protected int getReceiveAccount(int account) {
        return account;
    }
}
