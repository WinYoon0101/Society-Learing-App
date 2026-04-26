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
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreatePostViewModel extends ViewModel {

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getIsSuccess() { return isSuccess; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void uploadPost(Context context, String content, Uri imageUri) {
        isLoading.setValue(true);

        // 1. Gói nội dung chữ
        RequestBody contentBody = RequestBody.create(MediaType.parse("text/plain"), content);

        MultipartBody.Part imagePart = null;
        if (imageUri != null) {
            // Sửa tên biến từ uri -> imageUri cho khớp tham số
            File file = FileUtils.getFileFromUri(context, imageUri);

            if (file != null) {
                RequestBody requestFile = RequestBody.create(
                        MediaType.parse(context.getContentResolver().getType(imageUri)),
                        file
                );
                imagePart = MultipartBody.Part.createFormData("media", file.getName(), requestFile);
            }
        }

        // 3. Gọi Retrofit
        ApiClient.getApiService(context).createPost(contentBody, imagePart).enqueue(new Callback<ApiResponse<Post>>() {
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
}