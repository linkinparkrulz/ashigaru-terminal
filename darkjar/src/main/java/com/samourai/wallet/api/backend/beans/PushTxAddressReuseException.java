package com.samourai.wallet.api.backend.beans;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class PushTxAddressReuseException extends Exception {
  private List<Integer> adressReuseOutputIndexs;

  public PushTxAddressReuseException(Collection<Integer> adressReuseOutputIndexs) {
    super("Address reuse detected for outputs: "+ Arrays.toString(adressReuseOutputIndexs.toArray()));
    this.adressReuseOutputIndexs = new LinkedList<>(adressReuseOutputIndexs);
  }

  public List<Integer> getAdressReuseOutputIndexs() {
    return adressReuseOutputIndexs;
  }
}
