package com.example.frontend.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Nhóm tin của 1 tác giả — backend trả về dạng này */
public class StoryGroup {
    @SerializedName("author")
    private User author;

    @SerializedName("stories")
    private List<Story> stories;

    @SerializedName("latestStoryId")
    private String latestStoryId;

    @SerializedName("latestMediaUrl")
    private String latestMediaUrl;

    @SerializedName("latestMediaType")
    private String latestMediaType;

    public User getAuthor()          { return author; }
    public List<Story> getStories()  { return stories; }
    public String getLatestStoryId() { return latestStoryId; }
    public String getLatestMediaUrl(){ return latestMediaUrl; }
    public String getLatestMediaType(){ return latestMediaType; }
}
