package s3fcache;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Saalik Hatia
 * @version 0.1
 **/
public interface Cache<K, V> {
    V get(K key);

    void put(K key, V value);

    void evict();

    ConcurrentMap<K, CacheEntry<K, V>> asMap();

    void invalidateAll();

    void invalidateAll(Iterable<K> keys);

    void putAll(Map<? extends K, ? extends V> map);

    long size();

    Set<K> getKeys();

    HashSet<K> getGhostKeySet();
}
