package com.example.frontend.ui.docs;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.frontend.data.model.DocumentListData;
import com.example.frontend.data.repository.DocumentRepository;
import com.example.frontend.utils.Result;

public class DocsViewModel extends AndroidViewModel {

    private final DocumentRepository repository;

    // LiveData dùng để lưu trữ và quan sát kết quả trả về từ API
    // Kết quả bao gồm danh sách tài liệu và thông tin phân trang (pagination)
    private final MutableLiveData<Result<DocumentListData>> myDocsResult = new MutableLiveData<>();

    public DocsViewModel(@NonNull Application application) {
        super(application);
        // Khởi tạo Repository để giao tiếp với ApiService
        repository = new DocumentRepository(application);
    }

    /**
     * Getter để Activity có thể quan sát (observe) dữ liệu.
     * Dùng LiveData thay vì MutableLiveData để đảm bảo Activity không thay đổi trực tiếp dữ liệu.
     */
    public LiveData<Result<DocumentListData>> getMyDocsResult() {
        return myDocsResult;
    }

    /**
     * Hàm gọi xuống Repository để bắt đầu quá trình lấy danh sách tài liệu cá nhân.
     * @param page Trang hiện tại (thường bắt đầu từ 1)
     * @param limit Số lượng tài liệu trên mỗi trang
     */
    public void fetchMyDocuments(int page, int limit) {
        repository.getMyDocuments(page, limit, myDocsResult);
    }
}