package ru.netology;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.*;
import java.io.BufferedOutputStream;

import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
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
        final int limit = 4096; // ограничение на размер реквестлайн и заголовков
        final Socket socket;
        try {
            socket = serverSocket.accept();
            ExecutorService threadPool = Executors.newFixedThreadPool(64);// пул на 64 потока

            Runnable runnable = () -> {
                try (final var in = new BufferedInputStream(socket.getInputStream());
                     final var out = new BufferedOutputStream(socket.getOutputStream());) {

                    in.mark(limit);
                    final var buffer = new byte[limit];
                    final var read = in.read(buffer); // читаем данные из буфера но не более нашего лимита
                    in.reset();

                   // ищем request line
                    final var requestLineDelimiter = new byte[]{'\r', '\n'};
                    final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
                    if (requestLineEnd == -1) {
                        bedRequest(out);
                        socket.close();
                    }

                    final var parts = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");

                    if (parts.length != 3) {
                        socket.close();
                    }

                    final var path = parts[1];
                    Request request = new Request(parts[0], parts[1], parts[2]);

                    String metod = parts[0];
                    //System.out.println(metod);
                    //System.out.println(path);
                    String protocol = parts[2];
                    //System.out.println(protocol);

                    // проверяем попадает ли запрос в наш список путей, если нет выдаем ошибку
                    if (!handlers.containsKey(request.getMetod()) &
                            !handlers.get(request.getMetod()).containsKey(request.getPath())) {
                        bedRequest(out);
                    } else if (!parts[1].startsWith("/")) {
                        bedRequest(out);
                    } else {
                        Handler handler = handlers.get(request.getMetod()).get(request.getPath());
                        handler.handle(request, out);
                        out.flush();
                    }

                    // ищем заголовки
                    final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
                    final var headersStart = requestLineEnd + requestLineDelimiter.length;
                    final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
                    if (headersEnd == -1) {
                        bedRequest(out);
                        socket.close();
                    }
                    // отматываем на начало буфера
                    in.reset();
                    // пропускаем requestLine
                    in.skip(headersStart);

                    final var headersBytes = in.readNBytes(headersEnd - headersStart);
                    final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));
                    request.setHead(headers);
                    //System.out.println(headers);

                    // для GET тела нет
                    if (parts[0].equals("GET")) {
                        in.skip(headersDelimiter.length);
                        // вычитываем Content-Length, чтобы прочитать body
                        final var contentLength = extractHeader(headers, "Content-Length");
                        if (contentLength.isPresent()) {
                            final var length = Integer.parseInt(contentLength.get());
                            final var bodyBytes = in.readNBytes(length);

                            final var body = new String(bodyBytes);
                            request.setBody(body);
                           // System.out.println(body);
                        }
                    }

                    List<NameValuePair> paths = getQueryParams(request);
                    List<List<String>> param = new ArrayList<>();
                    param.add(getQueryParam(paths, path, request));
                    System.out.println("\nпуть " + path + "\nсодержет следующие заголовки\n" + param + "\n");

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

    public List<NameValuePair> getQueryParams(Request request) {
        return URLEncodedUtils.parse(request.path, StandardCharsets.UTF_8);
    }

    public List<String> getQueryParam(List<NameValuePair> paths, String path, Request request) {
        for (NameValuePair name : paths) {
            if (path.startsWith(name.getName())) {
                return request.getHead();
            }
        }
        return null;
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

    // from google guava with modifications
    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }
}
