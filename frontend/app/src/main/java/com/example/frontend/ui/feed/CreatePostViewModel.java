package com.example.frontend.ui.feed;

import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Post;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.utils.FileUtils;
import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreatePostViewModel extends ViewModel {

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isSuccess = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<Boolean> getIsSuccess() {
        return isSuccess;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void resetSuccess() {
        isSuccess.setValue(false);
    }

    // ĐÃ BỔ SUNG THAM SỐ: String privacy (Nằm ở vị trí thứ 3)
    public void uploadPost(Context context, String content, String privacy, List<Uri> imageUris, String groupId, List<String> tagIds, String initialReaction) {
        isLoading.setValue(true);

        ApiService apiService = ApiClient.getApiService(context);

        // 1. Đóng gói Nội dung và Quyền riêng tư (Privacy)
        RequestBody contentBody = RequestBody.create(MediaType.parse("text/plain"), content != null ? content : "");
        RequestBody privacyBody = RequestBody.create(MediaType.parse("text/plain"), privacy != null ? privacy : "Public");

        // 2. Đóng gói Nhóm, Tag và Cảm xúc (Nếu có)
        RequestBody groupIdBody = (groupId != null && !groupId.isEmpty()) ? RequestBody.create(MediaType.parse("text/plain"), groupId) : null;
        RequestBody tagsBody = (tagIds != null && !tagIds.isEmpty()) ? RequestBody.create(MediaType.parse("text/plain"), new Gson().toJson(tagIds)) : null;
        RequestBody reactionBody = (initialReaction != null && !initialReaction.isEmpty()) ? RequestBody.create(MediaType.parse("text/plain"), initialReaction) : null;

        // 3. Xử lý đóng gói mảng Hình ảnh (Multipart)
        List<MultipartBody.Part> imageParts = new ArrayList<>();
        if (imageUris != null && !imageUris.isEmpty()) {
            for (Uri uri : imageUris) {
                try {
                    File file = FileUtils.getFileFromUri(context, uri);
                    if (file != null) {
                        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
                        MultipartBody.Part body = MultipartBody.Part.createFormData("images", file.getName(), requestFile);
                        imageParts.add(body);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // 4. Gắn đủ 6 gói hàng vào API và gửi lên Server Backend
        apiService.createPost(contentBody, privacyBody, groupIdBody, tagsBody, reactionBody, imageParts).enqueue(new Callback<ApiResponse<Post>>() {
            @Override
            public void onResponse(Call<ApiResponse<Post>> call, Response<ApiResponse<Post>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    isSuccess.setValue(true);
                } else {
                    errorMessage.setValue("Lỗi khi đăng bài: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Post>> call, Throwable t) {
                isLoading.setValue(false);
                errorMessage.setValue("Lỗi mạng: " + t.getMessage());
            }
        });
    }
}