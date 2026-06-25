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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Thêm thành viên vào conversation (group, hoặc 1-1 → thành group). Multi-select. */
public class AddMembersBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "AddMembersBottomSheet";
    private static final String LOG_TAG = "AddMembersSheet";
    private static final String ARG_CONV_ID = "conversationId";
    private static final String ARG_EXISTING = "existingIds";

    private EditText etSearch;
    private RecyclerView rvFriends;
    private ProgressBar progress;
    private TextView tvEmpty;
    private Button btnAdd;

    private FriendPickAdapter adapter;
    private final List<Friend> allFriends = new ArrayList<>();
    private final Set<String> existingIds = new HashSet<>();
    private String conversationId;
    private Call<ApiResponse<List<Friend>>> friendsCall;
    private Call<ApiResponse<Conversation>> addCall;
    private boolean adding = false;

    public static AddMembersBottomSheet newInstance(String conversationId, ArrayList<String> existingIds) {
        AddMembersBottomSheet f = new AddMembersBottomSheet();
        Bundle b = new Bundle();
        b.putString(ARG_CONV_ID, conversationId);
        b.putStringArrayList(ARG_EXISTING, existingIds);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_add_members, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            conversationId = getArguments().getString(ARG_CONV_ID);
            ArrayList<String> existing = getArguments().getStringArrayList(ARG_EXISTING);
            if (existing != null) existingIds.addAll(existing);
        }

        ImageButton btnClose = view.findViewById(R.id.btnAddMembersClose);
        etSearch = view.findViewById(R.id.etSearchAddMember);
        rvFriends = view.findViewById(R.id.rvAddMemberFriends);
        progress = view.findViewById(R.id.progressAddMember);
        tvEmpty = view.findViewById(R.id.tvEmptyAddMember);
        btnAdd = view.findViewById(R.id.btnAddMembers);

        btnClose.setOnClickListener(v -> dismiss());

        adapter = new FriendPickAdapter(friend -> updateAddButton());
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

        btnAdd.setOnClickListener(v -> addMembers());

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
                    // Loại các bạn đã là thành viên
                    for (Friend f : body.getData()) {
                        if (f.getId() == null || !existingIds.contains(f.getId())) {
                            allFriends.add(f);
                        }
                    }
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
        List<Friend> filtered = new ArrayList<>();
        for (Friend f : allFriends) {
            String name = f.getUsername();
            if (q.isEmpty() || (name != null && name.toLowerCase(Locale.getDefault()).contains(q))) {
                filtered.add(f);
            }
        }
        if (filtered.isEmpty()) {
            showEmpty(allFriends.isEmpty() ? "Không có bạn bè để thêm" : "Không tìm thấy bạn bè phù hợp");
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

    private void updateAddButton() {
        int count = adapter.getSelectedIds().size();
        btnAdd.setEnabled(!adding && count > 0);
        btnAdd.setText(count > 0 ? "Thêm (" + count + ")" : "Thêm");
    }

    private void addMembers() {
        Set<String> selected = adapter.getSelectedIds();
        if (selected.isEmpty()) return;
        adding = true;
        btnAdd.setEnabled(false);

        Map<String, Object> body = new HashMap<>();
        body.put("userIds", new ArrayList<>(selected));

        ApiService api = ApiClient.getApiService(requireContext().getApplicationContext());
        addCall = api.addGroupMembers(conversationId, body);
        addCall.enqueue(new Callback<ApiResponse<Conversation>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Conversation>> call,
                                   @NonNull Response<ApiResponse<Conversation>> response) {
                if (!isAdded()) return;
                adding = false;
                ApiResponse<Conversation> b = response.body();
                if (response.isSuccessful() && b != null && b.isSuccess() && b.getData() != null) {
                    Fragment parent = getParentFragment();
                    if (parent instanceof ChatDetailFragment) {
                        ((ChatDetailFragment) parent).onConversationUpdated(b.getData());
                    }
                    dismiss();
                } else {
                    updateAddButton();
                    Toast.makeText(getContext(), "Thêm thành viên thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Conversation>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                adding = false;
                updateAddButton();
                Log.e(LOG_TAG, "addMembers error", t);
                Toast.makeText(getContext(), "Lỗi mạng khi thêm thành viên", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        if (friendsCall != null) friendsCall.cancel();
        if (addCall != null) addCall.cancel();
        super.onDestroyView();
    }
}
