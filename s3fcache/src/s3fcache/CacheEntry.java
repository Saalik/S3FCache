package s3fcache;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Saalik Hatia
 * @version 0.1
 **/

/**
 * Represents a generic cache entry with a key, value, and weight.
 * The weight is used to manage the entry's priority for eviction.
 * @author Saalik Hatia
 * @version 0.1
 */
public class CacheEntry<K, V> {
    private final K key;
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


    /**
     * Get the key associated with this cache entry.
     * @return the key associated with this cache entry.
     */
    public K getKey() {
        return key;
    }


    /**
     * Get the value stored in this cache entry.
     * @return the value stored in this cache entry.
     */
    public V getValue() {
        return value;
    }

    /**
     * Sets the value in this cache entry.
     * @param value the value to be stored in this cache entry.
     */
    public void setValue(V value) {
        this.value = value;
    }

    /**
     * Get the weight of this cache entry.
     * @return the weight of this cache entry.
     */
    public AtomicInteger getWeight() {
        return weight;
    }

    /**
     * Set the weight of this cache entry.
     * @param weight the weight to be set for this cache entry.
     */
    public void setWeight(AtomicInteger weight) {
        this.weight = weight;
    }
}
