package ru.netology;

public class Request {
    String metod;
    String head;
    String body;

    public Request(String metod, String head, String body){
        this.metod = metod;
        this.head = head;
        this.body = body;
    }

    public String getMetod(){
        return metod;
    }
    public String getHead(){
        return head;
    }
    public String getBody(){
        return body;
    }

    @Override
    public String toString (){
        return metod + " " + head + " " + body;
    }

}
