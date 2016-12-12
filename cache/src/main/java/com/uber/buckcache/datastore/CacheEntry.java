package com.uber.buckcache.datastore;

import javax.annotation.Nonnull;

/**
 * An entry in the build cache.
 */
public class CacheEntry {
  @Nonnull
  byte[] buckData;
  @Nonnull
  private final int bytes;

  public CacheEntry(byte[] buckData) {
    this.buckData = buckData;
    this.bytes = this.buckData.length;
  }

  public byte[] getBuckData() {
    return buckData;
  }

  public void setBuckData(byte[] buckData) {
    this.buckData = buckData;
  }

  public int getBytes() {
    return bytes;
  }

}
