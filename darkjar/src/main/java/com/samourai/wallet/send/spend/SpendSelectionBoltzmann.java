package com.samourai.wallet.send.spend;

import com.samourai.wallet.hd.AddressType;
import com.samourai.wallet.send.BoltzmannUtil;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.send.provider.UtxoProvider;
import com.samourai.wallet.send.beans.SpendError;
import com.samourai.wallet.send.beans.SpendTx;
import com.samourai.wallet.send.beans.SpendType;
import com.samourai.wallet.send.exceptions.SpendException;
import com.samourai.wallet.util.TxUtil;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.TransactionOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;

public class SpendSelectionBoltzmann extends SpendSelection {
    private static final Logger log = LoggerFactory.getLogger(SpendSelectionBoltzmann.class);
    private static boolean TEST_MODE = false;
    private Pair<ArrayList<MyTransactionOutPoint>, ArrayList<TransactionOutput>> pair;

    public SpendSelectionBoltzmann(Pair<ArrayList<MyTransactionOutPoint>, ArrayList<TransactionOutput>> pair) {
        super(SpendType.STONEWALL);
        this.pair = pair;
    }

    public static SpendSelectionBoltzmann compute(long neededAmount, UtxoProvider utxoProvider, AddressType changeType, long amount, String address, WhirlpoolAccount account, AddressType forcedChangeType, NetworkParameters params, BigInteger feePerKb, Runnable restoreChangeIndexes) {
        if (log.isDebugEnabled()) {
            log.debug("needed amount:" + neededAmount);
        }

        Collection<UTXO> _utxos1 = null;
        Collection<UTXO> _utxos2 = null;

        Collection<UTXO> utxosP2WPKH = utxoProvider.getUtxos(account, AddressType.SEGWIT_NATIVE);
        Collection<UTXO> utxosP2SH_P2WPKH = utxoProvider.getUtxos(account, AddressType.SEGWIT_COMPAT);
        Collection<UTXO> utxosP2PKH = utxoProvider.getUtxos(account, AddressType.LEGACY);

        long valueP2WPKH = UTXO.sumValue(utxosP2WPKH);
        long valueP2SH_P2WPKH = UTXO.sumValue(utxosP2SH_P2WPKH);
        long valueP2PKH = UTXO.sumValue(utxosP2PKH);

        if (log.isDebugEnabled()) {
            log.debug("value P2WPKH:" + valueP2WPKH);
            log.debug("value P2SH_P2WPKH:" + valueP2SH_P2WPKH);
            log.debug("value P2PKH:" + valueP2PKH);
        }

        boolean selectedP2WPKH = false;
        boolean selectedP2SH_P2WPKH = false;
        boolean selectedP2PKH = false;

        if ((valueP2WPKH > (neededAmount * 2)) && changeType == AddressType.SEGWIT_NATIVE) {
            if (log.isDebugEnabled()) {
                log.debug("set 1 P2WPKH 2x");
            }
            _utxos1 = utxosP2WPKH;
            selectedP2WPKH = true;
        } else if (changeType == AddressType.SEGWIT_COMPAT && (valueP2SH_P2WPKH > (neededAmount * 2))) {
            if (log.isDebugEnabled()) {
                log.debug("set 1 P2SH_P2WPKH 2x");
            }
            _utxos1 = utxosP2SH_P2WPKH;
            selectedP2SH_P2WPKH = true;
        } else if (changeType == AddressType.LEGACY && (valueP2PKH > (neededAmount * 2))) {
            if (log.isDebugEnabled()) {
                log.debug("set 1 P2PKH 2x");
            }
            _utxos1 = utxosP2PKH;
            selectedP2PKH = true;
        } else if (valueP2WPKH > (neededAmount * 2)) {
            if (log.isDebugEnabled()) {
                log.debug("set 1 P2WPKH 2x");
            }
            _utxos1 = utxosP2WPKH;
            selectedP2WPKH = true;
        } else if (valueP2SH_P2WPKH > (neededAmount * 2)) {
            if (log.isDebugEnabled()) {
                log.debug("set 1 P2SH_P2WPKH 2x");
            }
            _utxos1 = utxosP2SH_P2WPKH;
            selectedP2SH_P2WPKH = true;
        } else if (valueP2PKH > (neededAmount * 2)) {
            if (log.isDebugEnabled()) {
                log.debug("set 1 P2PKH 2x");
            }
            _utxos1 = utxosP2PKH;
            selectedP2PKH = true;
        } else {
            ;
        }

        if (_utxos1 == null || _utxos1.size() == 0) {
            if (valueP2SH_P2WPKH > neededAmount) {
                if (log.isDebugEnabled()) {
                    log.debug("set 1 P2SH_P2WPKH");
                }
                _utxos1 = utxosP2SH_P2WPKH;
                selectedP2SH_P2WPKH = true;
            } else if (valueP2WPKH > neededAmount) {
                if (log.isDebugEnabled()) {
                    log.debug("set 1 P2WPKH");
                }
                _utxos1 = utxosP2WPKH;
                selectedP2WPKH = true;
            } else if (valueP2PKH > neededAmount) {
                if (log.isDebugEnabled()) {
                    log.debug("set 1 P2PKH");
                }
                _utxos1 = utxosP2PKH;
                selectedP2PKH = true;
            } else {
                ;
            }
        }

        if (_utxos1 != null && _utxos1.size() > 0) {
            if (!selectedP2SH_P2WPKH && valueP2SH_P2WPKH > neededAmount) {
                if (log.isDebugEnabled()) {
                    log.debug("set 2 P2SH_P2WPKH");
                }
                _utxos2 = utxosP2SH_P2WPKH;
                selectedP2SH_P2WPKH = true;
            }
            if (!selectedP2SH_P2WPKH && !selectedP2WPKH && valueP2WPKH > neededAmount) {
                if (log.isDebugEnabled()) {
                    log.debug("set 2 P2WPKH");
                }
                _utxos2 = utxosP2WPKH;
                selectedP2WPKH = true;
            }
            if (!selectedP2SH_P2WPKH && !selectedP2WPKH && !selectedP2PKH && valueP2PKH > neededAmount) {
                if (log.isDebugEnabled()) {
                    log.debug("set 2 P2PKH");
                }
                _utxos2 = utxosP2PKH;
                selectedP2PKH = true;
            } else {
                ;
            }
        }

        if ((_utxos1 == null || _utxos1.size() == 0) && (_utxos2 == null || _utxos2.size() == 0)) {
            // can't do boltzmann, revert to SPEND_SIMPLE
            return null;
        }

        if (log.isDebugEnabled()) {
            log.debug("boltzmann spend");
        }

        List<UTXO> _utxos1Shuffled = new ArrayList<>(_utxos1);
        if (!TEST_MODE) {
            Collections.shuffle(_utxos1Shuffled);
        }
        List<UTXO> _utxos2Shuffled = null;
        if (_utxos2 != null && _utxos2.size() > 0) {
            _utxos2Shuffled = new ArrayList<>(_utxos2);
            if (!TEST_MODE) {
                Collections.shuffle(_utxos2Shuffled);
            }
        }

        // boltzmann spend (STONEWALL)
        Pair<ArrayList<MyTransactionOutPoint>, ArrayList<TransactionOutput>> pair = BoltzmannUtil.getInstance().boltzmann(_utxos1Shuffled, _utxos2Shuffled, BigInteger.valueOf(amount), address, account, utxoProvider, forcedChangeType, params, feePerKb);

        if (pair == null) {
            // can't do boltzmann, revert to SPEND_SIMPLE
            if (restoreChangeIndexes != null) {
                restoreChangeIndexes.run();
            }
            return null;
        }

        return new SpendSelectionBoltzmann(pair);
    }

