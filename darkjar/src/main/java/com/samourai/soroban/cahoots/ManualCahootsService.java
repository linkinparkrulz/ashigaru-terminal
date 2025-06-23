package com.samourai.soroban.cahoots;

import com.samourai.soroban.client.SorobanInteraction;
import com.samourai.soroban.client.SorobanMessageService;
import com.samourai.soroban.client.SorobanReply;
import com.samourai.wallet.SamouraiWalletConst;
import com.samourai.wallet.cahoots.*;
import com.samourai.wallet.cahoots.stonewallx2.STONEWALLx2;
import com.samourai.wallet.cahoots.stonewallx2.Stonewallx2Service;
import com.samourai.wallet.cahoots.stowaway.Stowaway;
import com.samourai.wallet.cahoots.stowaway.StowawayService;
import java8.util.Optional;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManualCahootsService extends SorobanMessageService<ManualCahootsMessage, CahootsContext> {
    private static final Logger log = LoggerFactory.getLogger(ManualCahootsService.class);

    private CahootsWallet cahootsWallet;

    public ManualCahootsService(CahootsWallet cahootsWallet) {
        this.cahootsWallet = cahootsWallet;
    }

    public ManualCahootsMessage initiate(int account, CahootsContext cahootsContext) throws Exception {
        AbstractCahootsService cahootsService = newCahootsService(cahootsContext.getCahootsType());
        Cahoots payload0;
        switch(cahootsContext.getCahootsType()) {
            case STOWAWAY:
                payload0 = ((StowawayService)cahootsService).startInitiator(cahootsWallet, cahootsContext.getAmount(), account);
                break;
            case STONEWALLX2:
                payload0 = ((Stonewallx2Service)cahootsService).startInitiator(cahootsWallet, cahootsContext.getAmount(), account, cahootsContext.getAddress());
                break;
            default:
                throw new Exception("Unknown Cahoots type");
        }
        ManualCahootsMessage response = new ManualCahootsMessage(payload0);

        verifyResponse(cahootsContext, response);
        return response;
    }

    @Override
    public ManualCahootsMessage parse(String payload) throws Exception{
        return ManualCahootsMessage.parse(payload);
    }

    public SorobanReply reply(int account, ManualCahootsMessage request) throws Exception {
        return reply(account, null, request);
    }

    @Override
    public SorobanReply reply(int account, CahootsContext cahootsContext, ManualCahootsMessage request) throws Exception {
        if (cahootsContext != null) {
            verifyRequest(cahootsContext, request);
        }

        final AbstractCahootsService cahootsService = newCahootsService(request.getType());
        final Cahoots payload = request.getCahoots();
        SorobanReply response;
        if (payload.getStep() == 0) {
            // new Cahoots as counterparty/receiver
            Cahoots cahootsResponse = cahootsService.startCollaborator(cahootsWallet, account, payload);
            response = new ManualCahootsMessage(cahootsResponse);
        } else {
            // continue existing Cahoots

            // check for interaction
            Optional<TypeInteraction> optInteraction =
                    TypeInteraction.find(request.getTypeUser().getPartner(), request.getStep() + 1);
            if (optInteraction.isPresent()) {
                // reply interaction
                final TypeInteraction typeInteraction = optInteraction.get();
                switch (typeInteraction) {
                    case TX_BROADCAST:
                        Cahoots signedCahoots = cahootsService.reply(cahootsWallet, payload);
                        if (cahootsContext != null) {
                            verifyResponse(cahootsContext, signedCahoots);
                        }
                        response = new TxBroadcastInteraction(signedCahoots);
                        break;
                    default:
                        throw new Exception("Unknown typeInteraction: "+typeInteraction);
                }
            } else {
                // standard reply
                Cahoots cahootsResponse = cahootsService.reply(cahootsWallet, payload);
                response = new ManualCahootsMessage(cahootsResponse);
            }
        }
        if (cahootsContext != null && !(response instanceof SorobanInteraction)) {
            verifyResponse(cahootsContext, (ManualCahootsMessage)response);
        }
        return response;
    }

    private void verifyRequest(CahootsContext sorobanContext, ManualCahootsMessage message) throws Exception {
        CahootsTypeUser typeUserExpected = sorobanContext.getTypeUser().getPartner();
        doVerify(sorobanContext, message, typeUserExpected);
    }

    private void verifyResponse(CahootsContext cahootsContext, ManualCahootsMessage message) throws Exception {
        CahootsTypeUser typeUserExpected = cahootsContext.getTypeUser();
        doVerify(cahootsContext, message, typeUserExpected);

        verifyResponse(cahootsContext, message.getCahoots());
    }

    private void verifyResponse(CahootsContext cahootsContext, Cahoots cahoots) throws Exception {
        if (cahoots.getStep() >= 3) {
            // check fee
            long minerFee = cahoots.getFeeAmount();
            if (minerFee > SamouraiWalletConst.MAX_ACCEPTABLE_FEES) {
                throw new Exception("Cahoots fee too high: " + cahoots.getTransaction().getFee().longValue());
            }

            // check verifiedSpendAmount
            long maxSpendAmount = computeMaxSpendAmount(minerFee, cahootsContext);
            long verifiedSpendAmount = cahoots.getVerifiedSpendAmount();
            if (verifiedSpendAmount == 0) {
                throw new Exception("Cahoots spendAmount verification failed");
            }
            if (log.isDebugEnabled()) {
                log.debug(cahootsContext.getTypeUser()+" verifiedSpendAmount="+verifiedSpendAmount+", maxSpendAmount="+maxSpendAmount);
            }
            if (verifiedSpendAmount > maxSpendAmount) {
                throw new Exception("Cahoots verifiedSpendAmount mismatch: " + verifiedSpendAmount);
            }
        }
    }

    private void doVerify(CahootsContext cahootsContext, ManualCahootsMessage message, CahootsTypeUser typeUserExpected) throws Exception {
        Cahoots cahoots = message.getCahoots();

        // check type
        CahootsType cahootsType = CahootsType.find(cahoots.getType()).get();
        if (!cahootsType.equals(cahootsContext.getCahootsType())) {
            throw new Exception("Cahoots type mismatch");
        }
        switch (cahootsType) {
            case STONEWALLX2:
                if (!(cahoots instanceof STONEWALLx2)) {
                    throw new Exception("Cahoots instance type mismatch");
                }
                break;
            case STOWAWAY:
                if (!(cahoots instanceof Stowaway)) {
                    throw new Exception("Cahoots instance type mismatch");
                }
                break;
            default:
                throw new Exception("Unknown Cahoots type");
        }

        // check typeUser
        if (message.getTypeUser() == null) {
            throw new Exception("Cahoots typeUser required");
        }
        if (!message.getTypeUser().equals(typeUserExpected)) {
            throw new Exception("Cahoots typeUser mismatch");
        }
    }

    private long computeMaxSpendAmount(long minerFee, CahootsContext cahootsContext) throws Exception {
        long maxSpendAmount;
        switch (cahootsContext.getCahootsType()) {
            case STONEWALLX2:
                // shares minerFee
                long sharedMinerFee = minerFee / 2;
                switch (cahootsContext.getTypeUser()) {
                    case SENDER:
                        // spends amount + minerFee
                        maxSpendAmount = cahootsContext.getAmount()+sharedMinerFee;
                        break;
                    case COUNTERPARTY:
                        // receives money (maxSpendAmount < 0)
                        maxSpendAmount = sharedMinerFee;
                        break;
                    default:
                        throw new Exception("Unknown typeUser");
                }
                break;
            case STOWAWAY:
                switch (cahootsContext.getTypeUser()) {
                    case SENDER:
                        // spends amount + minerFee
                        maxSpendAmount = cahootsContext.getAmount()+minerFee;
                        break;
                    case COUNTERPARTY:
                        // receives money (<0)
                        maxSpendAmount = 0;
                        break;
                    default:
                        throw new Exception("Unknown typeUser");
                }
                break;
            default:
                throw new Exception("Unknown Cahoots type");
        }
        return maxSpendAmount;
    }

    private AbstractCahootsService newCahootsService(CahootsType cahootsType) throws Exception {
        NetworkParameters params = cahootsWallet.getParams();
        switch(cahootsType) {
            case STOWAWAY:
                return new StowawayService(params);
            case STONEWALLX2:
                return new Stonewallx2Service(params);
        }
        throw new Exception("Unrecognized #Cahoots");
    }
}
