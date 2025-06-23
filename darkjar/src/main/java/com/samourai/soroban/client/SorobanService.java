package com.samourai.soroban.client;

import com.samourai.soroban.cahoots.CahootsContext;
import com.samourai.soroban.cahoots.TxBroadcastInteraction;
import com.samourai.soroban.client.dialog.RpcDialog;
import com.samourai.soroban.client.dialog.User;
import com.samourai.soroban.client.rpc.RpcClient;
import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import io.reactivex.Observable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import java.security.Provider;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanService {
  private static final Logger log = LoggerFactory.getLogger(SorobanService.class);

  private static OnlineSorobanInteraction SUBJECT_RESET =
      new OnlineSorobanInteraction(new TxBroadcastInteraction(null), null);

  private NetworkParameters params;
  private RpcClient rpc;
  private User user;
  private Subject<SorobanMessage> interactiveMessageProvider;
  private Subject<OnlineSorobanInteraction> onInteraction;

  public SorobanService(
      BIP47UtilGeneric bip47Util,
      NetworkParameters params,
      Provider provider,
      BIP47Wallet bip47w,
      int bip47Account,
      RpcClient rpcClient) {
    this.params = params;
    this.rpc = rpcClient;
    this.user = new User(bip47Util, bip47w, bip47Account, params, provider);
    this.interactiveMessageProvider = null;
    this.onInteraction = BehaviorSubject.create();
  }

  public Observable<SorobanMessage> initiator(
      final int account,
      final CahootsContext cahootsContext,
      final SorobanMessageService messageService,
      final PaymentCode paymentCodeCounterParty,
      final long timeoutMs,
      final SorobanMessage message)
      throws Exception {
    reset();
    final BehaviorSubject<SorobanMessage> onMessage = BehaviorSubject.create();
    Thread t =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                RpcDialog dialogOrNull = null;
                try {
                  String initialDirectory =
                      user.getMeeetingAddressSend(paymentCodeCounterParty, params)
                          .getBech32AsString();

                  dialogOrNull = new RpcDialog(rpc, user, initialDirectory);
                  final RpcDialog dialog = dialogOrNull;
                  closeDialogOnError(onMessage, dialog, paymentCodeCounterParty);
                  dialog(
                      account,
                      cahootsContext,
                      messageService,
                      dialog,
                      paymentCodeCounterParty,
                      timeoutMs,
                      message,
                      "INITIATOR",
                      onMessage);
                  onMessage.onComplete();
                } catch (Exception e) {
                  log.error("INITIATOR => error", e);
                  fail(e.getMessage(), onMessage, dialogOrNull, paymentCodeCounterParty);
                }
              }
            });
    t.setName("soroban-initiator");
    t.start();
    return onMessage;
  }

  public Observable<SorobanMessage> contributor(
      final int account,
      final CahootsContext cahootsContext,
      final SorobanMessageService messageService,
      final PaymentCode paymentCodeInitiator,
      final long timeoutMs)
      throws Exception {
    reset();
    final BehaviorSubject onMessage = BehaviorSubject.create();
    Thread t =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                RpcDialog dialogOrNull = null;
                try {
                  String initialDirectory =
                      user.getMeeetingAddressReceive(paymentCodeInitiator, params)
                          .getBech32AsString();

                  dialogOrNull = new RpcDialog(rpc, user, initialDirectory);
                  final RpcDialog dialog = dialogOrNull;
                  closeDialogOnError(onMessage, dialog, paymentCodeInitiator);

                  String info = "CONTRIBUTOR";
                  SorobanMessage message =
                      receive(messageService, dialog, paymentCodeInitiator, timeoutMs)
                          .blockingSingle();
                  onMessage.onNext(message);
                  if (log.isDebugEnabled()) {
                    log.debug(info + " #(0) <= " + message.toString());
                  }
                  if (message.isDone()) {
                    if (log.isDebugEnabled()) {
                      log.debug(info + " #(0) done.");
                    }
                    onMessage.onComplete();
                    return;
                  }

                  SorobanMessage response =
                      (SorobanMessage)
                          safeReply(
                              messageService,
                              account,
                              cahootsContext,
                              message,
                              dialog,
                              paymentCodeInitiator);
                  dialog(
                      account,
                      cahootsContext,
                      messageService,
                      dialog,
                      paymentCodeInitiator,
                      timeoutMs,
                      response,
                      "CONTRIBUTOR",
                      onMessage);
                  onMessage.onComplete();
                } catch (Exception e) {
                  log.error("CONTRIBUTOR => error ", e);
                  fail(e.getMessage(), onMessage, dialogOrNull, paymentCodeInitiator);
                }
              }
            });
    t.setName("soroban-contributor");
    t.start();
    return onMessage;
  }

  private SorobanReply safeReply(
      SorobanMessageService messageService,
      int account,
      CahootsContext cahootsContext,
      SorobanMessage message,
      RpcDialog dialog,
      PaymentCode paymentCodePartner)
      throws Exception {
    try {
      return messageService.reply(account, cahootsContext, message);
    } catch (Exception e) {
      // send error
      dialog.sendError("Dialog failed", paymentCodePartner).subscribe();
      log.error("Dialog failed", e);
      throw e;
    }
  }

  private SorobanMessage dialog(
      int account,
      CahootsContext cahootsContext,
      SorobanMessageService messageService,
      RpcDialog dialog,
      PaymentCode paymentCodePartner,
      long timeoutMs,
      SorobanMessage message,
      String info,
      Subject<SorobanMessage> onMessage)
      throws Exception {
    int i = 0;
    while (true) {
      // send first message
      if (log.isDebugEnabled()) {
        log.debug(info + " #" + i + " => " + message.toString());
      }
      if (onMessage != null) {
        onMessage.onNext(message);
      }
      dialog.send(message, paymentCodePartner).blockingSingle();

      if (message.isDone()) {
        // done
        if (log.isDebugEnabled()) {
          log.debug(info + " #" + i + " done.");
        }
        break;
      }

      // receive response
      message = receive(messageService, dialog, paymentCodePartner, timeoutMs).blockingSingle();
      if (onMessage != null) {
        onMessage.onNext(message);
      }
      if (log.isDebugEnabled()) {
        log.debug(info + " #" + i + " <= " + message.toString());
      }

      if (message.isDone()) {
        // done
        if (log.isDebugEnabled()) {
          log.debug(info + " #" + i + " done.");
        }
        break;
      }

      // prepare reply
      SorobanReply reply =
          safeReply(messageService, account, cahootsContext, message, dialog, paymentCodePartner);

      if (reply instanceof SorobanInteraction) {
        // wrap interaction for Soroban
        OnlineSorobanInteraction interaction = computeOnlineInteraction((SorobanInteraction) reply);
        if (log.isDebugEnabled()) {
          log.debug(info + " #" + i + " => [INTERACTIVE] ... >? " + interaction);
        }
        onInteraction.onNext(interaction);

        // wait for interaction confirmation
        message = interactiveMessageProvider.blockingNext().iterator().next();
        if (log.isDebugEnabled()) {
          log.debug(info + " #" + i + " => [INTERACTIVE] " + message.toString());
        }
      } else {
        // direct reply
        message = (SorobanMessage) reply;
      }
      i++;
    }
    return message;
  }

  private OnlineSorobanInteraction computeOnlineInteraction(final SorobanInteraction interaction) {
    OnlineSorobanInteraction onlineInteraction = new OnlineSorobanInteraction(interaction, this);
    return onlineInteraction;
  }

  private Observable<SorobanMessage> receive(
      final SorobanMessageService messageService,
      RpcDialog dialog,
      PaymentCode paymentCodePartner,
      long timeoutMs)
      throws Exception {
    return dialog
        .receive(paymentCodePartner, timeoutMs)
        .map(
            new Function<String, SorobanMessage>() {
              @Override
              public SorobanMessage apply(String payload) throws Exception {
                SorobanMessage response = messageService.parse(payload);
                return response;
              }
            });
  }

  void replyInteractive(SorobanMessage message) {
    if (log.isDebugEnabled()) {
      log.debug(" => replyInteractive");
    }
    interactiveMessageProvider.onNext(message);
  }

  void replyInteractive(Exception e) {
    if (log.isDebugEnabled()) {
      log.debug(" => replyInteractive reject: " + e.getMessage());
    }
    interactiveMessageProvider.onError(e);
    interactiveMessageProvider.onComplete();
  }

  public Observable<OnlineSorobanInteraction> getOnInteraction() {
    return skipNull(onInteraction);
  }

  private void reset() {
    interactiveMessageProvider = BehaviorSubject.create();
    onInteraction.onNext(SUBJECT_RESET);
  }

  private <T> Observable<T> skipNull(Subject<T> subject) {
    return subject.filter(
        new Predicate<Object>() {
          @Override
          public boolean test(Object o) {
            return o != SUBJECT_RESET;
          }
        });
  }

  private void closeDialogOnError(
      final Subject onMessage, final RpcDialog dialog, final PaymentCode paymentCodePartner) {
    onMessage.doOnDispose(
        new Action() {
          @Override
          public void run() throws Exception {
            fail("Canceled by user", onMessage, dialog, paymentCodePartner);
          }
        });
    onMessage.doOnError(
        new Consumer<Throwable>() {
          @Override
          public void accept(Throwable throwable) throws Exception {
            fail(throwable.getMessage(), onMessage, dialog, paymentCodePartner);
          }
        });
    interactiveMessageProvider.doOnError(
        new Consumer<Throwable>() {
          @Override
          public void accept(Throwable throwable) throws Exception {
            fail(throwable.getMessage(), onMessage, dialog, paymentCodePartner);
          }
        });
  }

  private void fail(
      String error, Subject onMessage, RpcDialog dialog, PaymentCode paymentCodePartner) {
    if (dialog != null) {
      dialog.sendError(error, paymentCodePartner).subscribe();
      dialog.close();
    }

    onMessage.onError(new Exception(error));
    onMessage.onComplete();
  }
}
