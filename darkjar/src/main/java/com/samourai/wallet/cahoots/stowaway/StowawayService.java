package com.samourai.wallet.cahoots.stowaway;

import com.samourai.wallet.SamouraiWalletConst;
import com.samourai.wallet.cahoots.*;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.FeeUtil;
import com.samourai.wallet.util.RandomUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static com.samourai.wallet.whirlpool.WhirlpoolConst.WHIRLPOOL_POSTMIX_ACCOUNT;

public class StowawayService extends AbstractCahootsService<Stowaway> {
    private static final Logger log = LoggerFactory.getLogger(StowawayService.class);
    private static final Bech32UtilGeneric bech32Util = Bech32UtilGeneric.getInstance();

    public StowawayService(NetworkParameters params) {
        super(params);
    }

    public Stowaway startInitiator(CahootsWallet cahootsWallet, long amount, int account) throws Exception {
        if (amount <= 0) {
            throw new Exception("Invalid amount");
        }
        byte[] fingerprint = cahootsWallet.getBip84Wallet().getFingerprint();
        Stowaway stowaway0 = doStowaway0(amount, account, fingerprint);
        if (log.isDebugEnabled()) {
            log.debug("# Stowaway INITIATOR => step="+stowaway0.getStep());
        }
        return stowaway0;
    }

    @Override
    public Stowaway startCollaborator(CahootsWallet cahootsWallet, int account, Stowaway stowaway0) throws Exception {
        Stowaway stowaway1 = doStowaway1(stowaway0, cahootsWallet, account);
        if (log.isDebugEnabled()) {
            log.debug("# Stowaway COUNTERPARTY => step="+stowaway1.getStep());
        }
        return stowaway1;
    }

    @Override
    public Stowaway reply(CahootsWallet cahootsWallet, Stowaway stowaway) throws Exception {
        int step = stowaway.getStep();
        if (log.isDebugEnabled()) {
            log.debug("# Stowaway <= step="+step);
        }
        Stowaway payload;
        switch (step) {
            case 1:
                payload = doStowaway2(stowaway, cahootsWallet);
                break;
            case 2:
                payload = doStowaway3(stowaway, cahootsWallet);
                break;
            case 3:
                payload = doStowaway4(stowaway, cahootsWallet);
                break;
            default:
                throw new Exception("Unrecognized #Cahoots step");
        }
        if (payload == null) {
            throw new Exception("Cannot compose #Cahoots");
        }
        if (log.isDebugEnabled()) {
            log.debug("# Stowaway => step="+payload.getStep());
        }
        return payload;
    }

    //
    // sender
    //
    private Stowaway doStowaway0(long spendAmount, int account, byte[] fingerprint) {
        //
        //
        // step0: B sends spend amount to A,  creates step0
        //
        //
        if (log.isDebugEnabled()) {
            log.debug("sender account (0):" + account);
        }
        Stowaway stowaway0 = new Stowaway(spendAmount, params, account);
        stowaway0.setFingerprint(fingerprint);
        return stowaway0;
    }

