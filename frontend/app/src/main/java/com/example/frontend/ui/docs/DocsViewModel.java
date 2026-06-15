package com.example.frontend.ui.docs;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.frontend.data.model.Document;
import com.example.frontend.data.model.DocumentListData;
import com.example.frontend.data.repository.DocumentRepository;
import com.example.frontend.utils.Result;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocsViewModel extends AndroidViewModel {

    private final DocumentRepository repository;

    // LiveData cho Tài liệu đã đăng
    private final MutableLiveData<Result<DocumentListData>> myDocsResult = new MutableLiveData<>();
    private final MutableLiveData<Result<String>> deleteResult = new MutableLiveData<>();
    private final MutableLiveData<Result<Document>> updateResult = new MutableLiveData<>();

    // LiveData cho Tài liệu đã lưu
    private final MutableLiveData<Result<List<Document>>> savedDocsResult = new MutableLiveData<>();
    private final MutableLiveData<Result<Boolean>> toggleSaveResult = new MutableLiveData<>();

    public DocsViewModel(@NonNull Application application) {
        super(application);
        repository = new DocumentRepository(application);
    }

    public LiveData<Result<DocumentListData>> getMyDocsResult() { return myDocsResult; }
    public LiveData<Result<String>> getDeleteResult() { return deleteResult; }
    public LiveData<Result<Document>> getUpdateResult() { return updateResult; }

    public LiveData<Result<List<Document>>> getSavedDocsResult() { return savedDocsResult; }
    public LiveData<Result<Boolean>> getToggleSaveResult() { return toggleSaveResult; }

    // --- LOGIC ĐÃ ĐĂNG ---
    public void fetchMyDocuments(int page, int limit) {
        repository.getMyDocuments(page, limit, myDocsResult);
    }

    public void deleteDocument(String docId) {
        repository.deleteDocument(docId, deleteResult);
    }

    public void updateDocument(String docId, String newTitle, String newSubject) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", newTitle);
        updates.put("subject", newSubject);
        repository.updateDocument(docId, updates, updateResult);
    }

    // --- LOGIC ĐÃ LƯU ---
    public void fetchSavedDocuments() {
        repository.getSavedDocuments(savedDocsResult);
    }

    public void toggleSaveDocument(String docId) {
        repository.toggleSaveDocument(docId, toggleSaveResult);
    }
}