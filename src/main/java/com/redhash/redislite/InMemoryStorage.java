package com.redhash.redislite;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Getter
public class InMemoryStorage {

    private final ConcurrentMap<String, String> storage;
    private final ConcurrentMap<String, Long> expiry;
    private final ConcurrentMap<String, List<String>> listStorage;

    public InMemoryStorage() {
        this.storage = new ConcurrentHashMap<>();
        this.expiry = new ConcurrentHashMap<>();
        this.listStorage = new ConcurrentHashMap<>();
    }

    public InMemoryStorage(ConcurrentMap<String, String> storage,
                           ConcurrentMap<String, Long> expiry,
                           ConcurrentMap<String, List<String>> listStorage) {
        this.storage = storage;
        this.expiry = expiry;
        this.listStorage = listStorage;
    }

    public void set(String key, String value) {
        storage.put(key, value);
    }

    public String get(String key) {
        return storage.get(key);
    }

    public void setExpiry(String key, long seconds) {
        expiry.put(key, seconds);
    }

    public Long getExpiry(String key) {
        return expiry.get(key);
    }

    public void removeExpiry(String s) {
        expiry.remove(s);
    }

    public void remove(String s) {
        storage.remove(s);
    }

    public boolean exists(String key) {
        return storage.containsKey(key);
    }

    public int lpush(String key, String value) {
        listStorage.putIfAbsent(key, new ArrayList<>());
        listStorage.get(key).addFirst(value);
        return listStorage.get(key).size();
    }

    public int rpush(String key, String value) {
        listStorage.putIfAbsent(key, new ArrayList<>());
        listStorage.get(key).add(value);
        return listStorage.get(key).size();
    }
}
