package com.samourai.soroban.client.meeting;

import com.samourai.soroban.client.dialog.RpcDialog;
import com.samourai.soroban.client.dialog.User;
import com.samourai.soroban.client.rpc.RpcClient;
import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.cahoots.CahootsType;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import java.security.Provider;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanMeetingService {
  private static final Logger log = LoggerFactory.getLogger(SorobanMeetingService.class);

  private BIP47UtilGeneric bip47Util;
  private RpcClient rpc;
  private BIP47Wallet bip47w;
  private int bip47Account;
  private User user;

  public SorobanMeetingService(
      BIP47UtilGeneric bip47Util,
      NetworkParameters params,
      Provider provider,
      BIP47Wallet bip47w,
      int bip47Account,
      RpcClient rpcClient) {
    this.bip47Util = bip47Util;
    this.bip47w = bip47w;
    this.bip47Account = bip47Account;
    this.rpc = rpcClient;
    this.user = new User(bip47Util, bip47w, bip47Account, params, provider);
  }

  private PaymentCode getMyPaymentCode() {
    return bip47Util.getPaymentCode(bip47w, bip47Account);
  }

  public Observable<SorobanRequestMessage> sendMeetingRequest(
      PaymentCode paymentCodeCounterParty, CahootsType type) throws Exception {
    // send request
    RpcDialog dialog = new RpcDialog(rpc, user, paymentCodeCounterParty.toString());
    final SorobanRequestMessage request = new SorobanRequestMessage(type);
    if (log.isDebugEnabled()) {
      log.debug("[initiator] meeting request sending: " + request);
    }
    return dialog
        .sendWithSender(request, paymentCodeCounterParty)
        .map(
            new Function<Object, SorobanRequestMessage>() {
              @Override
              public SorobanRequestMessage apply(Object o) throws Exception {
                return request;
              }
            });
  }

  public Observable<SorobanRequestMessage> receiveMeetingRequest(long timeoutMs) throws Exception {
    RpcDialog dialog = new RpcDialog(rpc, user, getMyPaymentCode().toString());
    if (log.isDebugEnabled()) {
      log.debug("[contributor] listening");
    }
    return dialog
        .receiveWithSender(timeoutMs)
        .map(
            new Function<SorobanMessageWithSender, SorobanRequestMessage>() {
              @Override
              public SorobanRequestMessage apply(SorobanMessageWithSender message)
                  throws Exception {
                String sender = message.getSender();
                SorobanRequestMessage request = SorobanRequestMessage.parse(message.getPayload());
                request.setSender(sender); // set sender information
                if (log.isDebugEnabled()) {
                  log.debug("[contributor] meeting request received: " + request);
                }
                return request;
              }
            });
  }

  public Observable<SorobanResponseMessage> sendMeetingResponse(
      PaymentCode paymentCodeCounterParty, SorobanRequestMessage request, boolean accept)
      throws Exception {
    RpcDialog dialog = new RpcDialog(rpc, user, request);
    final SorobanResponseMessage response = new SorobanResponseMessage(accept);
    return dialog
        .send(response, paymentCodeCounterParty)
        .map(
            new Function<Object, SorobanResponseMessage>() {
              @Override
              public SorobanResponseMessage apply(Object o) throws Exception {
                if (log.isDebugEnabled()) {
                  log.debug("[contributor] meeting response sent: " + response);
                }
                return response;
              }
            });
  }

  public Observable<SorobanResponseMessage> receiveMeetingResponse(
      PaymentCode paymentCodeCounterParty, SorobanRequestMessage request, final long timeoutMs)
      throws Exception {
    // send request
    final RpcDialog dialog = new RpcDialog(rpc, user, request);
    return dialog
        .receive(paymentCodeCounterParty, timeoutMs)
        .map(
            new Function<String, SorobanResponseMessage>() {
              @Override
              public SorobanResponseMessage apply(String payload) throws Exception {
                SorobanResponseMessage response = SorobanResponseMessage.parse(payload);
                if (log.isDebugEnabled()) {
                  log.debug("[initiator] meeting response received: " + response);
                }
                return response;
              }
            });
  }
}
