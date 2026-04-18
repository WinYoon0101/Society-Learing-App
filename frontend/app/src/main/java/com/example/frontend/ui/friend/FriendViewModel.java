package com.example.frontend.ui.friend;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.AndroidViewModel;
import com.example.frontend.data.model.Friend;
import com.example.frontend.data.repository.FriendRepository;
import com.example.frontend.utils.Result;
import java.util.List;

public class FriendViewModel extends AndroidViewModel {
    private FriendRepository repository;

    // Biến lưu trữ danh sách Pending
    private MutableLiveData<Result<List<Friend>>> pendingRequestsResult = new MutableLiveData<>();
    // Biến lưu trữ kết quả của hành động Chấp nhận/Xóa
    private MutableLiveData<Result<Object>> actionResult = new MutableLiveData<>();

    // Biến lưu trữ danh sách Gợi ý (nếu có)
    private MutableLiveData<Result<List<Friend>>> suggestionsResult = new MutableLiveData<>();

    public LiveData<Result<List<Friend>>> getSuggestionsResult() {
        return suggestionsResult;
    }

    public FriendViewModel(@NonNull Application application) {
        super(application);
        // Truyền context vào Repository
        repository = new FriendRepository(application.getApplicationContext());
    }

    public LiveData<Result<List<Friend>>> getPendingRequestsResult() {
        return pendingRequestsResult;
    }

    public LiveData<Result<Object>> getActionResult() {
        return actionResult;
    }

    // Gọi lên Repo
    public void fetchPendingRequests() {
        repository.getPendingRequests(pendingRequestsResult);
    }

    public void acceptRequest(String userId) {
        repository.acceptFriendRequest(userId, actionResult);
    }

    public void fetchFriendSuggestions() {
        repository.getFriendSuggestions(suggestionsResult);
    }

    public void sendFriendRequest(String userId) {
        repository.sendFriendRequest(userId, actionResult);
    }

    public void declineRequest(String userId) {
        repository.declineFriendRequest(userId, actionResult); // Dùng chung actionResult
    }
}