package com.example.frontend.data.remote;

import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Friend;
import com.example.frontend.data.model.LoginResponse;
import com.example.frontend.data.model.UpdateProfile;
import com.example.frontend.data.model.User;

import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ApiService {
    @POST("auth/login") // Endpoint đăng nhập
    Call<ApiResponse<LoginResponse>> login(@Body LoginRequest request);

    @POST("auth/register")
    Call<ApiResponse<LoginResponse>> register(@Body RegisterRequest request);

    // Lấy danh sách gợi ý kết bạn
    @GET("friends/suggestions")
    Call<ApiResponse<List<Friend>>> getFriendSuggestions();

    // 1. Lấy danh sách bạn bè
    @GET("friends")
    Call<ApiResponse<List<Friend>>> getFriends();

    // 2. Lấy danh sách lời mời kết bạn (Pending)
    @GET("friends/pending")
    Call<ApiResponse<List<Friend>>> getPendingRequests();

    // 3. Gửi lời mời kết bạn
    @POST("friends/request/{id}")
    Call<ApiResponse<Object>> sendFriendRequest(@Path("id") String userId);

    // 4. Chấp nhận lời mời
    @PUT("friends/accept/{id}")
    Call<ApiResponse<Object>> acceptFriendRequest(@Path("id") String userId);

    // 5. Từ chối lời mời
    @DELETE("friends/decline/{id}")
    Call<ApiResponse<Object>> declineFriendRequest(@Path("id") String userId);

    // 6. Huỷ kết bạn
    @DELETE("friends/remove/{id}")
    Call<ApiResponse<Object>> removeFriend(@Path("id") String userId);

    @GET("users/me")
    Call<ApiResponse<User>> getMyProfile();

    @PUT("users/update")
    Call<ApiResponse<User>> updateProfile(@Body UpdateProfile request);

    @Multipart
    @POST("users/upload-avatar")
    Call<ApiResponse<String>> uploadAvatar(@Part MultipartBody.Part file);

    @Multipart
    @POST("users/upload-cover")
    Call<ApiResponse<String>> uploadCover(@Part MultipartBody.Part file);
}
