# Redis Lite Server

Welcome to the Redis Lite Server project! This repository contains a lightweight implementation of a Redis server built using Java 21 and Maven. The project serves as a learning exercise in understanding the internals of Redis, an in-memory data structure store used as a database, cache, and message broker.

## Project Overview

Redis Lite Server is designed to support fundamental Redis operations including handling multiple data types, managing key-value pairs, and supporting concurrent client connections. The implementation is structured to follow a series of steps that gradually build the server from basic protocol handling to advanced command support and performance testing.

## Features

1. **RESP Protocol Handling**: Implements serialization and deserialization of Redis Serialization Protocol (RESP) messages.
2. **Basic Commands**: Supports fundamental commands such as `PING`, `ECHO`, `SET`, and `GET`.
3. **Concurrent Clients**: Handles multiple client connections simultaneously using Java's concurrency features.
4. **Data Persistence**: Implements basic persistence with `SAVE` and `LOAD` commands to store and retrieve the database state.
5. **Advanced Commands**: Includes commands like `EXISTS`, `DEL`, `INCR`, `DECR`, `LPUSH`, and `RPUSH`.
6. **Expiry Options**: Extends `SET` command to support expiration options (`EX`, `PX`, `EXAT`, `PXAT`).

## Tech Stack

- **Programming Language**: Java 21
- **Build Tool**: Maven

## Getting Started

### Prerequisites

- Java 21
- Maven

### Installation

1. Clone the repository:
    ```sh
    git clone https://github.com/your-username/redis-lite-server.git
    cd redis-lite-server
    ```

2. Build the project using Maven:
    ```sh
    mvn clean install
    ```

### Running the Server

1. Start the Redis Lite Server:
    ```sh
    java -jar target/redis-lite-server-1.0-SNAPSHOT.jar
    ```

2. Use a Redis client to connect to the server (default port is 6379):
    ```sh
    redis-cli -p 6379
    ```

## Usage

### Basic Commands

- **PING**: Test the connection to the server.
    ```sh
    redis-cli PING
    PONG
    ```

- **ECHO**: Echoes the input string.
    ```sh
    redis-cli ECHO "Hello World"
    "Hello World"
    ```

- **SET**: Set a key to hold the string value.
    ```sh
    redis-cli SET Name John
    OK
    ```

- **GET**: Get the value of a key.
    ```sh
    redis-cli GET Name
    "John"
    ```

### Advanced Commands

- **EXISTS**: Check if a key exists.
    ```sh
    redis-cli EXISTS Name
    (integer) 1
    ```

- **DEL**: Delete one or more keys.
    ```sh
    redis-cli DEL Name
    (integer) 1
    ```

- **INCR**: Increment the integer value of a key by one.
    ```sh
    redis-cli INCR counter
    (integer) 1
    ```

- **DECR**: Decrement the integer value of a key by one.
    ```sh
    redis-cli DECR counter
    (integer) 0
    ```

- **LPUSH**: Insert values at the head of the list.
    ```sh
    redis-cli LPUSH mylist "world"
    (integer) 1
    ```

- **RPUSH**: Insert values at the tail of the list.
    ```sh
    redis-cli RPUSH mylist "hello"
    (integer) 2
    ```

- **SAVE**: Save the database state to disk.
    ```sh
    redis-cli SAVE
    OK
    ```

- **LOAD**: Load the database state from disk (run on server startup).

## Performance Testing

Use `redis-benchmark` to test the performance of the server. For example:
```sh
redis-benchmark -t SET,GET -q
