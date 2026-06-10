package com.example.frontend.data.model;

public class Pagination {
    private int page;
    private int limit;
    private int total;
    private int totalPages;

    public int getPage() { return page; }
    public int getLimit() { return limit; }
    public int getTotal() { return total; }
    public int getTotalPages() { return totalPages; }
}