package com.oracle.oci.intellij.ui.common.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleCache<K,V> implements Cache<K,V>{

    private final Map<K,V> cache ;


    public SimpleCache() {
        this.cache = new ConcurrentHashMap<>();
    }

    @Override
    public void put(K key, V value) {
        cache.put(key,value);
    }

    @Override
    public V get(K key) {
        return cache.get(key);
    }

    @Override
    public V remove(K key) {
        return cache.remove(key);
    }

    @Override
    public void clear() {
        cache.clear();
    }

}
