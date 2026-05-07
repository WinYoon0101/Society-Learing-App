package com.example.frontend.data.model;

public class ProfileResponse {
    private Data data;
    public Data getData() {return data;}
    public class Data{
        private User user;
        public User getUser() {return user;}
    }
}
