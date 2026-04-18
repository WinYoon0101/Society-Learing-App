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

    // --- Hàm Thành Công ---
    public static <T> Result<T> success(T data) {
        return new Result<>(Status.SUCCESS, data, null);
    }

    // --- Các hàm Lỗi (Overloading) ---
    // Hàm nhận 2 tham số
    public static <T> Result<T> error(String message, T data) {
        return new Result<>(Status.ERROR, data, message);
    }

    //  Hàm chỉ nhận message (Dùng khi lỗi mà không có data trả về)
    public static <T> Result<T> error(String message) {
        return new Result<>(Status.ERROR, null, message);
    }

    // --- Các hàm Loading (Overloading) ---
    // Hàm nhận 1 tham số
    public static <T> Result<T> loading(T data) {
        return new Result<>(Status.LOADING, data, null);
    }

    //  Hàm không nhận tham số (Dùng khi bắt đầu tải dữ liệu)
    public static <T> Result<T> loading() {
        return new Result<>(Status.LOADING, null, null);
    }
}