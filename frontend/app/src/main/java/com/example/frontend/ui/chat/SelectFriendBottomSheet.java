package com.example.frontend.ui.chat;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Friend;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SelectFriendBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "SelectFriendBottomSheet";
    private static final String LOG_TAG = "SelectFriendSheet";

    private RecyclerView rvFriends;
    private View layoutEmpty;
    private TextView tvEmpty;
    private ProgressBar progress;
    private EditText etSearch;

    private FriendPickAdapter adapter;
    private final List<Friend> allFriends = new ArrayList<>();
    private Call<ApiResponse<List<Friend>>> pendingCall;

    public static SelectFriendBottomSheet newInstance() {
        return new SelectFriendBottomSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_select_friend, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton btnClose = view.findViewById(R.id.btnSelectFriendClose);
        ImageButton btnCreateGroup = view.findViewById(R.id.btnSelectFriendCreateGroup);
        etSearch = view.findViewById(R.id.etSearchFriend);
        rvFriends = view.findViewById(R.id.rvFriendsPick);
        layoutEmpty = view.findViewById(R.id.layoutEmptyFriendPick);
        tvEmpty = view.findViewById(R.id.tvEmptyFriendPick);
        progress = view.findViewById(R.id.progressFriendPick);

        btnClose.setOnClickListener(v -> dismiss());

        btnCreateGroup.setOnClickListener(v -> {
            Fragment parent = getParentFragment();
            dismiss();
            if (parent instanceof ChatFragment) {
                ((ChatFragment) parent).openCreateGroup();
            }
        });

        adapter = new FriendPickAdapter(this::onFriendPicked);
        rvFriends.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFriends.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                applyFilter(s == null ? "" : s.toString());
            }
        });

        fetchFriends();
    }

    private void fetchFriends() {
        showLoading();

        ApiService api = ApiClient.getApiService(requireContext().getApplicationContext());
        pendingCall = api.getFriends();
        pendingCall.enqueue(new Callback<ApiResponse<List<Friend>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Friend>>> call,
                                   @NonNull Response<ApiResponse<List<Friend>>> response) {
                if (!isAdded()) return;
                ApiResponse<List<Friend>> body = response.body();
                if (response.isSuccessful() && body != null && body.isSuccess() && body.getData() != null) {
                    allFriends.clear();
                    allFriends.addAll(body.getData());
                    applyFilter(etSearch.getText() == null ? "" : etSearch.getText().toString());
                } else {
                    String msg = body != null ? body.getMessage() : "Lỗi tải bạn bè";
                    Log.w(LOG_TAG, "getFriends failed: " + msg);
                    showEmpty("Không tải được danh sách bạn bè");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Friend>>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Log.e(LOG_TAG, "getFriends error", t);
                showEmpty("Không tải được danh sách bạn bè");
            }
        });
    }

    private void applyFilter(String query) {
        String q = query.trim().toLowerCase(Locale.getDefault());
        List<Friend> filtered;
        if (q.isEmpty()) {
            filtered = new ArrayList<>(allFriends);
        } else {
            filtered = new ArrayList<>();
            for (Friend f : allFriends) {
                String name = f.getUsername();
                if (name != null && name.toLowerCase(Locale.getDefault()).contains(q)) {
                    filtered.add(f);
                }
            }
        }

        if (filtered.isEmpty()) {
            showEmpty(allFriends.isEmpty() ? "Chưa có bạn bè" : "Không tìm thấy bạn bè phù hợp");
        } else {
            showList(filtered);
        }
    }

    private void showLoading() {
        progress.setVisibility(View.VISIBLE);
        rvFriends.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
    }

    private void showList(List<Friend> list) {
        progress.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        rvFriends.setVisibility(View.VISIBLE);
        adapter.submitList(list);
    }

    private void showEmpty(String message) {
        progress.setVisibility(View.GONE);
        rvFriends.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
        if (tvEmpty != null && message != null) {
            tvEmpty.setText(message);
        }
    }

    private void onFriendPicked(Friend friend) {
        if (friend == null || friend.getId() == null) return;
        String friendId = friend.getId();

        Fragment parent = getParentFragment();
        dismiss();

        if (parent instanceof ChatFragment) {
            ((ChatFragment) parent).openChatWith(friendId);
        } else {
            Log.w(LOG_TAG, "Parent fragment is not ChatFragment, cannot open chat");
        }
    }

    @Override
    public void onDestroyView() {
        if (pendingCall != null) {
            pendingCall.cancel();
            pendingCall = null;
        }
        super.onDestroyView();
    }
}