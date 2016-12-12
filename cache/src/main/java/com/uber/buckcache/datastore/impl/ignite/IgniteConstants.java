package com.uber.buckcache.datastore.impl.ignite;

import org.apache.ignite.events.EventType;

import java.util.HashMap;
import java.util.Map;

class IgniteConstants {

  static final String KEYS_CACHE_NAME = "keys";
  static final String KEYS_REVERSE_CACHE_NAME = "keys-reverse";
  static final String METADATA_CACHE_NAME = "metadata";
  static final String ARTIFACT_CACHE_NAME = "artifacts";
  static final String UNDERLYING_KEY_SEQUENCE_NAME = "underlyingArtifactKeys";

  static final Map<Integer, String> EVENT_TYPE_TO_NAME_MAP = new HashMap<Integer, String>() {
    {
      put(EventType.EVT_CACHE_OBJECT_EXPIRED, "EXPIRED");
      put(EventType.EVT_CACHE_OBJECT_REMOVED, "REMOVED");
      put(EventType.EVT_CACHE_ENTRY_EVICTED, "EVICTED");
    }
  };
}
