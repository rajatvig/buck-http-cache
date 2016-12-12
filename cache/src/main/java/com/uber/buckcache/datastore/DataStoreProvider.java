package com.uber.buckcache.datastore;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.health.HealthCheck;
import com.uber.buckcache.CacheInstanceMode;
import io.dropwizard.lifecycle.Managed;

public interface DataStoreProvider extends Managed {

  /**
   * CacheProviderConfig : passes all the configs that are provider in the config.yml file
   * CacheInstanceMode : this tells the cache provider whether to only come up as
   * cacheClient or server or both.
   * @param cacheProviderConfig
   */
  void init(DataStoreProviderConfig cacheProviderConfig, CacheInstanceMode mode) throws Exception;

  /**
   * returns cache entry associated with the key
   * @param key
   * @return
   */
  CacheEntry getData(String key) throws Exception;

  /**
   * @param keys : all the keys that associated with this given Cache Entry
   * @param cacheData : data to store/cache.
   */
  void putData(String[] keys, CacheEntry cacheData) throws Exception;

  /**
   * @param keys : all the keys that associated with this given Cache Entry
   * @param cacheData : data to store/cache
   * @param expirationTimeUnit : the time unit of expiration for the data
   * @param  expirationTimeValue : the time value of expiration for the data
   */
  void putData(String[] keys, CacheEntry cacheData, TimeUnit expirationTimeUnit, Long expirationTimeValue) throws Exception;

  /**
   * return the current key count of the cache
   * @return
   */
  int getNumberOfKeys() throws Exception;

  /**
   * return the current value count of the cache
   * @return
   */
  int getNumberOfValues() throws Exception;

  /**
   * this is to monitor the health of the data store.
   * @return
   * @throws Exception
   */
  HealthCheck.Result check() throws Exception;

}
