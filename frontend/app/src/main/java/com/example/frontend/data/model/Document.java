package com.example.frontend.data.model;

import java.util.Objects;

public class Document {
    private String _id;
    private String title;
    private String description;
    private String subject;
    private int numberView;
    private int numberDownload;
    private User uploaderId; // Object lồng nhau vì Backend dùng .populate()
    private Media mediaId;
    private String createdAt;

    public String get_id() {
        return _id;
    }

    public String getId() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public int getNumberView() {
        return numberView;
    }

    public void setNumberView(int numberView) {
        this.numberView = numberView;
    }

    public int getNumberDownload() {
        return numberDownload;
    }

    public void setNumberDownload(int numberDownload) {
        this.numberDownload = numberDownload;
    }

    public User getUploaderId() {
        return uploaderId;
    }

    public void setUploaderId(User uploaderId) {
        this.uploaderId = uploaderId;
    }

    public Media getMediaId() {
        return mediaId;
    }

    public void setMediaId(Media mediaId) {
        this.mediaId = mediaId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUploaderName() {
        return (uploaderId != null) ? uploaderId.getUsername() : "Ẩn danh";
    }

    public String getFileUrl() {
        if (mediaId == null || mediaId.getUrl() == null) return "";
        String url = mediaId.getUrl();

        // 1. Nếu là link Google Drive
        if (url.contains("drive.google.com")) {
            String fileId = url.contains("/d/") ? url.split("/d/")[1].split("/")[0] : url.split("id=")[1].split("&")[0];
            return "https://drive.google.com/uc?id=" + fileId + "&export=download";
        }

        // 2. Với link Cloudinary
        //  thêm ?f=... để Google Docs Viewer chịu mở bản xem trước
        if (url.toLowerCase().contains(".pdf")) {
            if (!url.contains("?f=")) url += "?f=.pdf";
        } else if (url.toLowerCase().contains(".doc")) {
            if (!url.contains("?f=")) url += "?f=.docx";
        } else if (url.toLowerCase().contains(".ppt")) {
            if (!url.contains("?f=")) url += "?f=.pptx";
        } else if (url.toLowerCase().contains(".xls")) {
            if (!url.contains("?f=")) url += "?f=.xlsx";
        }

        return url;
    }

    public String getFileType() {
        return (mediaId != null) ? mediaId.getFileType() : "unknown";
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;

        // Sửa tên biến cho đúng với khai báo ở trên
        return numberView == document.numberView &&
                numberDownload == document.numberDownload &&
                Objects.equals(_id, document._id) && // Dùng Objects.equals để tránh null
                Objects.equals(title, document.title);
    }

    @Override
    public int hashCode() {
        // Sửa tên biến ở đây luôn
        return Objects.hash(_id, title, numberView, numberDownload);
    }
}
