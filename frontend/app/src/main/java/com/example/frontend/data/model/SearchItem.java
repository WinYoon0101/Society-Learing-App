package com.example.frontend.data.model;

public class SearchItem {
    public static final int TYPE_USER = 1;
    public static final int TYPE_GROUP = 2;
    public static final int TYPE_POST = 3;

    private int type;
    private Object data;

    public SearchItem(int type, Object data) {
        this.type = type;
        this.data = data;
    }

    public int getType() { return type; }
    public Object getData() { return data; }
}