    @Override
    public SpendTx spendTx(long amount, String address, AddressType changeType, WhirlpoolAccount account, boolean rbfOptIn, NetworkParameters params, BigInteger feePerKb, Runnable restoreChangeIndexes, UtxoProvider utxoProvider) throws SpendException {
        // select utxos for boltzmann
        long inputAmount = 0L;
        long outputAmount = 0L;

        for (MyTransactionOutPoint outpoint : pair.getLeft()) {
            UTXO u = new UTXO();
            List<MyTransactionOutPoint> outs = new ArrayList<MyTransactionOutPoint>();
            outs.add(outpoint);
            u.setOutpoints(outs);
            addSelectedUTXO(u);
            inputAmount += u.getValue();
        }

        Map<String, Long> receivers = new HashMap<>();
        for (TransactionOutput output : pair.getRight()) {
            try {
                String outputAddress = TxUtil.getInstance().getToAddress(output);
                receivers.put(outputAddress, output.getValue().longValue());
                outputAmount += output.getValue().longValue();
            } catch (Exception e) {
                throw new SpendException(SpendError.BIP126_OUTPUT);
            }
        }

        BigInteger fee = BigInteger.valueOf(inputAmount - outputAmount);
        long change = computeChange(amount, fee);
        return new SpendTx(changeType, amount, fee.longValue(), change, this, receivers, rbfOptIn, utxoProvider, params);
    }

    public static void _setTestMode(boolean testMode) {
        TEST_MODE = testMode;
    }
}
