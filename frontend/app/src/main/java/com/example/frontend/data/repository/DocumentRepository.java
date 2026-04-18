package com.example.frontend.data.repository;

import android.content.Context;
import androidx.lifecycle.MutableLiveData;

import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Document;
import com.example.frontend.data.model.DocumentListData;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.utils.Result;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DocumentRepository {
    private final ApiService apiService;

    public DocumentRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
    }

    // 1. Lấy danh sách tài liệu công khai
    public void getPublicDocuments(int page, String search, String subject, String sortBy,
                                   MutableLiveData<Result<List<Document>>> resultData) {
        resultData.setValue(Result.loading());
        apiService.getDocuments(page, search, subject, sortBy).enqueue(new Callback<ApiResponse<DocumentListData>>() {
            @Override
            public void onResponse(Call<ApiResponse<DocumentListData>> call, Response<ApiResponse<DocumentListData>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    resultData.setValue(Result.success(response.body().getData().getDocuments()));
                } else {
                    resultData.setValue(Result.error("Không tìm thấy tài liệu"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<DocumentListData>> call, Throwable t) {
                resultData.setValue(Result.error("Lỗi kết nối Server!"));
            }
        });
    }

    // 2. Tạo tài liệu mới
    public void createDocument(Map<String, Object> docData, MutableLiveData<Result<Document>> resultData) {
        resultData.setValue(Result.loading());
        apiService.createDocument(docData).enqueue(new Callback<ApiResponse<Document>>() {
            @Override
            public void onResponse(Call<ApiResponse<Document>> call, Response<ApiResponse<Document>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    resultData.setValue(Result.success(response.body().getData()));
                } else {
                    resultData.setValue(Result.error("Tạo tài liệu thất bại"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Document>> call, Throwable t) {
                resultData.setValue(Result.error("Lỗi mạng!"));
            }
        });
    }

    // 3. Toggle Save
    public void toggleSaveDocument(String docId, MutableLiveData<Result<Boolean>> resultData) {
        apiService.toggleSave(docId).enqueue(new Callback<ApiResponse<Map<String, Boolean>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Boolean>>> call, Response<ApiResponse<Map<String, Boolean>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    Boolean isSaved = response.body().getData().get("saved");
                    resultData.setValue(Result.success(isSaved != null && isSaved));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Boolean>>> call, Throwable t) {
                resultData.setValue(Result.error("Lỗi thao tác!"));
            }
        });
    }

    // 4. Lấy danh sách đã lưu
    public void getSavedDocuments(MutableLiveData<Result<List<Document>>> resultData) {
        resultData.setValue(Result.loading());
        apiService.getSavedDocuments(1, 20).enqueue(new Callback<ApiResponse<DocumentListData>>() {
            @Override
            public void onResponse(Call<ApiResponse<DocumentListData>> call, Response<ApiResponse<DocumentListData>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    resultData.setValue(Result.success(response.body().getData().getDocuments()));
                } else {
                    resultData.setValue(Result.error("Không lấy được danh sách lưu"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<DocumentListData>> call, Throwable t) {
                resultData.setValue(Result.error("Lỗi kết nối!"));
            }
        });
    }

    // 5. Xóa tài liệu
    public void deleteDocument(String docId, MutableLiveData<Result<String>> resultData) {
        apiService.deleteDocument(docId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    resultData.setValue(Result.success("Xóa thành công"));
                } else {
                    resultData.setValue(Result.error("Không có quyền xóa hoặc lỗi server"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                resultData.setValue(Result.error("Lỗi hệ thống!"));
            }
        });
    }



    // 2. Hàm cập nhật tài liệu (Sửa title, môn học hoặc đổi file mới)
    public void updateDocument(String docId, Map<String, Object> updates, MutableLiveData<Result<Document>> resultData) {
        resultData.setValue(Result.loading());
        apiService.updateDocument(docId, updates).enqueue(new Callback<ApiResponse<Document>>() {
            @Override
            public void onResponse(Call<ApiResponse<Document>> call, Response<ApiResponse<Document>> response) {
                if (response.isSuccessful()) resultData.setValue(Result.success(response.body().getData()));
                else resultData.setValue(Result.error("Cập nhật thất bại"));
            }
            @Override
            public void onFailure(Call<ApiResponse<Document>> call, Throwable t) {
                resultData.setValue(Result.error("Lỗi mạng"));
            }
        });
    }
// 3. Hàm tăng lượt tải (Gọi khi user nhấn nút Download)
    public void incrementDownloadCount(String docId) {
        // Gọi API incrementDownload đã khai báo trong ApiService
        apiService.incrementDownload(docId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    // Thành công: Lượt tải đã được tăng trên Server
                    android.util.Log.d("REPO", "Đã tăng lượt tải cho: " + docId);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                // Thất bại: Lỗi mạng hoặc server
                android.util.Log.e("REPO", "Lỗi tăng lượt tải: " + t.getMessage());
            }
        });
    }

    // 3. Hàm tăng lượt xem (Gọi khi user nhấn vào xem tài liệu)
    public void incrementViewCount(String docId) {
        // Backend của bạn tự tăng view khi gọi API lấy chi tiết tài liệu
        apiService.getDocumentById(docId).enqueue(new Callback<ApiResponse<Document>>() {
            @Override
            public void onResponse(Call<ApiResponse<Document>> call, Response<ApiResponse<Document>> response) {
                if (response.isSuccessful()) {
                    android.util.Log.d("REPO", "Đã tăng 1 view cho: " + docId);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Document>> call, Throwable t) {
                android.util.Log.e("REPO", "Lỗi tăng view: " + t.getMessage());
            }
        });
    }
}