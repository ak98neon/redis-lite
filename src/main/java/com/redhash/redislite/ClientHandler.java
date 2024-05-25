package com.redhash.redislite;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.IntStream;

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
        map.put("EXISTS", (args, out) -> processExistsCommand(storage, args, out));
        map.put("DEL", (args, out) -> processDelCommand(storage, args, out));
        map.put("INCR", (args, out) -> processIncrCommand(storage, args, out));
        map.put("DECR", (args, out) -> processDecrCommand(storage, args, out));
        map.put("LPUSH", (args, out) -> processLpushCommand(storage, args, out));
        map.put("RPUSH", (args, out) -> processRpushCommand(storage, args, out));
        map.put("SAVE", (args, out) -> processSaveCommand(storage, out));
        return map;
    }

    private static void processSaveCommand(InMemoryStorage storage, BufferedWriter out) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("redis-lite.dat"))) {
            oos.writeObject(storage.getStorage());
            oos.writeObject(storage.getExpiry());
            oos.writeObject(storage.getListStorage());
            out.write(RESPMarshalling.serialize(OK_RESPONSE));
        }
    }

    public static InMemoryStorage processLoadCommand() throws ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream("redis-lite.dat"))) {
            var storage = (ConcurrentMap<String, String>) in.readObject();
            var expiry = (ConcurrentMap<String, Long>) in.readObject();
            var listStorage = (ConcurrentMap<String, List<String>>) in.readObject();
            return new InMemoryStorage(storage, expiry, listStorage);
        } catch (Exception e) {
            log.warn("Error loading data from disk", e);
            return new InMemoryStorage();
        }
    }

    private static void processRpushCommand(InMemoryStorage storage, String[] args, BufferedWriter out) throws IOException {
        int listSize = 0;
        for (int i = 0; i < args.length; i++) {
            if (i > 1) {
                listSize = storage.rpush(args[1], args[i]);
            }
        }
        out.write(RESPMarshalling.serialize(String.valueOf(listSize)));
    }

    private static void processLpushCommand(InMemoryStorage storage, String[] args, BufferedWriter out) throws IOException {
        int listSize = 0;
        for (int i = 0; i < args.length; i++) {
            if (i > 1) {
                listSize = storage.lpush(args[1], args[i]);
            }
        }
        out.write(RESPMarshalling.serialize(String.valueOf(listSize)));
    }

    private static void processDecrCommand(InMemoryStorage storage, String[] args, BufferedWriter out) throws IOException {
        var value = args[1];
        if (storage.exists(value)) {
            var currentValue = storage.get(value);
            if (currentValue.matches("\\d+")) {
                var newValue = Long.parseLong(currentValue) - 1;
                storage.set(value, String.valueOf(newValue));
                out.write(RESPMarshalling.serialize(String.valueOf(newValue)));
            } else {
                out.write(RESPMarshalling.serialize("ERR value is not an integer or out of range"));
            }
        } else {
            storage.set(value, "-1");
            out.write(RESPMarshalling.serialize("-1"));
        }
    }

    private static void processIncrCommand(InMemoryStorage storage, String[] args, BufferedWriter out) throws IOException {
        var value = args[1];
        if (storage.exists(value)) {
            var currentValue = storage.get(value);
            if (currentValue.matches("\\d+")) {
                var newValue = Long.parseLong(currentValue) + 1;
                storage.set(value, String.valueOf(newValue));
                out.write(RESPMarshalling.serialize(String.valueOf(newValue)));
            } else {
                out.write(RESPMarshalling.serialize("ERR value is not an integer or out of range"));
            }
        } else {
            storage.set(value, "1");
            out.write(RESPMarshalling.serialize("1"));
        }
    }

    public static int getExpireTimeInMilliseconds(String type) {
        return EXPIRE_TIME_MAP.getOrDefault(type, 0);
    }

    private static void processExistsCommand(InMemoryStorage storage, String[] args, BufferedWriter out) throws IOException {
        int count = (int) IntStream.range(1, args.length)
                .filter(i -> storage.exists(args[i]))
                .count();

        out.write(RESPMarshalling.serialize(String.valueOf(count)));
    }

    private static void processDelCommand(InMemoryStorage storage, String[] args, BufferedWriter out) throws IOException {
        int count = 0;
        for (var arg : args) {
            storage.remove(arg);
            storage.removeExpiry(arg);
            count++;
        }

        out.write(RESPMarshalling.serialize(String.valueOf(count)));
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
