package com.example.frontend.ui.feed;

import android.text.Editable;
import android.text.TextWatcher;
import android.os.Bundle;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Conversation;
import com.example.frontend.data.model.Friend;
import com.example.frontend.data.model.Message;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.ui.chat.FriendPickAdapter;
import com.example.frontend.ui.chat.SharedPostMessage;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SharePostBottomSheet extends BottomSheetDialogFragment {
    public static final String TAG = "SharePostBottomSheet";

    private static final String ARG_POST_ID = "post_id";
    private static final String ARG_AUTHOR_NAME = "author_name";
    private static final String ARG_CONTENT = "content";
    private static final String ARG_IMAGE_URL = "image_url";

    private RecyclerView rvFriends;
    private View layoutEmpty;
    private TextView tvEmpty;
    private ProgressBar progress;
    private EditText etSearch;
    private Button btnSend;

    private FriendPickAdapter adapter;
    private ApiService api;
    private final List<Friend> allFriends = new ArrayList<>();
    private final Map<String, Friend> selectedFriends = new LinkedHashMap<>();
    private final List<Call<?>> pendingCalls = new ArrayList<>();
    private Call<ApiResponse<List<Friend>>> friendsCall;

    private int totalTargets;
    private int finishedTargets;
    private int successTargets;
    private boolean sending;

    public static SharePostBottomSheet newInstance(String postId, String authorName,
                                                   String content, String imageUrl) {
        SharePostBottomSheet sheet = new SharePostBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_POST_ID, postId);
        args.putString(ARG_AUTHOR_NAME, authorName);
        args.putString(ARG_CONTENT, content);
        args.putString(ARG_IMAGE_URL, imageUrl);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_share_post, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton btnClose = view.findViewById(R.id.btnSharePostClose);
        btnSend = view.findViewById(R.id.btnSharePostSend);
        etSearch = view.findViewById(R.id.etSharePostSearch);
        rvFriends = view.findViewById(R.id.rvSharePostConversations);
        layoutEmpty = view.findViewById(R.id.layoutEmptySharePost);
        tvEmpty = view.findViewById(R.id.tvEmptySharePost);
        progress = view.findViewById(R.id.progressSharePost);

        btnClose.setOnClickListener(v -> dismiss());
        btnSend.setOnClickListener(v -> sendSelectedFriends());

        adapter = new FriendPickAdapter(this::onFriendPicked);
        adapter.setSelectable(true);
        rvFriends.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFriends.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                applyFilter(s == null ? "" : s.toString());
            }
        });

        api = ApiClient.getApiService(requireContext().getApplicationContext());
        updateSendButton();
        fetchFriends();
    }

    private void fetchFriends() {
        showLoading();
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
                showEmpty("Không tải được danh sách bạn bè");
            }
        });
    }

    private void applyFilter(String query) {
        String q = query.trim().toLowerCase(Locale.getDefault());
        List<Friend> filtered = new ArrayList<>();

        for (Friend friend : allFriends) {
            String name = friend.getUsername();
            if (q.isEmpty() || (name != null && name.toLowerCase(Locale.getDefault()).contains(q))) {
                filtered.add(friend);
            }
        }

        if (filtered.isEmpty()) {
            showEmpty(allFriends.isEmpty() ? "Chưa có bạn bè" : "Không tìm thấy bạn bè phù hợp");
        } else {
            showList(filtered);
        }
    }

    private void onFriendPicked(Friend friend) {
        if (friend == null || friend.getId() == null) return;
        if (adapter.getSelectedIds().contains(friend.getId())) {
            selectedFriends.put(friend.getId(), friend);
        } else {
            selectedFriends.remove(friend.getId());
        }
        updateSendButton();
    }

    private void sendSelectedFriends() {
        if (sending) return;
        if (selectedFriends.isEmpty()) {
            Toast.makeText(getContext(), "Chọn ít nhất một người để gửi", Toast.LENGTH_SHORT).show();
            return;
        }

        Bundle args = getArguments();
        if (args == null) return;

        String postId = args.getString(ARG_POST_ID, "");
        if (postId.trim().isEmpty()) {
            Toast.makeText(getContext(), "Không tìm thấy bài viết để chia sẻ", Toast.LENGTH_SHORT).show();
            return;
        }

        String sharedText = SharedPostMessage.encode(
                postId,
                args.getString(ARG_AUTHOR_NAME, ""),
                args.getString(ARG_CONTENT, ""),
                args.getString(ARG_IMAGE_URL, "")
        );

        sending = true;
        totalTargets = selectedFriends.size();
        finishedTargets = 0;
        successTargets = 0;
        progress.setVisibility(View.VISIBLE);
        updateSendButton();

        for (Friend friend : new ArrayList<>(selectedFriends.values())) {
            openConversationThenSend(friend, sharedText);
        }
    }

    private void openConversationThenSend(Friend friend, String sharedText) {
        if (friend == null || friend.getId() == null) {
            onTargetFinished(false);
            return;
        }

        Map<String, String> body = new HashMap<>();
        body.put("targetUserId", friend.getId());

        Call<ApiResponse<Conversation>> openCall = api.getOrCreateConversation(body);
        pendingCalls.add(openCall);
        openCall.enqueue(new Callback<ApiResponse<Conversation>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Conversation>> call,
                                   @NonNull Response<ApiResponse<Conversation>> response) {
                if (!isAdded()) return;
                ApiResponse<Conversation> body = response.body();
                if (response.isSuccessful() && body != null && body.isSuccess()
                        && body.getData() != null && body.getData().getId() != null) {
                    sendPostToConversation(body.getData().getId(), sharedText);
                } else {
                    onTargetFinished(false);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Conversation>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                onTargetFinished(false);
            }
        });
    }

    private void sendPostToConversation(String conversationId, String sharedText) {
        Map<String, String> body = new HashMap<>();
        body.put("conversationId", conversationId);
        body.put("text", sharedText);

        Call<ApiResponse<Message>> sendCall = api.sendMessage(body);
        pendingCalls.add(sendCall);
        sendCall.enqueue(new Callback<ApiResponse<Message>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Message>> call,
                                   @NonNull Response<ApiResponse<Message>> response) {
                if (!isAdded()) return;
                ApiResponse<Message> responseBody = response.body();
                onTargetFinished(response.isSuccessful() && responseBody != null && responseBody.isSuccess());
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Message>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                onTargetFinished(false);
            }
        });
    }

    private void onTargetFinished(boolean success) {
        finishedTargets++;
        if (success) successTargets++;
        if (finishedTargets < totalTargets) return;

        sending = false;
        progress.setVisibility(View.GONE);
        updateSendButton();

        if (successTargets == totalTargets) {
            Toast.makeText(getContext(), "Đã gửi bài viết cho " + successTargets + " người", Toast.LENGTH_SHORT).show();
            dismiss();
        } else if (successTargets > 0) {
            Toast.makeText(getContext(), "Đã gửi " + successTargets + "/" + totalTargets + " người", Toast.LENGTH_SHORT).show();
            dismiss();
        } else {
            Toast.makeText(getContext(), "Gửi bài viết thất bại", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSendButton() {
        if (btnSend == null) return;
        int count = selectedFriends.size();
        btnSend.setEnabled(!sending && count > 0);
        btnSend.setText(count > 0 ? "Gửi (" + count + ")" : "Gửi");
    }

    private void showLoading() {
        progress.setVisibility(View.VISIBLE);
        rvFriends.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
    }

    private void showList(List<Friend> friends) {
        progress.setVisibility(sending ? View.VISIBLE : View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        rvFriends.setVisibility(View.VISIBLE);
        adapter.submitList(friends);
    }

    private void showEmpty(String message) {
        progress.setVisibility(sending ? View.VISIBLE : View.GONE);
        rvFriends.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
        if (tvEmpty != null) tvEmpty.setText(message);
    }

    @Override
    public void onDestroyView() {
        if (friendsCall != null) friendsCall.cancel();
        for (Call<?> call : pendingCalls) {
            if (call != null && !call.isCanceled()) call.cancel();
        }
        super.onDestroyView();
    }
}
