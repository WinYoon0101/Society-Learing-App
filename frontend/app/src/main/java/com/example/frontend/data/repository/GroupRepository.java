package com.example.frontend.data.repository;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Group;
import com.example.frontend.data.model.GroupInvitation;
import com.example.frontend.data.model.Post;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.utils.Result;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroupRepository {

    private final ApiService apiService;

    public GroupRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
    }

    // Tạo nhóm mới — avatarFile có thể null nếu người dùng không chọn ảnh
    public void createGroup(String groupName, String privacy,
                            File avatarFile,
                            MutableLiveData<Result<Group>> resultLiveData) {
        resultLiveData.postValue(Result.loading());

        RequestBody nameBody = RequestBody.create(MediaType.parse("text/plain"), groupName);
        RequestBody descBody = RequestBody.create(MediaType.parse("text/plain"), "");
        RequestBody privacyBody = RequestBody.create(MediaType.parse("text/plain"), privacy);

        MultipartBody.Part avatarPart = null;
        if (avatarFile != null && avatarFile.exists()) {
            RequestBody fileBody = RequestBody.create(
                    MediaType.parse("image/*"), avatarFile);
            avatarPart = MultipartBody.Part.createFormData("images", avatarFile.getName(), fileBody);
        }

        apiService.createGroup(nameBody, descBody, privacyBody, avatarPart)
                .enqueue(new Callback<ApiResponse<Group>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Group>> call,
                                           Response<ApiResponse<Group>> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()) {
                            resultLiveData.postValue(Result.success(response.body().getData()));
                        } else {
                            String msg = (response.body() != null && response.body().getMessage() != null)
                                    ? response.body().getMessage()
                                    : "Tạo nhóm thất bại";
                            resultLiveData.postValue(Result.error(msg));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Group>> call, Throwable t) {
                        resultLiveData.postValue(Result.error(t.getMessage()));
                    }
                });
    }

    // Tab "Bài viết"
    public void getGroupPosts(int page, int limit, MutableLiveData<Result<List<Post>>> liveData) {
        liveData.postValue(Result.loading());
        apiService.getGroupPosts(page, limit).enqueue(new Callback<ApiResponse<List<Post>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Post>>> call, Response<ApiResponse<List<Post>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    liveData.postValue(Result.success(response.body().getData()));
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "Không tải được bài viết";
                    liveData.postValue(Result.error(msg));
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Post>>> call, Throwable t) {
                liveData.postValue(Result.error(t.getMessage()));
            }
        });
    }

    // Tab "Khám phá"
    public void discoverGroups(String search, int page, int limit, MutableLiveData<Result<List<Group>>> liveData) {
        liveData.postValue(Result.loading());
        String q = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        apiService.discoverGroups(q, page, limit).enqueue(new Callback<ApiResponse<List<Group>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Group>>> call, Response<ApiResponse<List<Group>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    liveData.postValue(Result.success(response.body().getData()));
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "Không tải được nhóm";
                    liveData.postValue(Result.error(msg));
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Group>>> call, Throwable t) {
                liveData.postValue(Result.error(t.getMessage()));
            }
        });
    }

    // Tham gia nhóm Public
    public void joinGroup(String groupId, MutableLiveData<Result<Void>> liveData) {
        liveData.postValue(Result.loading());
        apiService.joinPublicGroup(groupId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    liveData.postValue(Result.success(null));
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "Tham gia nhóm thất bại";
                    liveData.postValue(Result.error(msg));
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                liveData.postValue(Result.error(t.getMessage()));
            }
        });
    }

    // Tab "Lời mời"
    public void getInvitations(MutableLiveData<Result<List<GroupInvitation>>> liveData) {
        liveData.postValue(Result.loading());
        apiService.getGroupInvitations().enqueue(new Callback<ApiResponse<List<GroupInvitation>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<GroupInvitation>>> call, Response<ApiResponse<List<GroupInvitation>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    liveData.postValue(Result.success(response.body().getData()));
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "Không tải được lời mời";
                    liveData.postValue(Result.error(msg));
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<GroupInvitation>>> call, Throwable t) {
                liveData.postValue(Result.error(t.getMessage()));
            }
        });
    }

    // Phản hồi lời mời
    public void respondToInvitation(String invitationId, String action, MutableLiveData<Result<Void>> liveData) {
        liveData.postValue(Result.loading());
        Map<String, String> body = new HashMap<>();
        body.put("action", action);
        apiService.respondToInvitation(invitationId, body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    liveData.postValue(Result.success(null));
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "Thao tác thất bại";
                    liveData.postValue(Result.error(msg));
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                liveData.postValue(Result.error(t.getMessage()));
            }
        });
    }

    // Tab "Nhóm của bạn"
    public void getMyGroups(MutableLiveData<Result<List<Group>>> resultLiveData) {
        resultLiveData.postValue(Result.loading());

        apiService.getMyGroups().enqueue(new Callback<ApiResponse<List<Group>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Group>>> call,
                                   Response<ApiResponse<List<Group>>> response) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().isSuccess()) {
                    resultLiveData.postValue(Result.success(response.body().getData()));
                } else {
                    String msg = (response.body() != null && response.body().getMessage() != null)
                            ? response.body().getMessage()
                            : "Không tải được danh sách nhóm";
                    resultLiveData.postValue(Result.error(msg, null));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Group>>> call, Throwable t) {
                resultLiveData.postValue(Result.error(t.getMessage(), null));
            }
        });
    }
}
