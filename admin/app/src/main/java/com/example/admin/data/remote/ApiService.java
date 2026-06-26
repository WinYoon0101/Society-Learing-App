package com.example.admin.data.remote;

import com.example.admin.data.model.ApiResponse;
import com.example.admin.data.model.DashboardResponse;
import com.example.admin.data.model.User;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;

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
}
