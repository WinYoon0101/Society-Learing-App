package com.example.frontend.ui.feed;

import android.content.Context;
import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Post;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.utils.FileUtils;
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
    private final MutableLiveData<Boolean> isSuccess = new MutableLiveData<>(null);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getIsSuccess() { return isSuccess; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    /** Gọi sau khi đã xử lý success để tránh re-trigger khi quay lại Fragment */
    public void resetSuccess() { isSuccess.setValue(null); }

    /**
     * @param groupId  null hoặc "" nếu đăng lên feed cá nhân, truyền groupId nếu đăng vào nhóm
     */
    public void uploadPost(Context context, String content, List<Uri> imageUris, String groupId, List<String> tagUserIds, String initialReaction) {
        isLoading.setValue(true);

        RequestBody contentBody = RequestBody.create(MediaType.parse("text/plain"),
                content != null ? content : "");
        RequestBody privacyBody = RequestBody.create(MediaType.parse("text/plain"), "Public");
        RequestBody groupIdBody = RequestBody.create(MediaType.parse("text/plain"),
                groupId != null ? groupId : "");
        RequestBody tagsBody = RequestBody.create(MediaType.parse("text/plain"),
                tagUserIds != null && !tagUserIds.isEmpty() ? new com.google.gson.Gson().toJson(tagUserIds) : "");
        RequestBody reactionBody = RequestBody.create(MediaType.parse("text/plain"),
                initialReaction != null ? initialReaction : "");

        List<MultipartBody.Part> imageParts = new ArrayList<>();
        if (imageUris != null && !imageUris.isEmpty()) {
            for (Uri uri : imageUris) {
                File file = FileUtils.getFileFromUri(context, uri);
                if (file != null) {
                    String mimeType = context.getContentResolver().getType(uri);
                    if (mimeType == null) mimeType = "image/*";
                    RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), file);
                    imageParts.add(MultipartBody.Part.createFormData("images", file.getName(), requestFile));
                }
            }
        }

        ApiClient.getApiService(context)
                .createPost(contentBody, privacyBody, groupIdBody, tagsBody, reactionBody, imageParts)
                .enqueue(new Callback<ApiResponse<Post>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Post>> call, Response<ApiResponse<Post>> response) {
                        isLoading.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            isSuccess.setValue(true);
                        } else {
                            errorMessage.setValue("Lỗi server: " + response.code());
                        }
                    }
                    @Override
                    public void onFailure(Call<ApiResponse<Post>> call, Throwable t) {
                        isLoading.setValue(false);
                        errorMessage.setValue("Lỗi kết nối: " + t.getMessage());
                    }
                });
    }

    // Overload không có groupId – giữ nguyên cho feed cá nhân
    public void uploadPost(Context context, String content, List<Uri> imageUris) {
        uploadPost(context, content, imageUris, null, null, null);
    }
}