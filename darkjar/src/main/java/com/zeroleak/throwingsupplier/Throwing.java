package com.zeroleak.throwingsupplier;

import java8.util.Optional;
import java8.util.function.Supplier;

public class Throwing<T, E extends Exception> extends Either<T, E> {

  public Throwing(T value) {
    super(value);
  }

  public Throwing(Exception e) {
    super((Optional<T>) Optional.empty(), (E) e);
  }

  public T getOrThrow() throws Exception {
    return getValue()
        .orElseThrow(
            new Supplier<Exception>() {
              @Override
              public Exception get() {
                return getFallback().get();
              }
            });
  }
}
