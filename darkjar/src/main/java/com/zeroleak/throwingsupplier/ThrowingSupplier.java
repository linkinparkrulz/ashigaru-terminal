package com.zeroleak.throwingsupplier;

import com.google.common.base.Supplier;

public abstract class ThrowingSupplier<T, E extends Exception> implements Supplier<Throwing<T, E>> {
  private int attempts = 1;

  public abstract T getOrThrow() throws Exception;

  @Override
  public Throwing<T, E> get() {
    Exception lastError = null;
    for (int i = 0; i < attempts; i++) {
      try {
        T value = getOrThrow();
        return new Throwing<T, E>(value);
      } catch (Exception e) {
        lastError = e;
      }
    }
    return new Throwing(lastError);
  }

  public ThrowingSupplier<T, E> attempts(int attempts) {
    this.attempts = attempts;
    return this;
  }
}
