package com.example.server;

public class ChatBean {
    private String content;
    private String name;
    private String time;
    private int type;

    public ChatBean(String content, String name, String time,int type) {
        this.content = content;
        this.name = name;
        this.time = time;
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public String getName() {
        return name;
    }

    public String getTime() {
        return time;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public String toJsonString(){
        String res = "{\"content\":\""+content+"\",\"name\":\""+name+"\",\"time\":\""+time+"\",\"type\":"+type+"}";
        return res;
    }
}
