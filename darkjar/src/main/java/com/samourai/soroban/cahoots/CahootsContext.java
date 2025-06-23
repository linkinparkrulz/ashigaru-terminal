package com.samourai.soroban.cahoots;

import com.samourai.wallet.cahoots.CahootsType;
import com.samourai.wallet.cahoots.CahootsTypeUser;
import com.samourai.soroban.client.SorobanContext;

public class CahootsContext implements SorobanContext {
    private CahootsTypeUser typeUser;
    private CahootsType cahootsType;
    private Long amount;
    private String address;

    private CahootsContext(CahootsTypeUser typeUser, CahootsType cahootsType, Long amount, String address) {
        this.typeUser = typeUser;
        this.cahootsType = cahootsType;
        this.amount = amount;
        this.address = address;
    }

    public static CahootsContext newCounterparty(CahootsType cahootsType) {
        return new CahootsContext(CahootsTypeUser.COUNTERPARTY, cahootsType, null, null);
    }

    public static CahootsContext newCounterpartyStowaway() {
        return newCounterparty(CahootsType.STOWAWAY);
    }

    public static CahootsContext newCounterpartyStonewallx2() {
        return newCounterparty(CahootsType.STONEWALLX2);
    }

    public static CahootsContext newInitiatorStowaway(long amount) {
        return new CahootsContext(CahootsTypeUser.SENDER, CahootsType.STOWAWAY, amount, null);
    }

    public static CahootsContext newInitiatorStonewallx2(long amount, String address) {
        return new CahootsContext(CahootsTypeUser.SENDER, CahootsType.STONEWALLX2, amount, address);
    }

    public CahootsTypeUser getTypeUser() {
        return typeUser;
    }

    public CahootsType getCahootsType() {
        return cahootsType;
    }

    public Long getAmount() {
        return amount;
    }

    public String getAddress() {
        return address;
    }
}
