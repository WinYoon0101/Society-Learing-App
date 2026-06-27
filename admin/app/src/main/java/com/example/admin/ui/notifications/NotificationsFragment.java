package com.example.admin.ui.notifications;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.admin.R;
import com.example.admin.data.model.ApiResponse;
import com.example.admin.data.model.NotificationRequest;
import com.example.admin.data.model.User;
import com.example.admin.data.remote.ApiService;
import com.example.admin.data.remote.RetrofitClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsFragment extends Fragment {

    private TextInputEditText edtContent;
    private RadioGroup rgType, rgTarget;
    private MaterialButton btnSend;
    private ProgressBar pbLoading;
    private LinearLayout layoutUserSelection;
    private EditText edtSearchUser;
    private RecyclerView rvUsers;

    private ApiService apiService;
    private UserCheckboxAdapter adapter;
    private boolean isSpecificUsers = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        // Ánh xạ View
        edtContent = view.findViewById(R.id.edtContent);
        rgType = view.findViewById(R.id.rgType);
        rgTarget = view.findViewById(R.id.rgTarget);
        btnSend = view.findViewById(R.id.btnSend);
        pbLoading = view.findViewById(R.id.pbLoading);

        layoutUserSelection = view.findViewById(R.id.layoutUserSelection);
        edtSearchUser = view.findViewById(R.id.edtSearchUser);
        rvUsers = view.findViewById(R.id.rvUsers);

        apiService = RetrofitClient.getApi();

        setupUserRecyclerView();
        setupListeners();

        return view;
    }

    private void setupUserRecyclerView() {
        adapter = new UserCheckboxAdapter();
        rvUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        rvUsers.setAdapter(adapter);

        // Bắt sự kiện gõ phím để tìm kiếm
        edtSearchUser.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupListeners() {
        // Lắng nghe đổi Target (Gửi tất cả hay Gửi nhóm)
        rgTarget.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbSendSpecific) {
                isSpecificUsers = true;
                layoutUserSelection.setVisibility(View.VISIBLE);
                btnSend.setText("Gửi cho người dùng");
                loadUsersList(); // Gọi API lấy list user khi mở khu vực này ra
            } else {
                isSpecificUsers = false;
                layoutUserSelection.setVisibility(View.GONE);
                btnSend.setText("Gửi thông báo");
            }
        });

        // Bắt sự kiện nút Gửi
        btnSend.setOnClickListener(v -> sendNotification());
    }

    private void loadUsersList() {
        // Tránh gọi API nhiều lần nếu đã có data
        if (adapter.getItemCount() > 0) return;

        apiService.getAllUsersAdmin().enqueue(new Callback<ApiResponse<List<User>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<User>>> call, Response<ApiResponse<List<User>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setUsers(response.body().getData());
                }
            }
            @Override public void onFailure(Call<ApiResponse<List<User>>> call, Throwable t) {}
        });
    }

    private void sendNotification() {
        String content = edtContent.getText() != null ? edtContent.getText().toString().trim() : "";
        if (TextUtils.isEmpty(content)) {
            edtContent.setError("Vui lòng nhập nội dung thông báo");
            return;
        }

        List<String> targetUserIds = null;
        if (isSpecificUsers) {
            targetUserIds = adapter.getSelectedUserIds();
            if (targetUserIds.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng tick chọn ít nhất 1 người dùng!", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Xác định loại thông báo
        String type = "system_notice";
        int checkedId = rgType.getCheckedRadioButtonId();
        if (checkedId == R.id.rbWarning) type = "system_warning";
        else if (checkedId == R.id.rbEvent) type = "system_event";

        setLoading(true);

        // Truyền thêm targetUserIds vào request
        NotificationRequest request = new NotificationRequest(content, type, targetUserIds);

        apiService.sendSystemNotification(request).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Object>> call, @NonNull Response<ApiResponse<Object>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(getContext(), "Gửi thông báo thành công!", Toast.LENGTH_SHORT).show();
                    edtContent.setText("");
                    // Có thể reset lại CheckBox nếu cần
                } else {
                    Toast.makeText(getContext(), "Lỗi từ server", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Object>> call, @NonNull Throwable t) {
                setLoading(false);
                Toast.makeText(getContext(), "Lỗi kết nối mạng!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            btnSend.setEnabled(false);
            pbLoading.setVisibility(View.VISIBLE);
        } else {
            btnSend.setEnabled(true);
            pbLoading.setVisibility(View.GONE);
        }
    }
}