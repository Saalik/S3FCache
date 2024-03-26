/**
 * @author Saalik Hatia
 * @version 0.1
 **/

package s3fcache;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * The main class for the S3FCache, implementing a cache system
 * with a three-queue eviction strategy based on the S3-FIFO algorithm.
 * This implementation is thread-safe.
 * @param <K> the type of keys used by this cache.
 * @param <V> the type of values stored in the cache.
 */
public class S3FCache<K, V> {
    private final static int DEFAULTSIZE = 10;
    private int maxWeight = 3;
    private final Deque<K> ghostQueue;
    private final Deque<CacheEntry<K, V>> smallQueue;
    private final Deque<CacheEntry<K, V>> bigQueue;

    private int bigQueueSize;
    private int smallQueueSize;
    private int ghostQueueSize;

    private final HashSet<K> ghostKeySet;
    private ConcurrentMap<K, CacheEntry<K, V>> cacheMap;


    /**
     * Constructs an S3FCache with specified sizes for each queue.
     * Initializes three concurrent queues to manage cache entries
     * in a thread-safe manner.
     * Default max weigth is 3.
     * Default size is 10.
     * @param maxQueueSize the maximum size of Big + Small queue.
     */
    public S3FCache(int maxQueueSize) {
        this.bigQueueSize = (int) (maxQueueSize * 0.9);
        this.smallQueueSize = (int) (maxQueueSize * 0.1);
        this.ghostQueueSize = maxQueueSize;
        this.ghostQueue = new ConcurrentLinkedDeque<>();
        this.smallQueue = new ConcurrentLinkedDeque<>();
        this.bigQueue = new ConcurrentLinkedDeque<>();
        this.ghostKeySet = new HashSet<>();
        this.cacheMap = new ConcurrentHashMap<>();
    }

    public S3FCache() {
        this(DEFAULTSIZE);
    }

    /**
     * If a key is in the small or big queue.
     * Return if and update the weight of the entry.
     * The weight is the min of the weight of the entry + 1 and the max weight.
     * If not in the cache, check if the key is in the ghost queue.
     * If it is, move it to the big queue and update the weight to 0.
     * If not in the cache, return null.
     * @param key the key to be searched for in the cache.
     * @return the value associated with the key, or null if not found.
     * @throws NullPointerException if the key is null.
     */
    public V get(K key) {
        // Assert that key is not null
        Objects.requireNonNull(key, "Key cannot be null");

        // Check if the key is in the cache
        if (cacheMap.containsKey(key)) {
            // Retrieve the entry from the cache
            CacheEntry<K, V> entry = cacheMap.get(key);
            // Update the weight of the entry
            AtomicInteger weight = entry.getWeight();
            // Set the weight to the min of the current weight + 1 and the max weight
            weight.set(Math.min(weight.get() + 1, maxWeight));
            return entry.getValue();
        }

        // Check if the key is in the ghost queue
        if (ghostKeySet.contains(key)) {
            ghostQueue.remove(key);
            ghostKeySet.remove(key);
            CacheEntry<K, V> entry = new CacheEntry<>(key, null);
            entry.getWeight().set(0);
            bigQueue.add(entry);
            cacheMap.put(key, entry);
            return null;
        }

        return null;
    }

    /**
     * New objects are inserted into S if not in G.
     * Otherwise, it is inserted into M.
     * When S is full, the object at the tail is either moved to M if it is accessed more than once or G if not.
     * And its access bits are cleared during the move.
     * When G is full, it evicts objects in FIFO order.
     * M uses an algorithm similar to FIFO-Reinsertion but tracks access information using two bits.
     * Objects that have been accessed at least once are reinserted with one bit set to 0 (similar to decreasing frequency by 1).
     * @param key the key to be associated with the value.
     * @param value the value to be stored in the cache.
     */
    public void put(K key, V value) {
        // Assert that key and value are not null
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(value, "Value cannot be null");

        // Check if the key is in the cache
        if (cacheMap.containsKey(key)) {
            // Retrieve the entry from the cache
            CacheEntry<K, V> entry = cacheMap.get(key);
            // Update the value of the entry
            entry.setValue(value);
            // Update the weight of the entry
            AtomicInteger weight = entry.getWeight();
            // Set the weight to the min of the current weight + 1 and the max weight
            weight.set(Math.min(weight.get() + 1, maxWeight));
            return;
        }

        // Check if we need to evict an entry from the cache
        if (memorySize() >= bigQueueSize + smallQueueSize + ghostQueueSize) {
            evict();
        }

        // Create a new cache entry with the specified key and value
        // Upon creation, the weight is initialized to 0
        CacheEntry<K, V> entry = new CacheEntry<>(key, value);

        // Check if the key is in the ghost queue
        if (ghostKeySet.contains(key)) {
            ghostKeySet.remove(key);
            ghostQueue.remove(key);
            bigQueue.add(entry);
            cacheMap.put(key, entry);
            return;
        }

        // If not in the ghost queue, add the entry to the small queue
        smallQueue.add(entry);
        cacheMap.put(key, entry);
    }

