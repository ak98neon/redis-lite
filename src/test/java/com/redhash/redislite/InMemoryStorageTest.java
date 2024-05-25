package com.redhash.redislite;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryStorageTest {

    private InMemoryStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryStorage();
    }

    @Test
    void testSetAndGet() {
        storage.set("key", "value");
        assertEquals("value", storage.get("key"));
    }

    @Test
    void testExpiry() {
        storage.setExpiry("key", 1000L);
        assertEquals(1000L, storage.getExpiry("key"));
        storage.removeExpiry("key");
        assertNull(storage.getExpiry("key"));
    }

    @Test
    void testExists() {
        storage.set("key", "value");
        assertTrue(storage.exists("key"));
        storage.remove("key");
        assertFalse(storage.exists("key"));
    }

    @Test
    void testListOperations() {
        storage.lpush("listKey", "value1");
        storage.rpush("listKey", "value2");
        assertEquals(2, storage.getListStorage().get("listKey").size());
        assertEquals("value1", storage.getListStorage().get("listKey").get(0));
        assertEquals("value2", storage.getListStorage().get("listKey").get(1));
    }
}
