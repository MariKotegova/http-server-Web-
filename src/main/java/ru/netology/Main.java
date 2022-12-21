package ru.netology;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        Server server = new Server();
        // добавление handler'ов (обработчиков)
        server.addHandler("GET", "/messages.txt", new Handler() {
            public void handle(Request request, BufferedOutputStream responseStream) {

                try {
                    final var filePath = Path.of(".", "public", request.getHead());
                    //System.out.println(" пришло " + filePath);
                    final var mimeType = Files.probeContentType(filePath);
                    final var length = Files.size(filePath);

                    responseStream.write((
                            "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: " + mimeType + "\r\n" +
                                    "Content-Length: " + length + "\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    Files.copy(filePath, responseStream); // выведет в браузер
                    responseStream.flush();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        server.addHandler("POST", "/messages", new Handler() {
            public void handle(Request request, BufferedOutputStream responseStream) {
                try {
                    final var filePath = Path.of(".", "public", request.getHead());
                    final var mimeType = request.getBody();

                    FileWriter writer = new FileWriter(filePath.toFile());
                    writer.write(mimeType);
                    writer.flush();
                    final var length = Files.size(filePath);

                    responseStream.write((
                            "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: " + mimeType + "\r\n" +
                                    "Content-Length: " + length + "\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    Files.copy(filePath, responseStream);
                    responseStream.flush();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        server.start(9999);
    }
}