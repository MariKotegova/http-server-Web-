package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class Main {
  public static void main(String[] args) {
    // список путей
    final var validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

    try (final var serverSocket = new ServerSocket(9999)) {
      while (true) {
        try (
            final var socket = serverSocket.accept();
            final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            final var out = new BufferedOutputStream(socket.getOutputStream());
        ) {
          // read only request line for simplicity
          // must be in form GET /path HTTP/1.1
          final var requestLine = in.readLine(); //читаем всю строку
          final var parts = requestLine.split(" ");// разбиваем строку по пробелам(метод. путь к ресурсу. версия протокола)

          if (parts.length != 3) {// смотрим если в строке меньше 3х эл то читаем следующую
            // just close socket
            continue;
          }

          final var path = parts[1]; // вытаскиваем путь к ресурсу
          if (!validPaths.contains(path)) {// проверяем попадает он или нет в наш список
            out.write((// если не попадает то выдаст эту ошибку
                "HTTP/1.1 404 Not Found\r\n" +
                    "Content-Length: 0\r\n" +
                    "Connection: close\r\n" +
                    "\r\n"
            ).getBytes());// формируется выходной поток и он записывается в поток байт и отправляется пользователю
            out.flush();
            continue;
          }

          final var filePath = Path.of(".", "public", path);// указываем по какой ссылке обращаться
          //.- корневая папка, "public"- папка в ней, path - путь к ресурсу
          // localhost:9999/index.html
          final var mimeType = Files.probeContentType(filePath);// с помощью этого метода определяем тип по которому можно обращаться
          //к файлу .

          // special case for classic
          // шаблонизатор.
          if (path.equals("/classic.html")) {// по этому ссылке можно допустим вывести время(или что то другое)
            final var template = Files.readString(filePath); //читаем текст как строку
            final var content = template.replace(// в шаблоне ищем якорь {time} который заменяем нашими данными
                    // template.replace с помощью этого метода.
                "{time}",
                    // сюда можно вписать любой текст или блок кода и он его вставит, например "hello"
                      LocalDateTime.now().toString()// будем подставлять дату
            ).getBytes();
            out.write((
                "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: " + mimeType + "\r\n" +
                    "Content-Length: " + content.length + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n"
            ).getBytes());
            out.write(content);
            out.flush();
            continue;
          }

          final var length = Files.size(filePath);// определяем длину тела ответа
          out.write((// созлаем выходной поток с котдом выполнения, передаем потоком байт
              "HTTP/1.1 200 OK\r\n" +
                   // если написать так то покажет не картинку а текст в файле html "Content-Type: " + "text/plain" + "\r\n" +// путь к файлу
                  "Content-Type: " + mimeType + "\r\n" +// путь к файлу
                  "Content-Length: " + length + "\r\n" +// длина тела ответа
                  "Connection: close\r\n" +
                  "\r\n"
          ).getBytes());
          Files.copy(filePath, out);// копируем файл в тело ответа
          out.flush();
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}


