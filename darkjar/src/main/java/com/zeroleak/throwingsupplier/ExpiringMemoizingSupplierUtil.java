package com.zeroleak.throwingsupplier;

import com.google.common.base.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

public class ExpiringMemoizingSupplierUtil {
    private static final Logger log = LoggerFactory.getLogger(ExpiringMemoizingSupplierUtil.class);

  /** Force a ExpiringMemoizingSupplier to expire now. */
  public static void expire(Supplier supplier) {
      try {
          Class<?> expiringMemoizingSupplierClass = Class.forName("com.google.common.base.Suppliers$ExpiringMemoizingSupplier");
          Field expirationNanosField = expiringMemoizingSupplierClass.getDeclaredField("expirationNanos");
          expirationNanosField.setAccessible(true);
          expirationNanosField.set(supplier, 0);
      } catch(Exception e) {
          log.error("Cannot expire supplier", e);
      }
  }
}
