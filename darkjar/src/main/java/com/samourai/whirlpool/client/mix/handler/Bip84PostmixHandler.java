package com.samourai.whirlpool.client.mix.handler;

import com.samourai.wallet.client.BipWalletAndAddressType;
import com.samourai.wallet.hd.Chain;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.IndexRange;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bip84PostmixHandler extends AbstractPostmixHandler {
    private static final Logger log = LoggerFactory.getLogger(Bip84PostmixHandler.class);

    private Bech32UtilGeneric bech32Util = Bech32UtilGeneric.getInstance();
    private BipWalletAndAddressType postmixWallet;
    private IndexRange indexRange;

    public Bip84PostmixHandler(
            NetworkParameters params, BipWalletAndAddressType postmixWallet, IndexRange indexRange) {
        super(postmixWallet.getIndexHandler(), params);
        this.postmixWallet = postmixWallet;
        this.indexRange = indexRange;
    }

    @Override
    protected MixDestination computeNextDestination() throws Exception {
        // index
        int index =
                ClientUtils.computeNextReceiveAddressIndex(
                        postmixWallet.getIndexHandler(), this.indexRange);

        // address
        HD_Address receiveAddress = postmixWallet.getAddressAt(Chain.RECEIVE.getIndex(), index);

        String address = bech32Util.toBech32(receiveAddress, params);
        String path = receiveAddress.getPathFull(postmixWallet.getAddressType());
        if (log.isDebugEnabled()) {
            log.debug("Mixing to POSTMIX -> receiveAddress=" + address + ", path=" + path);
        }
        return new MixDestination(DestinationType.POSTMIX, index, address, path);
    }
}
