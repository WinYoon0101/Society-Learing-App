package com.example.frontend.data.repository;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Group;
import com.example.frontend.data.model.GroupDetail;
import com.example.frontend.data.model.GroupInvitation;
import com.example.frontend.data.model.GroupMember;
import com.example.frontend.data.model.GroupPost;
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

    public void createGroup(String groupName, String privacy, File avatarFile,
                            MutableLiveData<Result<Group>> liveData) {
        liveData.postValue(Result.loading());
        RequestBody nameBody = body(groupName);
        RequestBody descBody = body("");
        RequestBody privacyBody = body(privacy);
        MultipartBody.Part avatarPart = null;
        if (avatarFile != null && avatarFile.exists()) {
            RequestBody fileBody = RequestBody.create(MediaType.parse("image/*"), avatarFile);
            avatarPart = MultipartBody.Part.createFormData("file", avatarFile.getName(), fileBody);
        }
        apiService.createGroup(nameBody, descBody, privacyBody, avatarPart)
                .enqueue(new Callback<ApiResponse<Group>>() {
                    @Override public void onResponse(Call<ApiResponse<Group>> call, Response<ApiResponse<Group>> r) {
                        if (ok(r)) liveData.postValue(Result.success(r.body().getData()));
                        else liveData.postValue(Result.error(msg(r, "Tạo nhóm thất bại")));
                    }
                    @Override public void onFailure(Call<ApiResponse<Group>> call, Throwable t) {
                        liveData.postValue(Result.error(t.getMessage()));
                    }
                });
    }

    public void getMyGroups(MutableLiveData<Result<List<Group>>> liveData) {
        liveData.postValue(Result.loading());
        apiService.getMyGroups().enqueue(new Callback<ApiResponse<List<Group>>>() {
            @Override public void onResponse(Call<ApiResponse<List<Group>>> call, Response<ApiResponse<List<Group>>> r) {
                if (ok(r)) liveData.postValue(Result.success(r.body().getData()));
                else liveData.postValue(Result.error(msg(r, "Không tải được danh sách nhóm"), null));
            }
            @Override public void onFailure(Call<ApiResponse<List<Group>>> call, Throwable t) {
                liveData.postValue(Result.error(t.getMessage(), null));
            }
        });
    }

    public void getGroupFeedPosts(int page, int limit, MutableLiveData<Result<List<GroupPost>>> liveData) {
        liveData.postValue(Result.loading());
        apiService.getGroupFeedPosts(page, limit).enqueue(new Callback<ApiResponse<List<GroupPost>>>() {
            @Override public void onResponse(Call<ApiResponse<List<GroupPost>>> call, Response<ApiResponse<List<GroupPost>>> r) {
                if (ok(r)) liveData.postValue(Result.success(r.body().getData()));
                else liveData.postValue(Result.error(msg(r, "Không tải được bài viết"), null));
            }
            @Override public void onFailure(Call<ApiResponse<List<GroupPost>>> call, Throwable t) {
                liveData.postValue(Result.error(t.getMessage(), null));
            }
        });
    }

    public void discoverGroups(String search, int page, int limit,
                               MutableLiveData<Result<List<Group>>> liveData) {
        liveData.postValue(Result.loading());
        apiService.discoverGroups(search, page, limit).enqueue(new Callback<ApiResponse<List<Group>>>() {
            @Override public void onResponse(Call<ApiResponse<List<Group>>> call, Response<ApiResponse<List<Group>>> r) {
                if (ok(r)) liveData.postValue(Result.success(r.body().getData()));
                else liveData.postValue(Result.error(msg(r, "Không tải được danh sách nhóm"), null));
            }
            @Override public void onFailure(Call<ApiResponse<List<Group>>> call, Throwable t) {
                liveData.postValue(Result.error(t.getMessage(), null));
            }
        });
    }

    public void getInvitations(MutableLiveData<Result<List<GroupInvitation>>> liveData) {
        liveData.postValue(Result.loading());
        apiService.getGroupInvitations().enqueue(new Callback<ApiResponse<List<GroupInvitation>>>() {
            @Override public void onResponse(Call<ApiResponse<List<GroupInvitation>>> call, Response<ApiResponse<List<GroupInvitation>>> r) {
                if (ok(r)) liveData.postValue(Result.success(r.body().getData()));
                else liveData.postValue(Result.error(msg(r, "Không tải được lời mời"), null));
            }
            @Override public void onFailure(Call<ApiResponse<List<GroupInvitation>>> call, Throwable t) {
                liveData.postValue(Result.error(t.getMessage(), null));
            }
        });
    }

    public void respondToInvitation(String invitationId, String action,
                                    MutableLiveData<Result<Object>> liveData) {
        liveData.postValue(Result.loading());
        Map<String, String> body = new HashMap<>();
        body.put("action", action);
        apiService.respondToInvitation(invitationId, body).enqueue(new Callback<ApiResponse<Object>>() {
            @Override public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> r) {
                if (ok(r)) liveData.postValue(Result.success(null));
                else liveData.postValue(Result.error(msg(r, "Thao tác thất bại")));
            }
            @Override public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                liveData.postValue(Result.error(t.getMessage()));
            }
        });
    }

    public void getGroupDetail(String groupId, MutableLiveData<Result<GroupDetail>> liveData) {
        liveData.postValue(Result.loading());
        apiService.getGroupDetail(groupId).enqueue(new Callback<ApiResponse<GroupDetail>>() {
            @Override public void onResponse(Call<ApiResponse<GroupDetail>> call, Response<ApiResponse<GroupDetail>> r) {
                if (ok(r)) liveData.postValue(Result.success(r.body().getData()));
                else liveData.postValue(Result.error(msg(r, "Không tải được thông tin nhóm")));
            }
            @Override public void onFailure(Call<ApiResponse<GroupDetail>> call, Throwable t) {
                liveData.postValue(Result.error(t.getMessage()));
            }
        });
    }

    public void updateGroup(String groupId, String groupName, String description, String privacy,
                            File avatarFile, MutableLiveData<Result<GroupDetail>> liveData) {
        liveData.postValue(Result.loading());
        RequestBody nameBody = body(groupName != null ? groupName : "");
        RequestBody descBody = body(description != null ? description : "");
        RequestBody privacyBody = body(privacy != null ? privacy : "");
        MultipartBody.Part avatarPart = null;
        if (avatarFile != null && avatarFile.exists()) {
            RequestBody fileBody = RequestBody.create(MediaType.parse("image/*"), avatarFile);
            avatarPart = MultipartBody.Part.createFormData("file", avatarFile.getName(), fileBody);
        }
        apiService.updateGroup(groupId, nameBody, descBody, privacyBody, avatarPart)
                .enqueue(new Callback<ApiResponse<GroupDetail>>() {
                    @Override public void onResponse(Call<ApiResponse<GroupDetail>> call, Response<ApiResponse<GroupDetail>> r) {
                        if (ok(r)) liveData.postValue(Result.success(r.body().getData()));
                        else liveData.postValue(Result.error(msg(r, "Cập nhật thất bại")));
                    }
                    @Override public void onFailure(Call<ApiResponse<GroupDetail>> call, Throwable t) {
                        liveData.postValue(Result.error(t.getMessage()));
                    }
                });
    }

    public void joinGroup(String groupId, MutableLiveData<Result<Object>> liveData) {
        liveData.postValue(Result.loading());
        apiService.joinGroup(groupId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> r) {
                if (ok(r)) liveData.postValue(Result.success(null));
                else liveData.postValue(Result.error(msg(r, "Tham gia thất bại")));
            }
            @Override public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                liveData.postValue(Result.error(t.getMessage()));
            }
        });
    }

    public void getPostsByGroup(String groupId, int page, int limit,
                                MutableLiveData<Result<List<GroupPost>>> liveData) {
        liveData.postValue(Result.loading());
        apiService.getPostsByGroup(groupId, page, limit).enqueue(new Callback<ApiResponse<List<GroupPost>>>() {
            @Override public void onResponse(Call<ApiResponse<List<GroupPost>>> call, Response<ApiResponse<List<GroupPost>>> r) {
                if (ok(r)) liveData.postValue(Result.success(r.body().getData()));
                else liveData.postValue(Result.error(msg(r, "Không tải được bài viết"), null));
            }
            @Override public void onFailure(Call<ApiResponse<List<GroupPost>>> call, Throwable t) {
                liveData.postValue(Result.error(t.getMessage(), null));
            }
        });
    }

    // ── THÀNH VIÊN ───────────────────────────────────────────────────────────
    public void getGroupMembers(String groupId, MutableLiveData<Result<List<GroupMember>>> liveData) {
        liveData.postValue(Result.loading());
        apiService.getGroupMembers(groupId).enqueue(new Callback<ApiResponse<List<GroupMember>>>() {
            @Override public void onResponse(Call<ApiResponse<List<GroupMember>>> call, Response<ApiResponse<List<GroupMember>>> r) {
                if (ok(r)) liveData.postValue(Result.success(r.body().getData()));
                else liveData.postValue(Result.error(msg(r, "Không tải được danh sách thành viên"), null));
            }
            @Override public void onFailure(Call<ApiResponse<List<GroupMember>>> call, Throwable t) {
                liveData.postValue(Result.error(t.getMessage(), null));
            }
        });
    }

    public void kickMember(String groupId, String memberId, MutableLiveData<Result<Object>> liveData) {
        liveData.postValue(Result.loading());
        apiService.kickMember(groupId, memberId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> r) {
                if (ok(r)) liveData.postValue(Result.success(null));
                else liveData.postValue(Result.error(msg(r, "Kick thành viên thất bại")));
            }
            @Override public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                liveData.postValue(Result.error(t.getMessage()));
            }
        });
    }

    public void sendGroupInvitation(String groupId, String inviteeId,
                                    MutableLiveData<Result<Object>> liveData) {
        liveData.postValue(Result.loading());
        Map<String, String> body = new HashMap<>();
        body.put("groupId", groupId);
        body.put("inviteeId", inviteeId);
        apiService.sendGroupInvitation(body).enqueue(new Callback<ApiResponse<Object>>() {
            @Override public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> r) {
                if (ok(r)) liveData.postValue(Result.success(null));
                else liveData.postValue(Result.error(msg(r, "Gửi lời mời thất bại")));
            }
            @Override public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                liveData.postValue(Result.error(t.getMessage()));
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static RequestBody body(String text) {
        return RequestBody.create(MediaType.parse("text/plain"), text);
    }

    private static <T> boolean ok(Response<ApiResponse<T>> r) {
        return r.isSuccessful() && r.body() != null && r.body().isSuccess();
    }

    private static <T> String msg(Response<ApiResponse<T>> r, String fallback) {
        if (r.body() != null && r.body().getMessage() != null) return r.body().getMessage();
        return fallback;
    }
}
