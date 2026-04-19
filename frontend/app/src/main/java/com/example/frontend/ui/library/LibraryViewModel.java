package com.example.frontend.ui.library;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.frontend.data.model.Document;
import com.example.frontend.data.repository.DocumentRepository;
import com.example.frontend.utils.Result;
import java.util.List;

public class LibraryViewModel extends AndroidViewModel {
    private final DocumentRepository repository;
    private final MutableLiveData<Result<List<Document>>> documents = new MutableLiveData<>();

    // Ghi nhớ trạng thái hiện tại để khi đổi Sort không bị mất Search và ngược lại
    private String currentSearch = "";
    private String currentSort = "newest";
    private String currentSubject = null;  // Để dành cho lọc theo môn học

    public LibraryViewModel(@NonNull Application application) {
        super(application);
        repository = new DocumentRepository(application);
    }

    public LiveData<Result<List<Document>>> getDocuments() {
        return documents;
    }

    /**
     * Hàm gọi khi người dùng gõ tìm kiếm (Search-as-you-type)
     */
    public void loadDocuments(String search) {
        this.currentSearch = search;
        // Gọi Repo với search mới + sort cũ
        executeLoad();
    }

    /**
     * Hàm gọi khi người dùng chọn mục trong PopupMenu Sắp xếp
     */
    public void loadDocumentsWithSort(String sortBy) {
        this.currentSort = sortBy;
        executeLoad();
    }

    // Hàm mới để lọc theo môn học
    public void loadDocumentsBySubject(String subject) {
        this.currentSubject = subject; // Nếu subject là "" hoặc null thì lấy tất cả
        executeLoad();
    }

    /**
     * Hàm trung tâm để thực thi việc gọi Repository
     */
    private void executeLoad() {
        // Luôn truyền đủ các tham số trạng thái hiện tại vào Repository
        // Tham số: (page, search, subject, sortBy, livedata)
        repository.getPublicDocuments(1, currentSearch, currentSubject, currentSort, documents);
    }
//  Hàm này sẽ được gọi khi người dùng nhấn vào nút Download, từ đó tăng lượt tải xuống lên 1
    public void incrementDownload(String docId) {
        repository.incrementDownloadCount(docId);
    }
//  Hàm này sẽ được gọi khi người dùng nhấn vào tên tài liệu để xem chi tiết, từ đó tăng lượt xem lên 1
    public void incrementView(String docId) {
        repository.incrementViewCount(docId);
    }

}