package com.example.frontend.data.model;

import java.util.List;

public class PagedResponse<T> {
    private boolean success;
    private List<T> data;
    private Pagination pagination;

    public boolean isSuccess() { return success; }
    public List<T> getData() { return data; }
    public Pagination getPagination() { return pagination; }
}