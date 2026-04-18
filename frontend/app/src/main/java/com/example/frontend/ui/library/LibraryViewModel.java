package com.example.frontend.ui.library;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel; // PHẢI LÀ AndroidViewModel
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.frontend.data.model.Document;
import com.example.frontend.data.repository.DocumentRepository;
import com.example.frontend.utils.Result;
import java.util.List;

public class LibraryViewModel extends AndroidViewModel { // Đổi ở đây
    private final DocumentRepository repository;
    private final MutableLiveData<Result<List<Document>>> documents = new MutableLiveData<>();

    public LibraryViewModel(@NonNull Application application) {
        super(application); // Lệnh này yêu cầu AndroidViewModel mới chạy được
        // LỖI Ở ĐÂY NẾU THIẾU THAM SỐ: Repository cần 'application' (context)
        repository = new DocumentRepository(application);
    }

    public LiveData<Result<List<Document>>> getDocuments() { return documents; }

    public void loadDocuments(String search) {
        // Đảm bảo truyền đủ 5 tham số như Repository yêu cầu
        repository.getPublicDocuments(1, search, null, "newest", documents);
    }

    public void incrementDownload(String docId) {
        // Gọi xuống repository để xử lý logic đếm lượt tải
        repository.incrementDownloadCount(docId);
    }
}