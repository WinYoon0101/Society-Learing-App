package com.example.frontend.data.remote;

import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Notification;
import com.example.frontend.data.model.User;
import com.example.frontend.data.model.Post;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ApiService {

    @POST("auth/login")
    Call<ApiResponse<User>> login(@Body Object loginData);

    @GET("user/profile")
    Call<ApiResponse<User>> getMyProfile();

    @GET("posts")
    Call<ApiResponse<List<Post>>> getPosts();

    @GET("notifications")
    Call<ApiResponse<List<Notification>>> getNotifications();

    @PUT("notifications/{id}/read")
    Call<ApiResponse<Void>> markAsRead(@Path("id") String id);

    @PUT("notifications/read-all")
    Call<ApiResponse<Void>> markAllAsRead();

    @DELETE("notifications/{id}")
    Call<ApiResponse<Void>> deleteNotification(@Path("id") String id);
}