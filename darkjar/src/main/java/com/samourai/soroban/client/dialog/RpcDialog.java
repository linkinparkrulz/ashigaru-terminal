package com.samourai.soroban.client.dialog;

import com.google.common.base.Charsets;
import com.samourai.soroban.client.SorobanMessage;
import com.samourai.soroban.client.meeting.SorobanMessageWithSender;
import com.samourai.soroban.client.rpc.RpcClient;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.Z85;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.subjects.BehaviorSubject;
import java.security.MessageDigest;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcDialog {
  private static final Logger log = LoggerFactory.getLogger(RpcDialog.class);
  private static final Z85 z85 = Z85.getInstance();
  private static final String ERROR_PREFIX = "ERROR:";

  private RpcClient rpc;
  private User user;
  private String nextDirectory;
  private boolean exit;

  public RpcDialog(RpcClient rpc, User user, String directory) throws Exception {
    this.rpc = rpc;
    this.user = user;
    setNextDirectory(directory);
    this.exit = false;
  }

  public RpcDialog(RpcClient rpc, User user, SorobanMessage sorobanMessage) throws Exception {
    this(rpc, user, sorobanMessage.toPayload());
  }

  public Observable<SorobanMessageWithSender> receiveWithSender(long timeoutMs) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("watching[withSender]: " + nextDirectory);
    }
    return doReceive(timeoutMs)
        .map(
            new Function<String, SorobanMessageWithSender>() {
              @Override
              public SorobanMessageWithSender apply(String payloadWithSender) throws Exception {
                SorobanMessageWithSender messageWithSender =
                    SorobanMessageWithSender.parse(payloadWithSender);
                String encryptedPayload = messageWithSender.getPayload();
                String sender = messageWithSender.getSender();
                PaymentCode paymentCodePartner = new PaymentCode(sender);

                // decrypt
                String payload = decrypt(encryptedPayload, paymentCodePartner);
                if (log.isDebugEnabled()) {
                  log.debug("(" + nextDirectory + ") <= " + payload);
                }

                // return clear object
                return new SorobanMessageWithSender(sender, payload);
              }
            });
  }

  public Observable<String> receive(final PaymentCode paymentCodePartner, long timeoutMs)
      throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("watching: " + nextDirectory);
    }
    return doReceive(timeoutMs)
        .map(
            new Function<String, String>() {
              @Override
              public String apply(String payload) throws Exception {
                // decrypt
                String decryptedPayload = decrypt(payload, paymentCodePartner);
                if (log.isDebugEnabled()) {
                  log.debug("(" + nextDirectory + ") <= " + payload);
                }

                // check for error
                String error = getError(decryptedPayload);
                if (error != null) {
                  throw new SorobanException(error);
                }
                return decryptedPayload;
              }
            });
  }

  private Observable<String> doReceive(long timeoutMs) throws Exception {
    if (exit) {
      throw new Exception("Canceled by user");
    }
    if (log.isDebugEnabled()) {
      log.debug("watching: " + nextDirectory);
    }
    return rpc.waitAndRemove(nextDirectory, timeoutMs)
        .map(
            new Function<String, String>() {
              @Override
              public String apply(String payload) throws Exception {
                setNextDirectory(payload);
                return payload;
              }
            });
  }

  public Observable sendWithSender(SorobanMessage message, PaymentCode paymentCodePartner)
      throws Exception {
    checkExit(paymentCodePartner);

    String payload = message.toPayload();
    if (log.isDebugEnabled()) {
      log.debug("(" + nextDirectory + ") => [withSender] " + payload);
    }

    // encrypt
    String encryptedPayload = encrypt(message.toPayload(), paymentCodePartner);

    // wrap with clear sender
    SorobanMessageWithSender messageWithSender =
        new SorobanMessageWithSender(user.getPaymentCode().toString(), encryptedPayload);
    return doSend(messageWithSender.toPayload());
  }

  public Observable send(SorobanMessage message, PaymentCode paymentCodePartner) throws Exception {
    return send(message.toPayload(), paymentCodePartner);
  }

  private Observable send(String payload, PaymentCode paymentCodePartner) throws Exception {
    checkExit(paymentCodePartner);
    if (log.isDebugEnabled()) {
      log.debug("(" + nextDirectory + ") => " + payload);
    }

    // encrypt
    String encryptedPayload = encrypt(payload, paymentCodePartner);
    return doSend(encryptedPayload);
  }

  protected Observable doSend(final String payload) throws Exception {
    return rpc.directoryAdd(nextDirectory, payload, "normal")
        .map(
            new Function() {
              @Override
              public Object apply(Object o) throws Exception {
                setNextDirectory(payload);
                return o;
              }
            });
  }

  private void checkExit(PaymentCode paymentCodePartner) throws Exception {
    if (exit) {
      sendError("Canceled by user", paymentCodePartner).subscribe();
      throw new SorobanException("Canceled by user");
    }
  }

  public Observable sendError(String message, PaymentCode paymentCodePartner) {
    // send error
    try {
      return send(ERROR_PREFIX + message, paymentCodePartner);
    } catch (Exception e) {
      // non-blocking error
      log.error("", e);
      BehaviorSubject s = BehaviorSubject.create();
      s.onNext("error");
      s.onComplete();
      return s;
    }
  }

  private static String encodeDirectory(String payload) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(payload.getBytes(Charsets.UTF_8));
    String result = Hex.toHexString(hash);
    if (result.isEmpty()) {
      throw new Exception("Invalid encodeDirectory value");
    }
    return result;
  }

  private void setNextDirectory(String nextDirectory) throws Exception {
    this.nextDirectory = encodeDirectory(nextDirectory);
    if (log.isDebugEnabled()) {
      log.debug("nextDirectory: " + this.nextDirectory);
    }
  }

  private String encrypt(String payload, PaymentCode paymentCodePartner) throws Exception {
    Encrypter encrypter = user.getEncrypter(paymentCodePartner);
    String encryptedPayload = z85.encode(encrypter.encrypt(payload));
    return encryptedPayload;
  }

  private String decrypt(String encryptedPayload, PaymentCode paymentCodePartner) throws Exception {
    Encrypter encrypter = user.getEncrypter(paymentCodePartner);
    return encrypter.decrypt(z85.decode(encryptedPayload));
  }

  private String getError(String payload) {
    if (payload != null && payload.startsWith(ERROR_PREFIX)) {
      return payload.substring(ERROR_PREFIX.length());
    }
    return null;
  }

  public void close() {
    exit = true;
  }
}
