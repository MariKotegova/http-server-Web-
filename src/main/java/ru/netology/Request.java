package ru.netology;

import java.util.List;

public class Request {
    String metod;
    String path;
    String protocol;
    List<String> head;
    String body;

    public Request(String metod, String path, String protocol) {
        this.metod = metod;
        this.path = path;
        this.protocol = protocol;
    }

    public String getMetod() {
        return metod;
    }

    public String getPath() {
        return path;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setHead(List<String> head) {
        this.head = head;
    }

    public List<String> getHead() {
        return head;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getBody() {
        return body;
    }

    @Override
    public String toString() {
        return metod + " " + path + " " + protocol;
    }
}
