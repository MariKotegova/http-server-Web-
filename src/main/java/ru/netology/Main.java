package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) throws IOException {
        Server server = new Server();
        // список путей
        final var validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

        ServerSocket serverSocket = server.startServer();
        while (true) {
            try {
                final var socket = serverSocket.accept();
                final ExecutorService threadPool = Executors.newFixedThreadPool(64);// пул на 64 потока
                Runnable runnable = () -> server.connection(socket, validPaths);
                threadPool.submit(runnable);// стартует потоки

                threadPool.shutdown(); // закроит потоки когда они будут не нужны
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}


