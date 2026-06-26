package com.example.admin.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DashboardStats {
    public Overview overview;
    public InteractionsPieChart interactionsPieChart;
    public ReactionBarChart reactionBarChart;
    public List<Growth7Days> growth7DaysChart;

    public static class Overview {
        public int totalUsers;
        public int newUsersToday;
        public int totalPosts;
        public int pendingReports;
    }

    public static class InteractionsPieChart {
        public int reactions;
        public int comments;
    }

    public static class ReactionBarChart {
        // Dùng SerializedName để map đúng key viết hoa từ Backend trả về
        @SerializedName("Like") public int like;
        @SerializedName("Love") public int love;
        @SerializedName("Haha") public int haha;
        @SerializedName("Wow") public int wow;
        @SerializedName("Angry") public int angry;
        @SerializedName("Sad") public int sad;
    }

    public static class Growth7Days {
        public String date;
        public int newUsers;
        public int newPosts;
    }
}