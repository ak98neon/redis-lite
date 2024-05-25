package com.redhash.redislite;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

class RESPMarshallingTest {

    private final RESPMarshalling marshalling = new RESPMarshalling();

    @Test
    void testSerialize() {
        String message = "OK";
        assertEquals("+OK\r\n", RESPMarshalling.serialize(message));
    }

    @Test
    void testDeserialize() throws Exception {
        BufferedReader reader = new BufferedReader(new StringReader("+OK\r\n"));
        assertEquals("OK", marshalling.deserialize(reader));
    }

    @Test
    void testDeserializeError() throws Exception {
        BufferedReader reader = new BufferedReader(new StringReader("-Error message\r\n"));
        assertEquals(new Exception("Error message").toString(), marshalling.deserialize(reader).toString());
    }

    @Test
    void testDeserializeInteger() throws Exception {
        BufferedReader reader = new BufferedReader(new StringReader(":1000\r\n"));
        assertEquals(1000L, marshalling.deserialize(reader));
    }

    @Test
    void testDeserializeBulkString() throws Exception {
        BufferedReader reader = new BufferedReader(new StringReader("$6\r\nfoobar\r\n"));
        assertEquals("foobar", marshalling.deserialize(reader));
    }

    @Test
    void testDeserializeArray() throws Exception {
        BufferedReader reader = new BufferedReader(new StringReader("*2\r\n$3\r\nfoo\r\n$3\r\nbar\r\n"));
        assertArrayEquals(new String[]{"foo", "bar"}, (String[]) marshalling.deserialize(reader));
    }
}
