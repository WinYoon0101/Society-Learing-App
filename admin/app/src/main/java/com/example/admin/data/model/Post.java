package com.example.admin.data.model;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Post {
    @SerializedName("_id")
    private String id;
    @SerializedName("authorId")
    private Author author;
    private String content;
    private String createdAt;
    private List<String> mediaFiles;

    private boolean isScanned = false;
    private boolean isToxicLocally = false;
    private String toxicLabel = "";

    public String getId() { return id; }
    public Author getAuthor() { return author; }
    public String getContent() { return content; }
    public String getCreatedAt() { return createdAt; }
    public List<String> getMediaFiles() { return mediaFiles; }

    public boolean isScanned() { return isScanned; }
    public void setScanned(boolean scanned) { isScanned = scanned; }
    public boolean isToxicLocally() { return isToxicLocally; }
    public void setToxicLocally(boolean toxic) { isToxicLocally = toxic; }
    public String getToxicLabel() { return toxicLabel; }
    public void setToxicLabel(String label) { this.toxicLabel = label; }
}