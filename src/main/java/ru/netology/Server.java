package ru.netology;

import java.io.*;
import java.io.BufferedOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    ServerSocket serverSocket;
    Map<String, ConcurrentHashMap<String, Handler>> handlers;

    public Server() {
        System.out.println("Start server");
        handlers = new ConcurrentHashMap<>();
    }

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            while (true) {
                acept();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void acept() {
        final int limit = 4096;
        final Socket socket;
        try {
            socket = serverSocket.accept();
            ExecutorService threadPool = Executors.newFixedThreadPool(64);// пул на 64 потока

            Runnable runnable = () -> {
                try (
                        final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        final var out = new BufferedOutputStream(socket.getOutputStream());
                ) {
                    // read only request line for simplicity
                    // must be in form GET /path HTTP/1.1
                    final var requestLine = in.readLine();
                    final var parts = requestLine.split(" ");

                    if (parts.length != 3) {
                        // just close socket
                        socket.close();
                    }

                    final var path = parts[1];
                    Request request = new Request(parts[0], parts[1], parts[2]);
                    //System.out.println(request);
                    // проверяем попадает ли запрос в наш список путей, если нет выдаем ошибку
                    if (!handlers.containsKey(request.getMetod()) &
                            !handlers.get(request.getMetod()).containsKey(request.getHead())) {

                        bedRequest(out);
                    } else {

                        Handler handler = handlers.get(request.getMetod()).get(request.getHead());
                        handler.handle(request, out);
                        out.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };
            threadPool.submit(runnable);// стартует потоки
            threadPool.shutdown(); // закроит потоки когда они будут не нужны
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addHandler(String metod, String path, Handler handler) {
        if (!handlers.containsKey(metod)) {
            handlers.put(metod, new ConcurrentHashMap<>());
        }
        handlers.get(metod).put(path, handler);

        File file = new File("./public" + path);
        try {
            if (file.createNewFile()) {
                System.out.println("Создан новый файл: " + path);
            } else {
                System.out.println("Файл не создан ");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void bedRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close&\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }
}