    //
    // receiver
    //
    private Stowaway doStowaway1(Stowaway stowaway0, CahootsWallet cahootsWallet, int account) throws Exception {
        HD_Wallet bip84Wallet = cahootsWallet.getBip84Wallet();

        byte[] fingerprint = bip84Wallet.getFingerprint();
        stowaway0.setFingerprintCollab(fingerprint);

        List<CahootsUtxo> utxos = cahootsWallet.getUtxosWpkhByAccount(account);
        // sort in descending order by value
        Collections.sort(utxos, new UTXO.UTXOComparator());
        if (log.isDebugEnabled()) {
            log.debug("BIP84 utxos:" + utxos.size());
        }

        List<CahootsUtxo> selectedUTXO = new ArrayList<CahootsUtxo>();
        long totalContributedAmount = 0L;
        List<CahootsUtxo> highUTXO = new ArrayList<CahootsUtxo>();
        for (CahootsUtxo utxo : utxos) {
            if (utxo.getValue() > stowaway0.getSpendAmount() + SamouraiWalletConst.bDust.longValue()) {
                highUTXO.add(utxo);
            }
        }
        if(highUTXO.size() > 0)    {
            SecureRandom random = RandomUtil.getSecureRandom();
            CahootsUtxo utxo = highUTXO.get(random.nextInt(highUTXO.size()));
            if (log.isDebugEnabled()) {
                log.debug("BIP84 selected random utxo:" + utxo.getValue());
            }
            selectedUTXO.add(utxo);
            totalContributedAmount = utxo.getValue();
        }
        if (selectedUTXO.size() == 0) {
            for (CahootsUtxo utxo : utxos) {
                selectedUTXO.add(utxo);
                totalContributedAmount += utxo.getValue();
                if (log.isDebugEnabled()) {
                    log.debug("BIP84 selected utxo:" + utxo.getValue());
                }
                if (stowaway0.isContributedAmountSufficient(totalContributedAmount)) {
                    break;
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug(selectedUTXO.size()+" selected utxos, totalContributedAmount="+totalContributedAmount+", requiredAmount="+stowaway0.computeRequiredAmount());
        }
        if (!stowaway0.isContributedAmountSufficient(totalContributedAmount)) {
            throw new Exception("Cannot compose #Cahoots: insufficient wallet balance");
        }

        NetworkParameters params = stowaway0.getParams();

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
            inputsA.put(_outpoint, Triple.of(eckey.getPubKey(), stowaway0.getFingerprintCollab(), path));
        }

        // destination output
        int receiveAccount = getReceiveAccount(account);
        int idx = bip84Wallet.getAccount(receiveAccount).getReceive().getAddrIdx();
        SegwitAddress segwitAddress = bip84Wallet.getSegwitAddressAt(receiveAccount, 0, idx);
        HashMap<_TransactionOutput, Triple<byte[], byte[], String>> outputsA = new HashMap<_TransactionOutput, Triple<byte[], byte[], String>>();
        byte[] scriptPubKey_A = bech32Util.computeScriptPubKey(segwitAddress.getBech32AsString(), params);
        _TransactionOutput output_A0 = new _TransactionOutput(params, null, Coin.valueOf(stowaway0.getSpendAmount()), scriptPubKey_A);
        outputsA.put(output_A0, Triple.of(segwitAddress.getECKey().getPubKey(), stowaway0.getFingerprintCollab(), "M/0/" + idx));

        stowaway0.setDestination(segwitAddress.getBech32AsString());
        stowaway0.setCounterpartyAccount(account);

        Stowaway stowaway1 = new Stowaway(stowaway0);
        stowaway1.doStep1(inputsA, outputsA);

        return stowaway1;
    }

    //
    // sender
    //
    private Stowaway doStowaway2(Stowaway stowaway1, CahootsWallet cahootsWallet) throws Exception {

        if (log.isDebugEnabled()) {
            log.debug("sender account (2):" + stowaway1.getAccount());
        }

        Transaction transaction = stowaway1.getTransaction();
        if (log.isDebugEnabled()) {
            log.debug("step2 tx:" + Hex.toHexString(transaction.bitcoinSerialize()));
        }
        int nbIncomingInputs = transaction.getInputs().size();

        List<CahootsUtxo> utxos = cahootsWallet.getUtxosWpkhByAccount(stowaway1.getAccount());
        // sort in ascending order by value
        Collections.sort(utxos, new UTXO.UTXOComparator());
        Collections.reverse(utxos);

        if (log.isDebugEnabled()) {
            log.debug("BIP84 utxos:" + utxos.size());
        }

        List<CahootsUtxo> selectedUTXO = new ArrayList<CahootsUtxo>();
        int nbTotalSelectedOutPoints = 0;
        long totalSelectedAmount = 0L;
        List<CahootsUtxo> lowUTXO = new ArrayList<CahootsUtxo>();
        for (CahootsUtxo utxo : utxos) {
            if(utxo.getValue() < stowaway1.getSpendAmount())    {
                lowUTXO.add(utxo);
            }
        }

        long feePerB = cahootsWallet.fetchFeePerB();

        List<List<CahootsUtxo>> listOfLists = new ArrayList<List<CahootsUtxo>>();
        Collections.shuffle(lowUTXO);
        listOfLists.add(lowUTXO);
        listOfLists.add(utxos);
        for(List<CahootsUtxo> list : listOfLists)   {

            selectedUTXO.clear();
            totalSelectedAmount = 0L;
            nbTotalSelectedOutPoints = 0;

            for (CahootsUtxo utxo : list) {
                selectedUTXO.add(utxo);
                totalSelectedAmount += utxo.getValue();
                if (log.isDebugEnabled()) {
                    log.debug("BIP84 selected utxo:" + utxo.getValue());
                }
                nbTotalSelectedOutPoints ++;
                if (stowaway1.isContributedAmountSufficient(totalSelectedAmount, estimatedFee(nbTotalSelectedOutPoints, nbIncomingInputs, feePerB))) {

                    // discard "extra" utxo, if any
                    List<CahootsUtxo> _selectedUTXO = new ArrayList<CahootsUtxo>();
                    Collections.reverse(selectedUTXO);
                    int _nbTotalSelectedOutPoints = 0;
                    long _totalSelectedAmount = 0L;
                    for (CahootsUtxo utxoSel : selectedUTXO) {
                        _selectedUTXO.add(utxoSel);
                        _totalSelectedAmount += utxoSel.getValue();
                        if (log.isDebugEnabled()) {
                            log.debug("BIP84 post selected utxo:" + utxoSel.getValue());
                        }
                        _nbTotalSelectedOutPoints ++;
                        if (stowaway1.isContributedAmountSufficient(_totalSelectedAmount, estimatedFee(_nbTotalSelectedOutPoints, nbIncomingInputs, feePerB))) {
                            selectedUTXO.clear();
                            selectedUTXO.addAll(_selectedUTXO);
                            totalSelectedAmount = _totalSelectedAmount;
                            nbTotalSelectedOutPoints = _nbTotalSelectedOutPoints;
                            break;
                        }
                    }

                    break;
                }
            }
            if (stowaway1.isContributedAmountSufficient(totalSelectedAmount, estimatedFee(nbTotalSelectedOutPoints, nbIncomingInputs, feePerB))) {
                break;
            }
        }

        long estimatedFee = estimatedFee(nbTotalSelectedOutPoints, nbIncomingInputs, feePerB);
        if (log.isDebugEnabled()) {
            log.debug(selectedUTXO.size()+" selected utxos, totalContributedAmount="+totalSelectedAmount+", requiredAmount="+stowaway1.computeRequiredAmount(estimatedFee));
        }
        if (!stowaway1.isContributedAmountSufficient(totalSelectedAmount, estimatedFee)) {
            throw new Exception("Cannot compose #Cahoots: insufficient wallet balance");
        }

        long fee = estimatedFee(nbTotalSelectedOutPoints, nbIncomingInputs, feePerB);
        if (log.isDebugEnabled()) {
            log.debug("fee:" + fee);
        }

        NetworkParameters params = stowaway1.getParams();

        //
        //
        // step2: B verif, utxos -> A (take smallest that cover amount)
        //
        //

        HD_Wallet bip84Wallet = cahootsWallet.getBip84Wallet();
        HashMap<MyTransactionOutPoint, Triple<byte[], byte[], String>> inputsB = new HashMap<MyTransactionOutPoint, Triple<byte[], byte[], String>>();

        for (CahootsUtxo utxo : selectedUTXO) {
            MyTransactionOutPoint _outpoint = utxo.getOutpoint();
            ECKey eckey = utxo.getKey();
            String path = utxo.getPath();
            inputsB.put(_outpoint, Triple.of(eckey.getPubKey(), stowaway1.getFingerprint(), path));
        }

        if (log.isDebugEnabled()) {
            log.debug("inputsB:" + inputsB.size());
        }

        // change output
        Pair<Integer,Integer> idxAndChain = cahootsWallet.fetchChangeIndex(stowaway1.getAccount());
        int idx = idxAndChain.getLeft();
        int chain = idxAndChain.getRight();
        SegwitAddress segwitAddress = bip84Wallet.getSegwitAddressAt(stowaway1.getAccount(), chain, idx);
        HashMap<_TransactionOutput, Triple<byte[], byte[], String>> outputsB = new HashMap<_TransactionOutput, Triple<byte[], byte[], String>>();
        byte[] scriptPubKey_B = bech32Util.computeScriptPubKey(segwitAddress.getBech32AsString(), params);
        _TransactionOutput output_B0 = new _TransactionOutput(params, null, Coin.valueOf((totalSelectedAmount - stowaway1.getSpendAmount()) - fee), scriptPubKey_B);
        outputsB.put(output_B0, Triple.of(segwitAddress.getECKey().getPubKey(), stowaway1.getFingerprint(), "M/1/" + idx));

        if (log.isDebugEnabled()) {
            log.debug("outputsB:" + outputsB.size());
        }

        Stowaway stowaway2 = new Stowaway(stowaway1);
        stowaway2.doStep2(inputsB, outputsB);
        stowaway2.setFeeAmount(fee);

        return stowaway2;
    }

    private long estimatedFee(int nbTotalSelectedOutPoints, int nbIncomingInputs, long feePerB) {
        return FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 2, 0, feePerB);
    }

