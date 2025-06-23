package com.zeroleak.throwingsupplier;

public abstract class LastValueFallbackSupplier<T, E extends Exception>
    extends ThrowingSupplier<T, E> {
  private Throwing<T, E> lastValue; // last non-throwing value

  @Override
  public Throwing<T, E> get() {
    Throwing<T, E> value = super.get();
    if (value.getValue().isPresent()) {
      // value found => update lastValue
      this.lastValue = value;
      return value;
    } else {
      // value not found => return lastValue
      if (this.lastValue != null) {
        return lastValue;
      }
      // no lastValue => return throwing value
      return value;
    }
  }
}