    /**
     * Evicts an entry from the cache based on the S3-FIFO algorithm.
     * If the small queue is not empty, the entry with the lowest weight is evicted.
     * If the weight is greater than 1, the weight is set to 0 and the entry is moved to the big queue.
     * If the weight is 1, the entry is moved to the ghost queue.
     * If the small queue is empty, the entry with the lowest weight is evicted from the big queue.
     * The entry is moved to the ghost queue.
     */
    public void evict() {
        // Check if the small queue is not empty
        if (!smallQueue.isEmpty()) {
            // Retrieve the entry from the small queue
            CacheEntry<K, V> entry = smallQueue.poll();
            // Update the weight of the entry
            AtomicInteger weight = entry.getWeight();
            // Check if the weight is greater than 1
            if (weight.get() > 1) {
                // Set the weight to 0
                weight.set(0);
                // Add the entry to the big queue
                bigQueue.add(entry);
            } else {
                // Add the key to the ghost queue
                ghostQueue.add(entry.getKey());
                ghostKeySet.add(entry.getKey());
            }
            // Remove the entry from the cache
            cacheMap.remove(entry.getKey());
        } else {
            // Retrieve the entry from the big queue
            CacheEntry<K, V> entry = bigQueue.poll();
            // Check if key is non-null
            Objects.requireNonNull(entry, "Entry cannot be null");
            // Add the key to the ghost queue
            ghostQueue.add(entry.getKey());
            ghostKeySet.add(entry.getKey());
            // Remove the entry from the cache
            cacheMap.remove(entry.getKey());
        }
    }

    /**
     * Returns a view of the cache as a concurrent map.
     * @return a concurrent map view of the cache.
     */
    public ConcurrentMap<K, CacheEntry<K, V>> asMap() {
        return cacheMap;
    }


    /**
     * Invalidates the cache entry for the specified key.
     * @param key the key to be invalidated.
     */
    public void invalidate(K key) {
        cacheMap.remove(key);
        removeKeyFromQueues(key);
    }

    /**
     * Removes the specified key from all queues.
     * @param key the key to be removed from the queues.
     */
    private void removeKeyFromQueues(K key) {
        ghostQueue.remove(key);
        smallQueue.removeIf(entry -> entry.getKey().equals(key));
        bigQueue.removeIf(entry -> entry.getKey().equals(key));
    }

    /**
     * Invalidates all cache entries.
     */
    public void invalidateAll() {
        cacheMap.clear();
        ghostQueue.clear();
        smallQueue.clear();
        bigQueue.clear();
    }


    /**
     * Invalidates the cache entries for the specified keys.
     * @param keys the keys to be invalidated.
     */
    public void invalidateAll(Iterable<K> keys) {
        for (K key : keys) {
            invalidate(key);
        }
    }

    /**
     * Copies all the mappings from the specified map to the cache.
     * @param map the map containing the mappings to be copied to the cache.
     */
    public void putAll(Map<? extends K,? extends V> map) {
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Returns the number of entries in the cache.
     * @return the number of entries in the cache.
     */
    public long size() {
        return cacheMap.size();
    }

    /** Returns the memory size of the cache.
     * @return the memory size of the cache.
     */
    public int memorySize() {
        return cacheMap.size() + ghostQueue.size() + smallQueue.size() + bigQueue.size();
    }

    /**
     * Returns the set of keys in the cache.
     * Does not contain the keys in the ghost queue.
     * @return the set of keys in the cache.
     */
    public Set<K> getKeys() {
        return cacheMap.keySet();
    }

    /**
     * Returns the set of keys in the ghost queue.
     * @return the set of keys in the ghost queue.
     */
    public HashSet<K> getGhostKeySet() {
        return ghostKeySet;
    }

    /**
     * Set new max weight for the cache.
     * @return old max weight.
     */
    public int setMaxWeight(int maxWeight) {
        int oldMaxWeight = this.maxWeight;
        this.maxWeight = maxWeight;
        return oldMaxWeight;
    }

    /**
     * Get the max weight for the cache.
     * @return max weight.
     */
    public int getMaxWeight() {
        return maxWeight;
    }

    /**
     * Set custom sizes for the queues.
     * All size are optional.
     * @param bigQueueSize the size of the big queue.
     * @param smallQueueSize the size of the small queue.
     * @param ghostQueueSize the size of the ghost queue.
     */
    public void setQueueSizes(int bigQueueSize, int smallQueueSize, int ghostQueueSize) {
        this.bigQueueSize = bigQueueSize;
        this.smallQueueSize = smallQueueSize;
        this.ghostQueueSize = ghostQueueSize;
    }

    /**
     * Set the size of the big queue.
     * @param bigQueueSize the size of the big queue.
     */
    public void setBigQueueSize(int bigQueueSize) {
        this.bigQueueSize = bigQueueSize;
    }

    /**
     * Set the size of the small queue.
     * @param smallQueueSize the size of the small queue.
     */
    public void setSmallQueueSize(int smallQueueSize) {
        this.smallQueueSize = smallQueueSize;
    }

    /**
     * Set the size of the ghost queue.
     * @param ghostQueueSize the size of the ghost queue.
     */
    public void setGhostQueueSize(int ghostQueueSize) {
        this.ghostQueueSize = ghostQueueSize;
    }

}
