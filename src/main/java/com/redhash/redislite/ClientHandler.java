package com.redhash.redislite;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import static java.lang.System.currentTimeMillis;

@Slf4j
public class ClientHandler extends Thread {

    private static final Map<String, Integer> EXPIRE_TIME_MAP = createExpireTimeMap();
    private static final String OK_RESPONSE = "OK";
    private static final String PONG_RESPONSE = "PONG";
    private static final String ECHO_RESPONSE = "Hello World!";
    private static final String UNKNOWN_COMMAND_RESPONSE = "Unknown command";
    private final Socket socket;
    private final Map<String, CommandHandler> commandMap;

    public ClientHandler(Socket accept, InMemoryStorage storage) {
        this.socket = accept;
        this.commandMap = createCommandMap(storage);
    }

    private static Map<String, Integer> createExpireTimeMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("EX", 1000);
        map.put("EXAT", 1000);
        map.put("PX", 1);
        map.put("PXAT", 1);
        return map;
    }

    private static Map<String, CommandHandler> createCommandMap(InMemoryStorage storage) {
        Map<String, CommandHandler> map = new HashMap<>();
        map.put("COMMAND", (args, out) -> out.write(RESPMarshalling.serialize(OK_RESPONSE)));
        map.put("PING", (args, out) -> out.write(RESPMarshalling.serialize(PONG_RESPONSE)));
        map.put("ECHO", (args, out) -> out.write(RESPMarshalling.serialize(ECHO_RESPONSE)));
        map.put("SET", (args, out) -> processSetCommand(storage, args, out));
        map.put("GET", (args, out) -> processGetCommand(storage, args, out));
        return map;
    }

    public static int getExpireTimeInMilliseconds(String type) {
        return EXPIRE_TIME_MAP.getOrDefault(type, 0);
    }

    private static void processSetCommand(InMemoryStorage storage, String[] arr, BufferedWriter out) throws IOException {
        storage.set(arr[1], arr[2]);
        if (arr.length > 3) {
            var multiplication = getExpireTimeInMilliseconds(arr[3]);
            long ttl = switch (arr[3]) {
                case "EXAT", "PXAT" -> Long.parseLong(arr[4]) * multiplication;
                default -> (Long.parseLong(arr[4]) * multiplication) + currentTimeMillis();
            };
            storage.setExpiry(arr[1], ttl);
        }
        out.write(RESPMarshalling.serialize(OK_RESPONSE));
    }

    private static void processGetCommand(InMemoryStorage storage, String[] arr, BufferedWriter out) throws IOException {
        var value = storage.get(arr[1]);
        if (storage.getExpiry(arr[1]) != null) {
            var ttl = storage.getExpiry(arr[1]);
            if (currentTimeMillis() > ttl) {
                storage.removeExpiry(arr[1]);
                storage.remove(arr[1]);
                value = null;
            }
        }
        out.write(RESPMarshalling.serialize(value));
    }

    @Override
    public void run() {
        handle();
    }

    private void handle() {
        try (var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             var out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            while (true) {
                var command = new RESPMarshalling().deserialize(in);
                if (command instanceof String[] arr) {
                    var cmd = arr[0];
                    var handler = commandMap.getOrDefault(cmd, (args, output) -> out.write(RESPMarshalling.serialize(UNKNOWN_COMMAND_RESPONSE)));
                    handler.handle(arr, out);
                }
                out.flush();
            }

        } catch (Exception e) {
            log.error("Error handling client", e);
        }
    }
}
