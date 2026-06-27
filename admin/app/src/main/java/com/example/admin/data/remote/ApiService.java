package com.example.admin.data.remote;

import com.example.admin.data.model.DashboardResponse;
import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {
    // Gọi API lấy thống kê Dashboard
    @GET("admin/dashboard")
    Call<DashboardResponse> getDashboardStats();
}
