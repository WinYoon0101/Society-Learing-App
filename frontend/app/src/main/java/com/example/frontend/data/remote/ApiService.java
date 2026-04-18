package com.example.frontend.data.remote;

import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Document;
import com.example.frontend.data.model.DocumentListData;
import com.example.frontend.data.model.Friend;
import com.example.frontend.data.model.LoginResponse;
import com.example.frontend.data.model.Media;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

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

    //TÀI LIỆU

    // 1. Lấy danh sách tài liệu công khai
    @GET("documents")
    Call<ApiResponse<DocumentListData>> getDocuments(
            @Query("page") int page,
            @Query("search") String search,
            @Query("subject") String subject,
            @Query("sortBy") String sortBy
    );

    // 2. Tạo tài liệu mới

    //Tải file lên Cloudinary thông qua Route Media để lấy mediaId rồi mới tạo Document
    @Multipart
    @POST("media/upload/document") // Đổi từ /single thành /document cho giống backend
    Call<ApiResponse<Media>> uploadSingleFile(
            @Part MultipartBody.Part file, // Giữ nguyên "media" ở đây nếu code Activity gửi "media"
            @Part("sourceType") RequestBody sourceType,
            @Part("targetId") RequestBody targetId
    );

    @POST("documents")
    Call<ApiResponse<Document>> createDocument(@Body Map<String, Object> body);


    // 3. Lưu/Bỏ lưu tài liệu
    @POST("documents/{id}/save")
    Call<ApiResponse<Map<String, Boolean>>> toggleSave(@Path("id") String id);

    // 4. Lấy tài liệu đã lưu
    @GET("documents/me/saved")
    Call<ApiResponse<DocumentListData>> getSavedDocuments(
            @Query("page") int page,
            @Query("limit") int limit
    );

    // 5. Xóa tài liệu
    @DELETE("documents/{id}")
    Call<ApiResponse<Void>> deleteDocument(@Path("id") String id);
// 6. Tăng lượt tải về
    @POST("documents/{id}/download")
    Call<ApiResponse<Object>> incrementDownload(@Path("id") String id);
        // 7. Cập nhật tài liệu (PATCH)
    @PATCH("documents/{id}")
    Call<ApiResponse<Document>> updateDocument(@Path("id") String id, @Body Map<String, Object> updates);

}
