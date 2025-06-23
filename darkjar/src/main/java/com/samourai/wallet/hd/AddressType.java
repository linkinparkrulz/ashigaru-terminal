package com.samourai.wallet.hd;

import com.google.common.base.Optional;
import com.samourai.wallet.util.FormatsUtilGeneric;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.NetworkParameters;

public enum AddressType {
    LEGACY("Original (P2PKH)", 44),
    SEGWIT_COMPAT("Segwit compatible (P2SH_P2WPKH)", 49),
    SEGWIT_NATIVE("Segwit native (P2WPKH)", 84);

    private String label;
    private int purpose;

    AddressType(String label, int purpose) {
        this.label = label;
        this.purpose = purpose;
    }

    public static Optional<AddressType> findByPurpose(int purpose) {
        for (AddressType item : AddressType.values()) {
            if (item.purpose == purpose) {
                return Optional.of(item);
            }
        }
        return Optional.absent();
    }

    public static AddressType findByAddress(String address, NetworkParameters params) {
        if (FormatsUtilGeneric.getInstance().isValidBech32(address)) {
            return SEGWIT_NATIVE;
        } else if (Address.fromBase58(params, address).isP2SHAddress()) {
            return AddressType.SEGWIT_COMPAT;
        }
        return AddressType.LEGACY;
    }

    public String getLabel() {
        return label;
    }

    public int getPurpose() {
        return purpose;
    }
}
