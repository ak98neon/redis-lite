package com.redhash.redislite;

import java.io.BufferedWriter;
import java.io.IOException;

@FunctionalInterface
interface CommandHandler {
    void handle(String[] args, BufferedWriter out) throws IOException;
}
