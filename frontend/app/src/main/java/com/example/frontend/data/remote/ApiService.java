package com.example.frontend.data.remote;

import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.AvatarResponse;
import com.example.frontend.data.model.Conversation;
import com.example.frontend.data.model.Comment;
import com.example.frontend.data.model.CommentRequest;
import com.example.frontend.data.model.CoverResponse;
import com.example.frontend.data.model.Document;
import com.example.frontend.data.model.DocumentListData;
import com.example.frontend.data.model.Friend;
import com.example.frontend.data.model.Group;
import com.example.frontend.data.model.GroupDetail;
import com.example.frontend.data.model.GroupInvitation;
import com.example.frontend.data.model.GroupMember;
import com.example.frontend.data.model.GroupPost;
import com.example.frontend.data.model.LiveModel;
import com.example.frontend.data.model.LoginResponse;
import com.example.frontend.data.model.Story;
import com.example.frontend.data.model.StoryGroup;
import com.example.frontend.data.model.NotificationListResponse;
import com.example.frontend.data.model.Notification;
import com.example.frontend.data.model.PagedResponse;
import com.example.frontend.data.model.ReactionItem;
import com.example.frontend.data.model.UpdateProfile;
import com.example.frontend.data.model.User;
import com.example.frontend.data.model.ProfileResponse;
import com.example.frontend.data.model.Media;
import com.example.frontend.data.model.Message;
import com.example.frontend.data.model.Post;
import com.example.frontend.data.model.Quiz;
import com.example.frontend.data.model.ReactionRequest;
import com.example.frontend.data.remote.SubmitQuizRequest;
import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    // ====== AUTH ======
    @POST("auth/login")
    Call<ApiResponse<LoginResponse>> login(@Body LoginRequest request);

    @POST("auth/register")
    Call<ApiResponse<LoginResponse>> register(@Body RegisterRequest request);

    @POST("auth/send-otp")
    Call<ApiResponse> sendOtp(@Body EmailRequest request);

    @POST("auth/google-login")
    Call<ApiResponse<LoginResponse>> googleLogin(@Body GoogleLoginRequest request);

    @POST("auth/facebook-login")
    Call<ApiResponse<LoginResponse>> facebookLogin(@Body FacebookLoginRequest request);

    @POST("auth/verify-otp")
    Call<ApiResponse> verifyOtp(@Body OtpRequest request);

    @POST("auth/reset-password")
    Call<ApiResponse> resetPassword(@Body ResetPasswordRequest request);

    // ====== FRIENDS ======
    @GET("friends/suggestions")
    Call<ApiResponse<List<Friend>>> getFriendSuggestions();

    @GET("friends")
    Call<ApiResponse<List<Friend>>> getFriends();

    @GET("friends/pending")
    Call<ApiResponse<List<Friend>>> getPendingRequests();

    @POST("friends/request/{id}")
    Call<ApiResponse<Object>> sendFriendRequest(@Path("id") String userId);

    @PUT("friends/accept/{id}")
    Call<ApiResponse<Object>> acceptFriendRequest(@Path("id") String userId);

    @DELETE("friends/decline/{id}")
    Call<ApiResponse<Object>> declineFriendRequest(@Path("id") String userId);

    @DELETE("friends/remove/{id}")
    Call<ApiResponse<Object>> removeFriend(@Path("id") String userId);

    // ====== STORIES ======
    @GET("stories")
    Call<ApiResponse<List<StoryGroup>>> getFeedStories();

    @Multipart
    @POST("stories")
    Call<ApiResponse<Story>> createStory(
            @Part MultipartBody.Part file,
            @Part("caption") RequestBody caption);

    @GET("stories/{storyId}")
    Call<ApiResponse<Story>> viewStory(@Path("storyId") String storyId);

    @DELETE("stories/{storyId}")
    Call<ApiResponse<Object>> deleteStory(@Path("storyId") String storyId);

    // ====== NOTIFICATIONS ======
    @GET("notifications")
    Call<NotificationListResponse> getNotifications(@Query("page") int page, @Query("limit") int limit);

    @PUT("notifications/{id}/read")
    Call<ApiResponse<Object>> markNotificationRead(@Path("id") String id);

    @PUT("notifications/mark-all-read")
    Call<ApiResponse<Object>> markAllNotificationsRead();

    // ====== USER ======
    @GET("user/search")
    Call<ApiResponse<List<User>>> searchUsers(@Query("q") String query);

    @GET("user/profile")
    Call<ApiResponse<User>> getMyProfile();

    @PUT("user/update")
    Call<ApiResponse<User>> updateProfile(@Body UpdateProfile request);

    @Multipart
    @PUT("user/avatar")
    Call<ApiResponse<AvatarResponse>> uploadAvatar(@Part MultipartBody.Part file);

    @Multipart
    @PUT("user/cover")
    Call<ApiResponse<CoverResponse>> uploadCover(@Part MultipartBody.Part file);

    // ====== DOCUMENTS ======
    @GET("documents/me/list")
    Call<ApiResponse<DocumentListData>> getMyDocuments(@Query("page") int page, @Query("limit") int limit);

    @GET("documents")
    Call<ApiResponse<DocumentListData>> getDocuments(
            @Query("page") int page,
            @Query("search") String search,
            @Query("subject") String subject,
            @Query("sortBy") String sortBy
    );

    @GET("media/me")
    Call<ApiResponse<List<Media>>> getMyMedia(@Query("fileType") String fileType);


    @Multipart

    @POST("media/upload/document")
    Call<ApiResponse<Media>> uploadSingleFile(
            @Part MultipartBody.Part file,
            @Part("sourceType") RequestBody sourceType,
            @Part("targetId") RequestBody targetId
    );

    @POST("documents")
    Call<ApiResponse<Document>> createDocument(@Body Map<String, Object> body);

    @POST("documents/{id}/save")
    Call<ApiResponse<Map<String, Boolean>>> toggleSave(@Path("id") String id);

    @GET("documents/me/saved")
    Call<ApiResponse<DocumentListData>> getSavedDocuments(@Query("page") int page, @Query("limit") int limit);

    @DELETE("documents/{id}")
    Call<ApiResponse<Void>> deleteDocument(@Path("id") String id);

    @POST("documents/{id}/download")
    Call<ApiResponse<Object>> incrementDownload(@Path("id") String id);

    @PATCH("documents/{id}")
    Call<ApiResponse<Document>> updateDocument(@Path("id") String id, @Body Map<String, Object> updates);

    @GET("documents/{id}")
    Call<ApiResponse<Document>> getDocumentById(@Path("id") String id);

    // ====== CHAT ======
    @GET("chat/conversations")
    Call<ApiResponse<List<Conversation>>> getConversations();

    @POST("chat/conversations")
    Call<ApiResponse<Conversation>> getOrCreateConversation(@Body Map<String, String> body);

    @GET("chat/conversations/{conversationId}/messages")
    Call<ApiResponse<List<Message>>> getMessages(@Path("conversationId") String conversationId);

    @POST("chat/messages")
    Call<ApiResponse<Message>> sendMessage(@Body Map<String, String> body);

    // ====== POSTS ======

    @GET("posts/user/{userId}")
    Call<ApiResponse<List<Post>>> getPostsByUser(@Path("userId") String userId);

    @GET("posts/feed")
    Call<ApiResponse<List<Post>>> getAllPosts();

    @Multipart
    @POST("posts/create")
    Call<ApiResponse<Post>> createPost(
            @Part("content") RequestBody content,
            @Part("privacy") RequestBody privacy,
            @Part("groupId") RequestBody groupId,
            @Part("tags") RequestBody tags,
            @Part("initialReaction") RequestBody initialReaction,
            @Part List<MultipartBody.Part> images
    );

    @DELETE("posts/{id}")
    Call<ApiResponse<Object>> deletePost(
            @Header("Authorization") String token,
            @Path("id") String postId
    );

    @GET("posts/me")
    Call<ApiResponse<List<Post>>> getMyPosts();

    @POST("posts/{id}/save")
    Call<ApiResponse<Object>> toggleSavePost(
            @Header("Authorization") String token,
            @Path("id") String postId
    );

    @GET("posts/my/saved")
    Call<ApiResponse<List<Post>>> getSavedPosts(@Header("Authorization") String token);

    // ====== COMMENTS ======
    @GET("/api/comments/post/{postId}")
    Call<ApiResponse<List<Comment>>> getComments(@Path("postId") String postId);

    @POST("/api/comments")
    Call<ApiResponse<Comment>> createComment(
            @Header("Authorization") String token,
            @Body CommentRequest body
    );

    @DELETE("/api/comments/{commentId}")
    Call<ApiResponse<Object>> deleteComment(
            @Header("Authorization") String token,
            @Path("commentId") String commentId
    );

    // ====== REACTIONS ======
    @GET("reactions/{targetId}")
    Call<ApiResponse<List<ReactionItem>>> getReactionsOfPost(@Path("targetId") String targetId);

    @POST("reactions/toggle")
    Call<ApiResponse<Object>> toggleReaction(@Body ReactionRequest request);

    // ====== QUIZ ======
    @POST("quiz/generate-quiz")
    Call<ApiResponse<Quiz>> generateQuiz(@Body QuizRequest request);

    @GET("quiz/my-quizzes")
    Call<ApiResponse<List<Quiz>>> getMyQuizzes();

    @POST("quiz/submit")
    Call<ApiResponse<Object>> submitQuiz(@Body SubmitQuizRequest request);

    // ====== MEDIA ======
    @Multipart
    @POST("media/upload")
    Call<ApiResponse<List<Media>>> uploadChatMedia(
            @Part MultipartBody.Part file,
            @Part("sourceType") RequestBody sourceType,
            @Part("targetId") RequestBody targetId
    );

    // ====== GROUPS ======
    // Tab "Nhóm của bạn"
    @GET("groups/my")
    Call<ApiResponse<List<Group>>> getMyGroups();

    // Tab "Bài viết" - tất cả bài từ nhóm đang tham gia
    @GET("groups/posts")
    Call<ApiResponse<List<GroupPost>>> getGroupFeedPosts(
            @Query("page") int page,
            @Query("limit") int limit
    );

    // Tab "Khám phá"
    @GET("groups/discover")
    Call<ApiResponse<List<Group>>> discoverGroups(
            @Query("search") String search,
            @Query("page") int page,
            @Query("limit") int limit
    );

    // Tab "Lời mời"
    @GET("groups/invitations")
    Call<ApiResponse<List<GroupInvitation>>> getGroupInvitations();

    // Phản hồi lời mời: body = { "action": "accept" | "decline" }
    @PATCH("groups/invitations/{invitationId}")
    Call<ApiResponse<Object>> respondToInvitation(
            @Path("invitationId") String invitationId,
            @Body Map<String, String> body
    );

    // Gửi lời mời: body = { groupId, inviteeId }
    @POST("groups/invitations")
    Call<ApiResponse<Object>> sendGroupInvitation(@Body Map<String, String> body);

    // Tạo nhóm
    @Multipart
    @POST("groups")
    Call<ApiResponse<Group>> createGroup(
            @Part("groupName") RequestBody groupName,
            @Part("description") RequestBody description,
            @Part("privacy") RequestBody privacy,
            @Part MultipartBody.Part avatar
    );

    // Chi tiết nhóm
    @GET("groups/{groupId}")
    Call<ApiResponse<GroupDetail>> getGroupDetail(@Path("groupId") String groupId);

    // Cập nhật nhóm (tên, mô tả, ảnh) - chỉ admin
    @Multipart
    @PATCH("groups/{groupId}")
    Call<ApiResponse<GroupDetail>> updateGroup(
            @Path("groupId") String groupId,
            @Part("groupName") RequestBody groupName,
            @Part("description") RequestBody description,
            @Part("privacy") RequestBody privacy,
            @Part MultipartBody.Part avatar
    );

    // Tham gia nhóm public
    @POST("groups/{groupId}/join")
    Call<ApiResponse<Object>> joinGroup(@Path("groupId") String groupId);

    // Bài viết của 1 nhóm cụ thể
    @GET("groups/{groupId}/posts")
    Call<ApiResponse<List<GroupPost>>> getPostsByGroup(
            @Path("groupId") String groupId,
            @Query("page") int page,
            @Query("limit") int limit
    );

    // Lấy danh sách thành viên nhóm
    @GET("groups/{groupId}/members")
    Call<ApiResponse<List<GroupMember>>> getGroupMembers(@Path("groupId") String groupId);

    // Kick thành viên (admin only)
    @DELETE("groups/{groupId}/members/{memberId}")
    Call<ApiResponse<Object>> kickMember(
            @Path("groupId") String groupId,
            @Path("memberId") String memberId
    );

    // ====== LIVE ======
    @POST("live/start")
    Call<ApiResponse<LiveModel>> startLive(@Body LiveRequest request);

    @GET("live/active")
    Call<ApiResponse<List<LiveModel>>> getActiveLives();

    @PUT("live/end/{liveId}")
    Call<ApiResponse<Void>> endLive(@Path("liveId") String liveId);
}