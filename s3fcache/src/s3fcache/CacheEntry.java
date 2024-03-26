/**
 * @author Saalik Hatia
 * @version 0.1
 **/

package s3fcache;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a generic cache entry with a key, value, and weight.
 * The weight is used to manage the entry's priority for eviction.
 * @param <K> the type of keys used by this cache entry.
 * @param <V> the type of values stored in this cache entry.
 */
public class CacheEntry<K, V> {
    private K key;
    private V value;
    private AtomicInteger weight;

    /**
     * Constructs a new CacheEntry with the specified key and value.
     * Initializes the weight with a default value of 1.
     *
     * @param key   the key associated with the cache entry.
     * @param value the value to be stored in the cache.
     */
    public CacheEntry(K key, V value) {
        this.key = key;
        this.value = value;
        this.weight = new AtomicInteger();
    }


    public K getKey() {
        return key;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public AtomicInteger getWeight() {
        return weight;
    }

    public void setWeight(AtomicInteger weight) {
        this.weight = weight;
    }
}
