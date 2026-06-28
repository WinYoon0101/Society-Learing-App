package com.example.frontend.data.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;
import java.util.List;

public class GroupPost {
    @SerializedName("_id")
    private String id;
    @SerializedName("content")
    private String content;
    @SerializedName("images")
    private List<String> images;
    @SerializedName("authorId")
    private User authorId;

    // groupId có thể là String (chưa populate) hoặc Object (đã populate)
    // Dùng GroupRef để handle cả hai trường hợp
    @SerializedName("groupId")
    @JsonAdapter(GroupRefDeserializer.class)
    private Group groupId;

    @SerializedName("createdAt")
    private String createdAt;
    @SerializedName("countComment")
    private int countComment;
    @SerializedName("countReaction")
    private int countReaction;
    @SerializedName("myReaction")
    private String myReaction;
    @SerializedName("topReactions")
    private List<String> topReactions;
    @SerializedName("feeling")
    private String feeling;

    public String getId() { return id; }
    public String getContent() { return content; }
    public List<String> getImages() { return images; }
    public User getAuthorId() { return authorId; }
    public Group getGroupId() { return groupId; }
    public String getCreatedAt() { return createdAt; }
    public int getCountComment() { return countComment; }
    public int getCountReaction() { return countReaction; }
    public String getMyReaction() { return myReaction; }
    public void setMyReaction(String myReaction) { this.myReaction = myReaction; }
    public List<String> getTopReactions() { return topReactions; }
    public void setTopReactions(List<String> topReactions) { this.topReactions = topReactions; }
    public void setCountReaction(int countReaction) { this.countReaction = countReaction; }
    public String getFeeling() { return feeling; }
    public void setFeeling(String feeling) { this.feeling = feeling; }

    /**
     * Gson custom deserializer: nếu groupId là primitive String thì tạo Group rỗng với chỉ _id,
     * nếu là object thì deserialize bình thường.
     */
    public static class GroupRefDeserializer implements JsonDeserializer<Group> {
        @Override
        public Group deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException {
            if (json.isJsonPrimitive()) {
                // groupId là string (chưa populate)
                Group g = new Group();
                g.setId(json.getAsString());
                return g;
            }
            // groupId là object (đã populate)
            return ctx.deserialize(json, Group.class);
        }
    }
}
