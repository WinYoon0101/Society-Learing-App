package com.example.frontend.utils;

public class Result<T> {
    public enum Status { SUCCESS, ERROR, LOADING }

    public final Status status;
    public final T data;
    public final String message;

    private Result(Status status, T data, String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(Status.SUCCESS, data, null);
    }

    public static <T> Result<T> error(String message, T data) {
        return new Result<>(Status.ERROR, data, message);
    }

    public static <T> Result<T> loading(T data) {
        return new Result<>(Status.LOADING, data, null);
    }
}