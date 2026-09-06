package com.sparrowwallet.sparrow.net.update;

/** One downloadable file attached to a GitHub release. */
public record ReleaseAsset(String name, String url, long size) {}
