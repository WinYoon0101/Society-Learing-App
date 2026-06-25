package com.example.frontend.ui.chat;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Conversation;
import com.example.frontend.data.model.Friend;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Tạo group chat: chọn nhiều bạn (multi-select) + tên tùy chọn → createGroup. */
public class CreateGroupBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "CreateGroupBottomSheet";
    private static final String LOG_TAG = "CreateGroupSheet";
    private static final int MIN_MEMBERS = 2;

    private EditText etGroupName;
    private EditText etSearch;
    private RecyclerView rvFriends;
    private ProgressBar progress;
    private TextView tvEmpty;
    private Button btnCreate;

    private FriendPickAdapter adapter;
    private final List<Friend> allFriends = new ArrayList<>();
    private Call<ApiResponse<List<Friend>>> friendsCall;
    private Call<ApiResponse<Conversation>> createCall;
    private boolean creating = false;

    public static CreateGroupBottomSheet newInstance() {
        return new CreateGroupBottomSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_create_group, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton btnClose = view.findViewById(R.id.btnCreateGroupClose);
        etGroupName = view.findViewById(R.id.etGroupName);
        etSearch = view.findViewById(R.id.etSearchGroupFriend);
        rvFriends = view.findViewById(R.id.rvGroupFriends);
        progress = view.findViewById(R.id.progressGroupFriends);
        tvEmpty = view.findViewById(R.id.tvEmptyGroupFriends);
        btnCreate = view.findViewById(R.id.btnCreateGroup);

        btnClose.setOnClickListener(v -> dismiss());

        adapter = new FriendPickAdapter(friend -> updateCreateButton());
        adapter.setSelectable(true);
        rvFriends.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFriends.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                applyFilter(s == null ? "" : s.toString());
            }
        });

        btnCreate.setOnClickListener(v -> createGroup());

        fetchFriends();
    }

    private void fetchFriends() {
        showLoading();
        ApiService api = ApiClient.getApiService(requireContext().getApplicationContext());
        friendsCall = api.getFriends();
        friendsCall.enqueue(new Callback<ApiResponse<List<Friend>>>() {
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
        tvEmpty.setVisibility(View.GONE);
    }

    private void showList(List<Friend> list) {
        progress.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);
        rvFriends.setVisibility(View.VISIBLE);
        adapter.submitList(list);
    }

    private void showEmpty(String message) {
        progress.setVisibility(View.GONE);
        rvFriends.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText(message);
    }

    private void updateCreateButton() {
        int count = adapter.getSelectedIds().size();
        btnCreate.setEnabled(!creating && count >= MIN_MEMBERS);
        btnCreate.setText(count >= MIN_MEMBERS ? "Tạo nhóm (" + count + ")" : "Tạo nhóm");
    }

    private void createGroup() {
        Set<String> selected = adapter.getSelectedIds();
        if (selected.size() < MIN_MEMBERS) {
            Toast.makeText(getContext(), "Chọn tối thiểu " + MIN_MEMBERS + " thành viên", Toast.LENGTH_SHORT).show();
            return;
        }
        creating = true;
        btnCreate.setEnabled(false);

        String name = etGroupName.getText() != null ? etGroupName.getText().toString().trim() : "";
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("memberIds", new ArrayList<>(selected));

        ApiService api = ApiClient.getApiService(requireContext().getApplicationContext());
        createCall = api.createGroup(body);
        createCall.enqueue(new Callback<ApiResponse<Conversation>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Conversation>> call,
                                   @NonNull Response<ApiResponse<Conversation>> response) {
                if (!isAdded()) return;
                creating = false;
                ApiResponse<Conversation> b = response.body();
                if (response.isSuccessful() && b != null && b.isSuccess() && b.getData() != null) {
                    Conversation group = b.getData();
                    Fragment parent = getParentFragment();
                    dismiss();
                    if (parent instanceof ChatFragment) {
                        ((ChatFragment) parent).openConversation(group);
                    }
                } else {
                    updateCreateButton();
                    Toast.makeText(getContext(), "Tạo nhóm thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Conversation>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                creating = false;
                updateCreateButton();
                Log.e(LOG_TAG, "createGroup error", t);
                Toast.makeText(getContext(), "Lỗi mạng khi tạo nhóm", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        if (friendsCall != null) friendsCall.cancel();
        if (createCall != null) createCall.cancel();
        super.onDestroyView();
    }
}
