package com.uber.buckcache.datastore.impl.ignite;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.cache.expiry.Duration;
import javax.cache.expiry.TouchedExpiryPolicy;

import com.google.common.base.Strings;
import com.spotify.dns.DnsSrvResolver;
import com.spotify.dns.DnsSrvResolvers;
import com.spotify.dns.LookupResult;
import com.uber.buckcache.utils.BytesRateLimiter.BIT_UNIT;
import org.apache.commons.lang3.StringUtils;
import org.apache.ignite.cache.CacheMemoryMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.AtomicConfiguration;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

class IgniteConfigurationBuilder {

  private final IgniteConfiguration igniteConfiguration;
  private final DnsSrvResolver dnsResolver;

  IgniteConfigurationBuilder() {
    this(DnsSrvResolvers.newBuilder()
        .retainingDataOnFailures(true)
        .build());
  }

   IgniteConfigurationBuilder(DnsSrvResolver dnsResolver) {
    this.dnsResolver = dnsResolver;
    igniteConfiguration = new IgniteConfiguration();
  }

  IgniteConfigurationBuilder addMulticastBasedDiscovery(String multicastIP, Integer multicastPort, List<String> hostIPs, String dnsLookupAddress) {
    if (hostIPs == null) {
      hostIPs = new ArrayList<>();
    }

    TcpDiscoverySpi spi = new TcpDiscoverySpi();

    TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryMulticastIpFinder();
    ((TcpDiscoveryMulticastIpFinder) ipFinder).setMulticastGroup(multicastIP);
    ((TcpDiscoveryMulticastIpFinder) ipFinder).setMulticastPort(multicastPort);

    boolean hasDNSLookupAddress = !Strings.isNullOrEmpty(dnsLookupAddress);

    if (hasDNSLookupAddress) {
      hostIPs.addAll(this.resolveAddressByDNS(dnsLookupAddress));
    }
    ipFinder.setAddresses(hostIPs);
    spi.setIpFinder(ipFinder);

    // Override default discovery SPI.
    igniteConfiguration.setDiscoverySpi(spi);
    return this;
  }

  IgniteConfigurationBuilder addCacheConfiguration(CacheMode cacheMode, Integer backupCount,
      TimeUnit expirationTimeUnit, Long expirationTimeValue, String offHeapMaxSize, String ...caches) {

    CacheConfiguration[] cacheConfigs = new CacheConfiguration[caches.length];

    for (int i = 0; i < caches.length; i++) {
      CacheConfiguration cacheConfiguration = new CacheConfiguration(caches[i]);
      cacheConfiguration.setCacheMode(cacheMode);
      cacheConfiguration.setMemoryMode(CacheMemoryMode.OFFHEAP_VALUES);
      cacheConfiguration.setStatisticsEnabled(true);

      String multiplier = offHeapMaxSize.substring(0, offHeapMaxSize.length() - 1);
      String unit = offHeapMaxSize.substring(offHeapMaxSize.length() - 1, offHeapMaxSize.length());

      long offHeapMemoryMax =
          Long.parseLong(multiplier) * BIT_UNIT.valueOf(StringUtils.lowerCase(unit)).getNumberOfBytes();
      cacheConfiguration.setOffHeapMaxMemory(offHeapMemoryMax);

      cacheConfiguration.setBackups(backupCount);
      cacheConfiguration
          .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(expirationTimeUnit, expirationTimeValue)));
      cacheConfigs[i] = cacheConfiguration;
    }

    igniteConfiguration.setCacheConfiguration(cacheConfigs);
    return this;
  }

  IgniteConfigurationBuilder addAtomicSequenceConfig(Integer reserveSize) {
    AtomicConfiguration atomicCfg = new AtomicConfiguration();
    atomicCfg.setAtomicSequenceReserveSize(reserveSize);
    igniteConfiguration.setAtomicConfiguration(atomicCfg);
    return this;
  }

  IgniteConfiguration build() {
    return igniteConfiguration;
  }

  private List<String> resolveAddressByDNS(String dnsLookupAddress) {
     List<String> address = new ArrayList<>();
     for (LookupResult node : dnsResolver.resolve(dnsLookupAddress)) {
       address.add(node.host());
     }
     return address;
  }
}
