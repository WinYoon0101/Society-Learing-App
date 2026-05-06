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
    private final MutableLiveData<Boolean> isSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getIsSuccess() { return isSuccess; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    // ĐÃ SỬA: Thay đổi tham số cuối từ `Uri imageUri` thành `List<Uri> imageUris`
    public void uploadPost(Context context, String content, List<Uri> imageUris) {
        isLoading.setValue(true);

        // 1. Gói nội dung chữ
        RequestBody contentBody = RequestBody.create(MediaType.parse("text/plain"), content != null ? content : "");

        // 2. Khởi tạo mặc định cho privacy và groupId để không bị thiếu tham số API
        RequestBody privacyBody = RequestBody.create(MediaType.parse("text/plain"), "Public");
        RequestBody groupIdBody = RequestBody.create(MediaType.parse("text/plain"), "");

        // 3. Xử lý danh sách ảnh
        List<MultipartBody.Part> imageParts = new ArrayList<>();
        if (imageUris != null && !imageUris.isEmpty()) {
            for (Uri uri : imageUris) {
                File file = FileUtils.getFileFromUri(context, uri); // Sử dụng đúng hàm của bạn
                if (file != null) {
                    String mimeType = context.getContentResolver().getType(uri);
                    if (mimeType == null) mimeType = "image/*";

                    RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), file);
                    // ĐÃ SỬA: Đổi tên formData thành "images" (quan trọng nhất)
                    imageParts.add(MultipartBody.Part.createFormData("images", file.getName(), requestFile));
                }
            }
        }

        // 4. Gọi Retrofit với đủ 4 tham số như ApiService đã yêu cầu
        ApiClient.getApiService(context).createPost(contentBody, privacyBody, groupIdBody, imageParts).enqueue(new Callback<ApiResponse<Post>>() {
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