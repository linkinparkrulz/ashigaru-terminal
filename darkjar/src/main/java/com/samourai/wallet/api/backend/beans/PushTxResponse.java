package com.samourai.wallet.api.backend.beans;

public class PushTxResponse {
  public PushTxStatus status;
  public String data;
  public PushTxError error;

  public PushTxResponse() {}

  public enum PushTxStatus {
    ok, error
  }

  public static class PushTxError {
    public static final String CODE_VIOLATION_STRICT_MODE_VOUTS = "VIOLATION_STRICT_MODE_VOUTS";

    public Object message;
    public String code;

    @Override
    public String toString() {
      return "PushTxError{" +
              "message=" + message +
              ", code='" + code + '\'' +
              '}';
    }
  }

  @Override
  public String toString() {
    return "PushTxResponse{" +
            "status=" + status +
            ", data='" + data + '\'' +
            ", error=" + error +
            '}';
  }
}

