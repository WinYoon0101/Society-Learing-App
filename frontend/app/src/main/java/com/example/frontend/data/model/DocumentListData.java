package com.example.frontend.data.model;

import java.util.List;

public class DocumentListData {
    private List<Document> documents;
    private Pagination pagination;
    public List<Document> getDocuments() { return documents; }
    public Pagination getPagination() { return pagination; }

    public static class Pagination {
        private int page;
        private int limit;
        private int total;
        private int totalPages;

        // Getter cho total nếu cần hiện: "Bạn có 10 tài liệu"
        public int getTotal() { return total; }
    }
}
