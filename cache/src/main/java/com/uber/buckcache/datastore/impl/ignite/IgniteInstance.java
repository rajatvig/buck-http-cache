package com.uber.buckcache.datastore.impl.ignite;

import static com.uber.buckcache.datastore.impl.ignite.IgniteConstants.KEYS_CACHE_NAME;
import static com.uber.buckcache.datastore.impl.ignite.IgniteConstants.KEYS_REVERSE_CACHE_NAME;
import static com.uber.buckcache.datastore.impl.ignite.IgniteConstants.METADATA_CACHE_NAME;
import static com.uber.buckcache.datastore.impl.ignite.IgniteConstants.UNDERLYING_KEY_SEQUENCE_NAME;
import static com.uber.buckcache.utils.MetricsRegistry.CPU_COUNT;
import static com.uber.buckcache.utils.MetricsRegistry.CPU_TIME;
import static com.uber.buckcache.utils.MetricsRegistry.HEAP_COUNT;
import static com.uber.buckcache.utils.MetricsRegistry.OFF_HEAP_COUNT;
import static com.uber.buckcache.utils.MetricsRegistry.OFF_HEAP_TIME;

import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import javax.cache.expiry.ExpiryPolicy;

import com.uber.buckcache.CacheInstanceMode;
import com.uber.buckcache.IgniteConfig;
import com.uber.buckcache.utils.StatsDClient;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteAtomicSequence;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterMetrics;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.events.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class IgniteInstance {
  private static final Logger logger = LoggerFactory.getLogger(IgniteInstance.class);
  private static final int TEN_SECONDS = 10 * 1000;
  private final Ignite ignite;

  private final IgniteAtomicSequence atomicSequence;
  private final IgniteCache<String, Long> cacheKeys;
  private final IgniteCache<Long, String[]> reverseCacheKeys;
  private final IgniteCache<Long, byte[]> buckDataCache;
  private final Timer timer = new Timer("ignite_metrics_reporter");

  IgniteInstance(CacheInstanceMode mode, IgniteConfig config) {
    IgniteConfiguration igniteConfiguration = new IgniteConfigurationBuilder()
        .addMulticastBasedDiscovery(config.getMulticastIP(), config.getMulticastPort(),
            config.getHostIPs(), config.getDnsLookupAddress())
        .addCacheConfiguration(config.getCacheMode(), config.getCacheBackupCount(),
            config.getExpirationTimeUnit(), config.getExpirationTimeValue(),
            config.getOffHeapStorageSize(), KEYS_CACHE_NAME, KEYS_REVERSE_CACHE_NAME, METADATA_CACHE_NAME)
        .addAtomicSequenceConfig(config.getAtomicSequencereserveSize()).build();

    logger.info("isClientMode : {}", mode == CacheInstanceMode.CLIENT);
    Ignition.setClientMode(mode == CacheInstanceMode.CLIENT);
    ignite = Ignition.start(igniteConfiguration);

    cacheKeys = ignite.cluster().ignite().getOrCreateCache(KEYS_CACHE_NAME);
    reverseCacheKeys = ignite.cluster().ignite().getOrCreateCache(KEYS_REVERSE_CACHE_NAME);
    buckDataCache = ignite.cluster().ignite().getOrCreateCache(METADATA_CACHE_NAME);
    atomicSequence = ignite.cluster().ignite().atomicSequence(UNDERLYING_KEY_SEQUENCE_NAME, 0, true);

    ignite.events().localListen(new LocalCacheEventListener(buckDataCache, reverseCacheKeys, cacheKeys),
        EventType.EVT_CACHE_OBJECT_EXPIRED, EventType.EVT_CACHE_OBJECT_REMOVED);
  }

  void start() {
    timer.scheduleAtFixedRate(new TimerTask() {

      @Override
      public void run() {
        reportMetrics();
      }
    }, TEN_SECONDS, TEN_SECONDS);
  }

  void stop() {
    timer.cancel();
    Ignition.stop(ignite.name(), false);
  }

  IgniteCache<String, Long> getCacheKeys() {
    return cacheKeys;
  }

  IgniteCache<Long, byte[]> getBuckDataCache() {
    return buckDataCache;
  }

  IgniteCache<String, Long> getCacheKeys(Optional<ExpiryPolicy> policy) {
    return policy.map(cacheKeys::withExpiryPolicy).orElse(cacheKeys);
  }

  IgniteCache<Long, String[]> getReverseCacheKeys(Optional<ExpiryPolicy> policy) {
    return policy.map(reverseCacheKeys::withExpiryPolicy).orElse(reverseCacheKeys);
  }

  IgniteCache<Long, byte[]> getBuckDataCache(Optional<ExpiryPolicy> policy) {
    return policy.map(buckDataCache::withExpiryPolicy).orElse(buckDataCache);
  }

  IgniteAtomicSequence getAtomicSequence() {
    return atomicSequence;
  }

  private void reportMetrics() {
    // TODO: check and make sure that these are not costly to compute
    ClusterNode thisNode = ignite.cluster().localNode();
    ClusterMetrics metrics = thisNode.metrics();

    Double cpuLoad = metrics.getCurrentCpuLoad();
    long heapUsage = metrics.getHeapMemoryUsed();
    long offHeapUsage = metrics.getNonHeapMemoryUsed();

    StatsDClient.get().count(CPU_COUNT, cpuLoad.longValue());
    StatsDClient.get().count(CPU_TIME, cpuLoad.longValue());

    StatsDClient.get().count(HEAP_COUNT, heapUsage);
    StatsDClient.get().count(HEAP_COUNT, heapUsage);

    StatsDClient.get().count(OFF_HEAP_COUNT, offHeapUsage);
    StatsDClient.get().count(OFF_HEAP_TIME, offHeapUsage);
  }
}
