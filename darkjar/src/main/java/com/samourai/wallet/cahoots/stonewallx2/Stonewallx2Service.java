package com.samourai.wallet.cahoots.stonewallx2;

import com.samourai.wallet.cahoots.*;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.util.FeeUtil;
import com.samourai.wallet.util.FormatsUtilGeneric;
import com.samourai.wallet.whirlpool.WhirlpoolConst;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.bitcoinj.core.*;
import org.bitcoinj.script.Script;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class Stonewallx2Service extends AbstractCahootsService<STONEWALLx2> {
    private static final Logger log = LoggerFactory.getLogger(Stonewallx2Service.class);
    private static final Bech32UtilGeneric bech32Util = Bech32UtilGeneric.getInstance();

    public Stonewallx2Service(NetworkParameters params) {
        super(params);
    }

    public STONEWALLx2 startInitiator(CahootsWallet cahootsWallet, long amount, int account, String address) throws Exception {
        if (amount <= 0) {
            throw new Exception("Invalid amount");
        }
        if (StringUtils.isEmpty(address)) {
            throw new Exception("Invalid address");
        }
        byte[] fingerprint = cahootsWallet.getBip84Wallet().getFingerprint();
        STONEWALLx2 stonewall0 = doSTONEWALLx2_0(amount, address, account, fingerprint);
        if (log.isDebugEnabled()) {
            log.debug("# STONEWALLx2 INITIATOR => step="+stonewall0.getStep());
        }
        return stonewall0;
    }

    @Override
    public STONEWALLx2 startCollaborator(CahootsWallet cahootsWallet, int account, STONEWALLx2 stonewall0) throws Exception {
        STONEWALLx2 stonewall1 = doSTONEWALLx2_1(stonewall0, cahootsWallet, account);
        if (log.isDebugEnabled()) {
            log.debug("# STONEWALLx2 COUNTERPARTY => step="+stonewall1.getStep());
        }
        return stonewall1;
    }

    @Override
    public STONEWALLx2 reply(CahootsWallet cahootsWallet, STONEWALLx2 stonewall) throws Exception {
        int step = stonewall.getStep();
        if (log.isDebugEnabled()) {
            log.debug("# STONEWALLx2 <= step="+step);
        }
        STONEWALLx2 payload;
        switch (step) {
            case 1:
                payload = doSTONEWALLx2_2(stonewall, cahootsWallet);
                break;
            case 2:
                payload = doSTONEWALLx2_3(stonewall, cahootsWallet);
                break;
            case 3:
                payload = doSTONEWALLx2_4(stonewall, cahootsWallet);
                break;
            default:
                throw new Exception("Unrecognized #Cahoots step");
        }
        if (payload == null) {
            throw new Exception("Cannot compose #Cahoots");
        }
        if (log.isDebugEnabled()) {
            log.debug("# STONEWALLx2 => step="+payload.getStep());
        }
        return payload;
    }

    //
    // sender
    //
    private STONEWALLx2 doSTONEWALLx2_0(long spendAmount, String address, int account, byte[] fingerprint) {
        //
        //
        // step0: B sends spend amount to A,  creates step0
        //
        //
        STONEWALLx2 stonewall0 = new STONEWALLx2(spendAmount, address, params, account);
        stonewall0.setFingerprint(fingerprint);

        return stonewall0;
    }



    //
    // counterparty
    //
    private STONEWALLx2 doSTONEWALLx2_1(STONEWALLx2 stonewall0, CahootsWallet cahootsWallet, int account) throws Exception {
        HD_Wallet bip84Wallet = cahootsWallet.getBip84Wallet();

        stonewall0.setCounterpartyAccount(account);
        byte[] fingerprint = bip84Wallet.getFingerprint();
        stonewall0.setFingerprintCollab(fingerprint);

        List<CahootsUtxo> utxos = cahootsWallet.getUtxosWpkhByAccount(stonewall0.getCounterpartyAccount());
        Collections.shuffle(utxos);

        if (log.isDebugEnabled()) {
            log.debug("BIP84 utxos:" + utxos.size());
        }

        List<CahootsUtxo> selectedUTXO = new ArrayList<CahootsUtxo>();
        long totalContributedAmount = 0L;
        for (int step = 0; step < 3; step++) {

            if (stonewall0.getCounterpartyAccount() == 0) {
                step = 2;
            }

            List<String> seenTxs = new ArrayList<String>();
            selectedUTXO = new ArrayList<CahootsUtxo>();
            totalContributedAmount = 0L;
            for (CahootsUtxo utxo : utxos) {

                switch (step) {
                    case 0:
                        if (utxo.getPath() != null && utxo.getPath().length() > 3 && utxo.getPath().charAt(2) != '0') {
                            continue;
                        }
                        break;
                    case 1:
                        if (utxo.getPath() != null && utxo.getPath().length() > 3 && utxo.getPath().charAt(2) != '1') {
                            continue;
                        }
                        break;
                    default:
                        break;
                }

                MyTransactionOutPoint outpoint = utxo.getOutpoint();
                if (!seenTxs.contains(outpoint.getHash().toString())) {
                    seenTxs.add(outpoint.getHash().toString());

                    selectedUTXO.add(utxo);
                    totalContributedAmount += utxo.getValue();
                    if (log.isDebugEnabled()) {
                        log.debug("BIP84 selected utxo:" + utxo.getValue());
                    }
                }

                if (stonewall0.isContributedAmountSufficient(totalContributedAmount)) {
                    break;
                }
            }
            if (stonewall0.isContributedAmountSufficient(totalContributedAmount)) {
                break;
            }
        }

        if (log.isDebugEnabled()) {
            log.debug(selectedUTXO.size()+" selected utxos, totalContributedAmount="+totalContributedAmount+", requiredAmount="+stonewall0.computeRequiredAmount());
        }
        if (!stonewall0.isContributedAmountSufficient(totalContributedAmount)) {
            throw new Exception("Cannot compose #Cahoots: insufficient wallet balance");
        }

        NetworkParameters params = stonewall0.getParams();

        //
        //
        // step1: A utxos -> B (take largest that cover amount)
        //
        //

        HashMap<MyTransactionOutPoint, Triple<byte[], byte[], String>> inputsA = new HashMap<MyTransactionOutPoint, Triple<byte[], byte[], String>>();

        for (CahootsUtxo utxo : selectedUTXO) {
            MyTransactionOutPoint _outpoint = utxo.getOutpoint();
            ECKey eckey = utxo.getKey();
            String path = utxo.getPath();
            inputsA.put(_outpoint, Triple.of(eckey.getPubKey(), stonewall0.getFingerprintCollab(), path));
        }

        HashMap<_TransactionOutput, Triple<byte[], byte[], String>> outputsA = new HashMap<_TransactionOutput, Triple<byte[], byte[], String>>();
        // contributor mix output
        Pair<Integer,Integer> idxAndChain = cahootsWallet.fetchReceiveIndex(stonewall0.getCounterpartyAccount());
        int idx = idxAndChain.getLeft();
        int chain = idxAndChain.getRight();
        SegwitAddress segwitAddress0 = bip84Wallet.getSegwitAddressAt(stonewall0.getCounterpartyAccount(), chain, idx);
        if (log.isDebugEnabled()) {
            log.debug("+output "+stonewall0.getCounterpartyAccount()+":M/"+chain+"/"+idx+" (CounterParty mix) = "+segwitAddress0.getBech32AsString());
        }
        if (segwitAddress0.getBech32AsString().equalsIgnoreCase(stonewall0.getDestination())) {
            idx++;
            segwitAddress0 = bip84Wallet.getSegwitAddressAt(stonewall0.getCounterpartyAccount(), chain, idx);
        }
        byte[] scriptPubKey_A0 = bech32Util.computeScriptPubKey(segwitAddress0.getBech32AsString(), params);
        _TransactionOutput output_A0 = new _TransactionOutput(params, null, Coin.valueOf(stonewall0.getSpendAmount()), scriptPubKey_A0);
        outputsA.put(output_A0, Triple.of(segwitAddress0.getECKey().getPubKey(), stonewall0.getFingerprintCollab(), "M/"+chain+"/" + idx));

        // contributor change output
        chain = 1;
        if (stonewall0.getCounterpartyAccount() == WhirlpoolConst.WHIRLPOOL_POSTMIX_ACCOUNT) {
            ++idx;
        } else {
            idx = bip84Wallet.getAccount(stonewall0.getCounterpartyAccount()).getChange().getAddrIdx();
        }
        SegwitAddress segwitAddress1 = bip84Wallet.getSegwitAddressAt(stonewall0.getCounterpartyAccount(), chain, idx);
        if (log.isDebugEnabled()) {
            log.debug("+output " + stonewall0.getAccount() + ":M/" + chain + "/" + idx + " (CounterParty change) = " + segwitAddress1.getBech32AsString());
        }
        byte[] scriptPubKey_A1 = bech32Util.computeScriptPubKey(segwitAddress1.getBech32AsString(), params);
        _TransactionOutput output_A1 = new _TransactionOutput(params, null, Coin.valueOf(totalContributedAmount - stonewall0.getSpendAmount()), scriptPubKey_A1);
        outputsA.put(output_A1, Triple.of(segwitAddress1.getECKey().getPubKey(), stonewall0.getFingerprintCollab(), "M/"+chain+"/" + idx));
        stonewall0.setCollabChange(segwitAddress1.getBech32AsString());

        STONEWALLx2 stonewall1 = new STONEWALLx2(stonewall0);
        stonewall1.doStep1(inputsA, outputsA);

        return stonewall1;
    }

    //
    // sender
    //
    private STONEWALLx2 doSTONEWALLx2_2(STONEWALLx2 stonewall1, CahootsWallet cahootsWallet) throws Exception {

        Transaction transaction = stonewall1.getTransaction();
        if (log.isDebugEnabled()) {
            log.debug("step2 tx:" + Hex.toHexString(transaction.bitcoinSerialize()));
            log.debug("step2 tx:" + transaction);
        }
        int nbIncomingInputs = transaction.getInputs().size();

        List<CahootsUtxo> utxos = cahootsWallet.getUtxosWpkhByAccount(stonewall1.getAccount());
        Collections.shuffle(utxos);

        if (log.isDebugEnabled()) {
            log.debug("BIP84 utxos:" + utxos.size());
        }

        List<String> seenTxs = new ArrayList<String>();
        for (TransactionInput input : transaction.getInputs()) {
            if (!seenTxs.contains(input.getOutpoint().getHash().toString())) {
                seenTxs.add(input.getOutpoint().getHash().toString());
            }
        }

        long feePerB = cahootsWallet.fetchFeePerB();

        List<CahootsUtxo> selectedUTXO = new ArrayList<CahootsUtxo>();
        long totalSelectedAmount = 0L;
        int nbTotalSelectedOutPoints = 0;
        for (int step = 0; step < 3; step++) {

            if (stonewall1.getCounterpartyAccount() == 0) {
                step = 2;
            }

            List<String> _seenTxs = seenTxs;
            selectedUTXO = new ArrayList<CahootsUtxo>();
            nbTotalSelectedOutPoints = 0;
            for (CahootsUtxo utxo : utxos) {

                switch (step) {
                    case 0:
                        if (utxo.getPath() != null && utxo.getPath().length() > 3 && utxo.getPath().charAt(2) != '0') {
                            continue;
                        }
                        break;
                    case 1:
                        if (utxo.getPath() != null && utxo.getPath().length() > 3 && utxo.getPath().charAt(2) != '1') {
                            continue;
                        }
                        break;
                    default:
                        break;
                }

                if (!_seenTxs.contains(utxo.getOutpoint().getHash().toString())) {
                    _seenTxs.add(utxo.getOutpoint().getHash().toString());

                    selectedUTXO.add(utxo);
                    totalSelectedAmount += utxo.getValue();
                    nbTotalSelectedOutPoints ++;
                    if (log.isDebugEnabled()) {
                        log.debug("BIP84 selected utxo:" + utxo.getValue());
                    }
                }

                if (stonewall1.isContributedAmountSufficient(totalSelectedAmount, estimatedFee(nbTotalSelectedOutPoints, nbIncomingInputs, feePerB))) {
                    break;
                }
            }
            if (stonewall1.isContributedAmountSufficient(totalSelectedAmount, estimatedFee(nbTotalSelectedOutPoints, nbIncomingInputs, feePerB))) {
                break;
            }
        }
        long estimatedFee = estimatedFee(nbTotalSelectedOutPoints, nbIncomingInputs, feePerB);
        if (log.isDebugEnabled()) {
            log.debug(selectedUTXO.size()+" selected utxos, totalContributedAmount="+totalSelectedAmount+", requiredAmount="+stonewall1.computeRequiredAmount(estimatedFee));
        }
        if (!stonewall1.isContributedAmountSufficient(totalSelectedAmount, estimatedFee)) {
            throw new Exception("Cannot compose #Cahoots: insufficient wallet balance");
        }

        long fee = estimatedFee(nbTotalSelectedOutPoints, nbIncomingInputs, feePerB);
        if (log.isDebugEnabled()) {
            log.debug("fee:" + fee);
        }
        if (fee % 2L != 0) {
            fee++;
        }
        if (log.isDebugEnabled()) {
            log.debug("fee pair:" + fee);
        }
        stonewall1.setFeeAmount(fee);

        if (log.isDebugEnabled()) {
            log.debug("destination:" + stonewall1.getDestination());
        }
        if (transaction.getOutputs() != null && transaction.getOutputs().size() == 2) {

            int idx = -1;
            for (int i = 0; i < 2; i++) {
                byte[] buf = transaction.getOutputs().get(i).getScriptBytes();
                byte[] script = new byte[buf.length];
                script[0] = 0x00;
                System.arraycopy(buf, 1, script, 1, script.length - 1);
                if (log.isDebugEnabled()) {
                    log.debug("script:" + new Script(script).toString());
                    log.debug("script hex:" + Hex.toHexString(script));
                    log.debug("address from script:" + bech32Util.getAddressFromScript(new Script(script), params));
                }
                if(bech32Util.getAddressFromScript(new Script(script), params).equalsIgnoreCase(stonewall1.getCollabChange())) {
                    idx = i;
                    break;
                }
            }

            if(idx == 0 || idx == 1) {
                Coin value = transaction.getOutputs().get(idx).getValue();
                Coin _value = Coin.valueOf(value.longValue() - (fee / 2L));
                if (log.isDebugEnabled()) {
                    log.debug("output value post fee:" + _value);
                }
                transaction.getOutputs().get(idx).setValue(_value);
                stonewall1.getPSBT().setTransaction(transaction);
            }
            else {
                throw new Exception("Cannot compose #Cahoots: invalid tx outputs");
            }

        }
        else {
            log.error("outputs: "+transaction.getOutputs().size());
            log.error("tx:"+transaction.toString());
            throw new Exception("Cannot compose #Cahoots: invalid tx outputs count");
        }

        NetworkParameters params = stonewall1.getParams();

        //
        //
        // step2: B verif, utxos -> A (take smallest that cover amount)
        //
        //

        HD_Wallet bip84Wallet = cahootsWallet.getBip84Wallet();
        String zpub = bip84Wallet.getAccount(stonewall1.getAccount()).zpubstr();
        HashMap<MyTransactionOutPoint, Triple<byte[], byte[], String>> inputsB = new HashMap<MyTransactionOutPoint, Triple<byte[], byte[], String>>();

        for (CahootsUtxo utxo : selectedUTXO) {
            MyTransactionOutPoint _outpoint = utxo.getOutpoint();
            ECKey eckey = utxo.getKey();
            String path = utxo.getPath();
            inputsB.put(_outpoint, Triple.of(eckey.getPubKey(), FormatsUtilGeneric.getInstance().getFingerprintFromXPUB(zpub), path));
        }

        // spender change output
        HashMap<_TransactionOutput, Triple<byte[], byte[], String>> outputsB = new HashMap<_TransactionOutput, Triple<byte[], byte[], String>>();
        Pair<Integer,Integer> idxAndChain = cahootsWallet.fetchChangeIndex(stonewall1.getAccount());
        int idx = idxAndChain.getLeft();
        int chain = idxAndChain.getRight();
        SegwitAddress segwitAddress = bip84Wallet.getSegwitAddressAt(stonewall1.getAccount(), chain, idx);
        if (log.isDebugEnabled()) {
            log.debug("+output " + stonewall1.getAccount() + ":M/" + chain + "/" + idx + " (Spender change) = " + segwitAddress.getBech32AsString());
        }
        byte[] scriptPubKey_B0 = bech32Util.computeScriptPubKey(segwitAddress.getBech32AsString(), params);
        _TransactionOutput output_B0 = new _TransactionOutput(params, null, Coin.valueOf((totalSelectedAmount - stonewall1.getSpendAmount()) - (fee / 2L)), scriptPubKey_B0);
        outputsB.put(output_B0, Triple.of(segwitAddress.getECKey().getPubKey(), stonewall1.getFingerprint(), "M/"+chain+"/" + idx));

        STONEWALLx2 stonewall2 = new STONEWALLx2(stonewall1);
        stonewall2.doStep2(inputsB, outputsB);

        return stonewall2;
    }

    private long estimatedFee(int nbTotalSelectedOutPoints, int nbIncomingInputs, long feePerB) {
        return FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 4, 0, feePerB);
    }

    //
    // counterparty
    //
    private STONEWALLx2 doSTONEWALLx2_3(STONEWALLx2 stonewall2, CahootsWallet cahootsWallet) throws Exception {
        List<CahootsUtxo> utxos = cahootsWallet.getUtxosWpkhByAccount(stonewall2.getCounterpartyAccount());
        HashMap<String, ECKey> keyBag_A = computeKeyBag(stonewall2, utxos);

        STONEWALLx2 stonewall3 = new STONEWALLx2(stonewall2);
        stonewall3.doStep3(keyBag_A);

        // compute verifiedSpendAmount
        long verifiedSpendAmount = computeSpendAmount(keyBag_A, cahootsWallet, stonewall3, CahootsTypeUser.COUNTERPARTY);
        stonewall3.setVerifiedSpendAmount(verifiedSpendAmount);
        return stonewall3;
    }

    //
    // sender
    //
    private STONEWALLx2 doSTONEWALLx2_4(STONEWALLx2 stonewall3, CahootsWallet cahootsWallet) throws Exception {
        List<CahootsUtxo> utxos = cahootsWallet.getUtxosWpkhByAccount(stonewall3.getAccount());
        HashMap<String, ECKey> keyBag_B = computeKeyBag(stonewall3, utxos);

        STONEWALLx2 stonewall4 = new STONEWALLx2(stonewall3);
        stonewall4.doStep4(keyBag_B);

        // compute verifiedSpendAmount
        long verifiedSpendAmount = computeSpendAmount(keyBag_B, cahootsWallet, stonewall4, CahootsTypeUser.SENDER);
        stonewall4.setVerifiedSpendAmount(verifiedSpendAmount);
        return stonewall4;
    }
}
