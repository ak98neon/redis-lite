package com.redhash.redislite;

import java.io.BufferedReader;
import java.io.IOException;

public class RESPMarshalling {

    public static String serialize(String message) {
        return "+" + message + "\r\n";
    }

    public Object deserialize(BufferedReader in) throws IOException {
        var message = in.readLine();
        if (message == null || message.isEmpty()) {
            return null;
        }

        char type = message.charAt(0);
        String content = message.substring(1);

        switch (type) {
            case '+':
                return content;
            case '-':
                return new Exception(content);
            case ':':
                return Long.parseLong(content);
            case '$':
                var length = Integer.parseInt(content);
                if (length == -1) {
                    return null;
                }

                var data = new char[length];
                in.read(data, 0, length);
                in.readLine();
                return new String(data);
            case '*':
                var size = Integer.parseInt(content);

                int i = 0;
                var arr = new String[size];
                while (size-- > 0) {
                    arr[i++] = (String) deserialize(in);
                }

                return arr;
            default:
                return content;
        }
    }
}