    //
    // receiver
    //
    private Stowaway doStowaway3(Stowaway stowaway2, CahootsWallet cahootsWallet) throws Exception {
        List<CahootsUtxo> utxos = cahootsWallet.getUtxosWpkhByAccount(stowaway2.getCounterpartyAccount());
        HashMap<String, ECKey> keyBag_A = computeKeyBag(stowaway2, utxos);

        Stowaway stowaway3 = new Stowaway(stowaway2);
        stowaway3.doStep3(keyBag_A);

        // compute verifiedSpendAmount
        long verifiedSpendAmount = computeSpendAmount(keyBag_A, cahootsWallet, stowaway3, CahootsTypeUser.COUNTERPARTY);
        stowaway3.setVerifiedSpendAmount(verifiedSpendAmount);
        return stowaway3;
    }

    //
    // sender
    //
    private Stowaway doStowaway4(Stowaway stowaway3, CahootsWallet cahootsWallet) throws Exception {
        List<CahootsUtxo> utxos = cahootsWallet.getUtxosWpkhByAccount(stowaway3.getAccount());
        HashMap<String, ECKey> keyBag_B = computeKeyBag(stowaway3, utxos);

        Stowaway stowaway4 = new Stowaway(stowaway3);
        stowaway4.doStep4(keyBag_B);

        // compute verifiedSpendAmount
        long verifiedSpendAmount = computeSpendAmount(keyBag_B, cahootsWallet, stowaway4, CahootsTypeUser.SENDER);
        stowaway4.setVerifiedSpendAmount(verifiedSpendAmount);
        return stowaway4;
    }

    protected int getReceiveAccount(int account) {
        return account == WHIRLPOOL_POSTMIX_ACCOUNT ? 0 : account;
    }
}
