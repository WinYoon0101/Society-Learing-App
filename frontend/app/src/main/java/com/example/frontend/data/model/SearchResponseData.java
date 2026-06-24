package com.example.frontend.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SearchResponseData {

    @SerializedName("users")
    private List<User> users;

    @SerializedName("posts")
    private List<Post> posts;

    @SerializedName("groups")
    private List<Group> groups;

    // Getter và Setter cho Users
    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    // Getter và Setter cho Posts
    public List<Post> getPosts() {
        return posts;
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts;
    }

    // Getter và Setter cho Groups
    public List<Group> getGroups() {
        return groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }
}