package com.example.admin.data.remote;

import com.example.admin.data.model.ApiResponse;
import com.example.admin.data.model.DashboardResponse;
import com.example.admin.data.model.Post;
import com.example.admin.data.model.User;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    // Gọi API lấy thống kê Dashboard
    @GET("admin/dashboard")
    Call<DashboardResponse> getDashboardStats();

    // 1. Lấy danh sách toàn bộ User
    @GET("admin/users")
    Call<ApiResponse<List<User>>> getAllUsersAdmin();

    // 2. Khóa / Mở khóa User
    @PUT("admin/users/{id}/toggle-status")
    Call<ApiResponse<User>> toggleUserStatus(@Path("id") String userId);

    @GET("admin/posts")
    Call<ApiResponse<List<Post>>> getAllPostsAdmin(
            @Query("page") int page,
            @Query("limit") int limit
    );

    @DELETE("admin/posts/{id}")
    Call<ApiResponse<Object>> deletePostByAdmin(@Path("id") String postId);

    // 3. Gửi thông báo hệ thống
    @retrofit2.http.POST("admin/notifications/send")
    Call<ApiResponse<Object>> sendSystemNotification(@retrofit2.http.Body com.example.admin.data.model.NotificationRequest request);

}
