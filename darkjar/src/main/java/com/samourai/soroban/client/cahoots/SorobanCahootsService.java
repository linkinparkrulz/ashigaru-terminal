package com.samourai.soroban.client.cahoots;

import com.samourai.soroban.cahoots.CahootsContext;
import com.samourai.soroban.client.SorobanMessage;
import com.samourai.soroban.client.SorobanService;
import com.samourai.soroban.client.meeting.SorobanMeetingService;
import com.samourai.soroban.client.meeting.SorobanRequestMessage;
import com.samourai.soroban.client.meeting.SorobanResponseMessage;
import com.samourai.soroban.client.rpc.RpcClient;
import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.cahoots.CahootsType;
import com.samourai.wallet.cahoots.CahootsWallet;
import io.reactivex.Observable;
import java.security.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanCahootsService {
  private final Logger log = LoggerFactory.getLogger(SorobanCahootsService.class);

  private OnlineCahootsService onlineCahootsService;
  private SorobanService sorobanService;
  private SorobanMeetingService sorobanMeetingService;

  public SorobanCahootsService(
      OnlineCahootsService onlineCahootsService,
      SorobanService sorobanService,
      SorobanMeetingService sorobanMeetingService) {
    this.onlineCahootsService = onlineCahootsService;
    this.sorobanService = sorobanService;
    this.sorobanMeetingService = sorobanMeetingService;
  }

  public SorobanCahootsService(
      BIP47UtilGeneric bip47Util,
      Provider provider,
      CahootsWallet cahootsWallet,
      RpcClient rpcClient) {
    this(
        new OnlineCahootsService(cahootsWallet),
        new SorobanService(
            bip47Util,
            cahootsWallet.getParams(),
            provider,
            cahootsWallet.getBip47Wallet(),
            cahootsWallet.getBip47Account(),
            rpcClient),
        new SorobanMeetingService(
            bip47Util,
            cahootsWallet.getParams(),
            provider,
            cahootsWallet.getBip47Wallet(),
            cahootsWallet.getBip47Account(),
            rpcClient));
  }

  protected void checkTor() throws Exception {
    // override here
  }

  // meeting

  public Observable<SorobanRequestMessage> sendMeetingRequest(
      PaymentCode paymentCodeCounterParty, CahootsType type) throws Exception {
    checkTor();
    return sorobanMeetingService.sendMeetingRequest(paymentCodeCounterParty, type);
  }

  public Observable<SorobanRequestMessage> receiveMeetingRequest(long timeoutMs) throws Exception {
    checkTor();
    return sorobanMeetingService.receiveMeetingRequest(timeoutMs);
  }

  public Observable<SorobanResponseMessage> sendMeetingResponse(
      PaymentCode paymentCodeCounterParty, SorobanRequestMessage request, boolean accept)
      throws Exception {
    checkTor();
    return sorobanMeetingService.sendMeetingResponse(paymentCodeCounterParty, request, accept);
  }

  public Observable<SorobanResponseMessage> receiveMeetingResponse(
      PaymentCode paymentCodeCounterparty, SorobanRequestMessage request, long timeoutMs)
      throws Exception {
    checkTor();
    return sorobanMeetingService.receiveMeetingResponse(
        paymentCodeCounterparty, request, timeoutMs);
  }

  // cahoots

  public Observable<SorobanMessage> initiator(
      int account,
      CahootsContext cahootsContext,
      PaymentCode paymentCodeCounterparty,
      long timeoutMs)
      throws Exception {
    checkTor();
    OnlineCahootsMessage message = onlineCahootsService.initiate(account, cahootsContext);
    return sorobanService.initiator(
        account, cahootsContext, onlineCahootsService, paymentCodeCounterparty, timeoutMs, message);
  }

  public Observable<SorobanMessage> contributor(
      int account, CahootsContext cahootsContext, PaymentCode paymentCodeInitiator, long timeoutMs)
      throws Exception {
    checkTor();
    return sorobanService.contributor(
        account, cahootsContext, onlineCahootsService, paymentCodeInitiator, timeoutMs);
  }

  public SorobanService getSorobanService() {
    return sorobanService;
  }

  public OnlineCahootsService getOnlineCahootsService() {
    return onlineCahootsService;
  }

  public SorobanMeetingService getSorobanMeetingService() {
    return sorobanMeetingService;
  }
}
