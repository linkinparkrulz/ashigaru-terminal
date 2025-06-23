package com.samourai.whirlpool.client.wallet.beans;

import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.listener.MixStep;
import com.samourai.whirlpool.client.utils.ClientUtils;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public class WhirlpoolUtxoState {
  private String poolId;
  private WhirlpoolUtxoStatus status;
  private MixProgress mixProgress;
  private MixableStatus mixableStatus;

  private String message;
  private String error;

  private Long lastActivity;
  private Long lastError;
  private Subject<WhirlpoolUtxoState> observable;

  public WhirlpoolUtxoState(String poolId) {
    this.poolId = poolId;
    this.status = WhirlpoolUtxoStatus.READY;
    this.mixProgress = null;
    this.mixableStatus = null;

    this.message = null;
    this.error = null;

    this.lastActivity = null;
    this.lastError = null;
    this.observable = BehaviorSubject.create();
  }

  private void emit() {
    // notify observers
    observable.onNext(this);
  }

  public String getPoolId() {
    return poolId;
  }

  public WhirlpoolUtxoStatus getStatus() {
    return status;
  }

  protected void setStatus(
      WhirlpoolUtxoStatus status,
      boolean updateLastActivity,
      MixProgress mixProgress,
      String error,
      boolean updateLastError) {
    this.status = status;
    this.mixProgress = mixProgress;
    if (mixProgress != null) {
      String message = null;
      MixStep mixStep = mixProgress.getMixStep();
      if (mixStep != MixStep.SUCCESS) this.message = message;
    }
    this.error = error;
    if (updateLastError) {
      setLastError();
    }
    if (updateLastActivity) {
      setLastActivity();
    }
    emit();
  }

  public void setStatusMixing(
      WhirlpoolUtxoStatus status,
      boolean updateLastActivity,
      MixParams mixParams,
      MixStep mixStep) {
    setStatus(status, updateLastActivity, new MixProgress(mixParams, mixStep), null, false);
  }

  public void setStatusError(WhirlpoolUtxoStatus status, String error) {
    setStatus(status, true, null, error, true);
  }

  public void setStatusMixingError(WhirlpoolUtxoStatus status, MixParams mixParams, String error) {
    setStatus(status, true, new MixProgress(mixParams, MixStep.FAIL), error, true);
  }

  public void setStatus(
      WhirlpoolUtxoStatus status, boolean updateLastActivity, boolean clearError) {
    setStatus(status, updateLastActivity, null, clearError ? null : error, false);
  }

  public MixProgress getMixProgress() {
    return mixProgress;
  }

  public MixableStatus getMixableStatus() {
    return mixableStatus;
  }

  public void setMixableStatus(MixableStatus mixableStatus) {
    this.mixableStatus = mixableStatus;
  }

  public boolean hasMessage() {
    return message != null;
  }

  public String getMessage() {
    return message;
  }

  public boolean hasError() {
    return error != null;
  }

  public String getError() {
    return error;
  }

  public Long getLastActivity() {
    return lastActivity;
  }

  public void setLastActivity() {
    this.lastActivity = System.currentTimeMillis();
  }

  public void setLastError() {
    this.lastError = System.currentTimeMillis();
  }

  public Long getLastError() {
    return lastError;
  }

  public void setLastError(Long lastError) {
    this.lastError = lastError;
  }

  public Observable<WhirlpoolUtxoState> getObservable() {
    return observable;
  }

  @Override
  public String toString() {
    return "poolId="
        + (poolId != null ? poolId : "null")
        + ", status="
        + status
        + (mixProgress != null ? "(" + mixProgress + ")" : "")
        + ", mixableStatus="
        + (mixableStatus != null ? mixableStatus : "null")
        + (hasMessage() ? ", message=" + message : "")
        + (hasError() ? ", error=" + error : "")
        + (lastActivity != null ? ", lastActivity=" + ClientUtils.dateToString(lastActivity) : "")
        + (lastError != null ? ", lastError=" + ClientUtils.dateToString(lastError) : "");
  }
}
