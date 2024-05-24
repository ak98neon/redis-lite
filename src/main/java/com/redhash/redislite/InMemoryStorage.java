package com.redhash.redislite;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryStorage {

    private final ConcurrentMap<String, String> storage;
    private final ConcurrentMap<String, Long> expiry;

    public InMemoryStorage() {
        this.storage = new ConcurrentHashMap<>();
        this.expiry = new ConcurrentHashMap<>();
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
}
