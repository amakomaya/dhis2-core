/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.cache;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public interface Cache<V> {
  /**
   * Returns the value associated with the {@code key} in this cache instance, or {@code
   * Optional.empty()} if there is no cached value.Note: This method will NOT return the
   * defaultValue in case of absence of associated cache value.
   *
   * @param key the key whose associated value is to be retrieved
   * @return the value wrapped in Optional, or {@code Optional.empty()}
   */
  Optional<V> getIfPresent(String key);

  /**
   * Returns the value associated with the {@code key} in this cache instance. Note: This method
   * will return the defaultValue in case of absence of associated cache value, but will not store
   * the default value into the cache.
   *
   * @param key the key whose associated value is to be retrieved
   * @return the value wrapped in Optional, or {@code Optional of defaultValue}
   */
  Optional<V> get(String key);

  /**
   * Returns the value mapped to {@code key} in this cache instance, obtaining that value from the
   * {@code mappingFunction} if necessary. This method provides a simple substitute for the
   * conventional "if cached, return; otherwise create, cache and return" pattern. If value is null,
   * the given mapping function is evaluated and inserted into this cache unless {@code null}. Note:
   * This method will return the defaultValue in case of absence of associated cache value. But will
   * not store the default value into the cache.
   *
   * @param key the key for retrieving the value
   * @param mappingFunction the function to compute a value.
   * @return an optional containing current (existing or computed) value, or Optional.empty() if the
   *     computed value is null
   * @throws IllegalArgumentException if the specified mappingFunction is null
   */
  V get(String key, Function<String, V> mappingFunction);

  /**
   * Returns a collection of all the values in the cache
   *
   * @return collection with all cached values
   */
  Stream<V> getAll();

  /**
   * Should only be used with caution in cases where the set of keys is known to be a small set.
   *
   * @return an unmodifiable set of all keys set
   */
  Iterable<String> keys();

  /**
   * Associates the {@code value} with the {@code key} in this cache. If the cache previously
   * contained a value associated with the {@code key}, the old value is replaced by the new {@code
   * value}. Prefer {@link #get(String, Function)} when using the conventional "if cached, return;
   * otherwise create, cache and return" pattern.
   *
   * @param key the key for the value
   * @param value value to be mapped to the key
   * @throws IllegalArgumentException if the specified value is null
   */
  void put(String key, V value);

  /**
   * Associates the {@code value} with the {@code key} in this cache. If the cache previously
   * contained a value associated with the {@code key}, the old value is replaced by the new {@code
   * value}. It also sets a custom time to live for the given key, which overrides the cache's
   * default.
   *
   * @param key the key for the value
   * @param value value to be mapped to the key
   * @param ttlInSeconds the time to live for the key, in seconds
   * @throws IllegalArgumentException if the specified value is null
   */
  void put(String key, V value, long ttlInSeconds);

  /**
   * Associates the {@code value} with the {@code key} in this cache if and only of the cache does
   * not already contain a value for the key.
   *
   * @param key the key for the value
   * @param value value to be mapped to the key
   * @return true, if the value was put, false otherwise
   * @throws IllegalArgumentException if the specified value is null
   */
  boolean putIfAbsent(String key, V value);

  /**
   * Discards any cached value for the {@code key}. The behavior of this operation is undefined for
   * an entry that is being loaded and is otherwise not present.
   *
   * @param key the key whose mapping is to be removed from the cache
   */
  void invalidate(String key);

  /** Discards all entries in this cache instance. */
  void invalidateAll();

  /**
   * Returns the type of the cache. IN_MEMORY or REDIS or NONE.
   *
   * @return
   */
  CacheType getCacheType();
}
