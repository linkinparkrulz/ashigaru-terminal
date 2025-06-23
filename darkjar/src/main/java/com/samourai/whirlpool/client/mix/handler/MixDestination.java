package com.samourai.whirlpool.client.mix.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MixDestination {
  private static final Logger log = LoggerFactory.getLogger(MixDestination.class);

  private DestinationType type;
  private int index;
  private String address;
  private String path;

  public MixDestination(DestinationType type, int index, String address, String path) {
    this.type = type;
    this.index = index;
    this.address = address;
    this.path = path;
  }

  public DestinationType getType() {
    return type;
  }

  public int getIndex() {
    return index;
  }

  public String getAddress() {
    return address;
  }

  public String getPath() {
    return path;
  }
}
