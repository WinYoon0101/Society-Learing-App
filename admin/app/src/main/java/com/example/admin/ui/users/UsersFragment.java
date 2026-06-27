package com.example.admin.ui.users;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // NHỚ IMPORT
import com.example.admin.R;
import com.example.admin.data.model.ApiResponse;
import com.example.admin.data.model.User;
import com.example.admin.data.remote.RetrofitClient;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UsersFragment extends Fragment {

    private RecyclerView rcvUsers;
    private EditText edtSearchUser;
    private UserAdminAdapter userAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_users, container, false);
        initViews(view);
        setupRecyclerView();
        setupSearch(); // Gắn lại tính năng Search
        fetchData();
        return view;
    }

    private void initViews(View view) {
        rcvUsers = view.findViewById(R.id.rcvUsers);
        edtSearchUser = view.findViewById(R.id.edtSearchUser);
    }

    private void setupRecyclerView() {
        userAdapter = new UserAdminAdapter();
        rcvUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        rcvUsers.setAdapter(userAdapter);

        userAdapter.setListener((user, position) -> {
            showUserBottomSheet(user, position);
        });
    }

    // KHÔI PHỤC LẠI TÍNH NĂNG TÌM KIẾM THEO TÊN & EMAIL
    private void setupSearch() {
        edtSearchUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (userAdapter != null) {
                    userAdapter.filter(s.toString().trim());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchData() {
        RetrofitClient.getApi().getAllUsersAdmin().enqueue(new Callback<ApiResponse<List<User>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<User>>> call, Response<ApiResponse<List<User>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userAdapter.setData(response.body().data);
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<User>>> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showUserBottomSheet(User user, int position) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);
        View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.layout_bottom_sheet_user, null);
        bottomSheetDialog.setContentView(sheetView);

        // Ánh xạ
        ImageView imgCover = sheetView.findViewById(R.id.bs_imgCover);
        ImageView imgAvatar = sheetView.findViewById(R.id.bs_imgAvatar);
        TextView tvName = sheetView.findViewById(R.id.bs_tvUsername);
        TextView tvLocation = sheetView.findViewById(R.id.bs_tvLocation);
        TextView tvStatus = sheetView.findViewById(R.id.bs_tvStatus);
        TextView tvBio = sheetView.findViewById(R.id.bs_tvBio);
        Button btnToggleStatus = sheetView.findViewById(R.id.bs_btnToggleStatus);

        // Load Ảnh bằng Glide
        Glide.with(this).load(user.cover).placeholder(R.color.gray).into(imgCover);
        Glide.with(this).load(user.avatar).circleCrop().into(imgAvatar);

        // Gán text
        tvName.setText(user.username);
        tvLocation.setText(user.location != null ? user.location : "Chưa cập nhật");
        tvBio.setText("Tiểu sử: " + (user.bio != null ? user.bio : "Không có"));

        // Fill Grid Info
        setInfoCard(sheetView.findViewById(R.id.cardGender), "Giới tính", user.gender);
        setInfoCard(sheetView.findViewById(R.id.cardDob), "Ngày sinh", user.dateOfBirth);
        setInfoCard(sheetView.findViewById(R.id.cardHometown), "Quê quán", user.hometown);
        setInfoCard(sheetView.findViewById(R.id.cardEmail), "Email", user.email);

        if (user.isActive) {
            tvStatus.setText("Hoạt động");
            tvStatus.setTextColor(Color.parseColor("#065F46"));
            tvStatus.setBackgroundColor(Color.parseColor("#D1FAE5"));

            btnToggleStatus.setText("Khóa tài khoản");
            btnToggleStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FEE2E2")));
            btnToggleStatus.setTextColor(Color.parseColor("#B91C1C"));
        } else {
            tvStatus.setText("Bị khóa");
            tvStatus.setTextColor(Color.parseColor("#991B1B"));
            tvStatus.setBackgroundColor(Color.parseColor("#FEE2E2"));

            btnToggleStatus.setText("Mở khóa tài khoản");
            btnToggleStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#D1FAE5")));
            btnToggleStatus.setTextColor(Color.parseColor("#065F46"));
        }

        btnToggleStatus.setOnClickListener(v -> {
            toggleUserStatus(user._id, position, bottomSheetDialog);
        });

        bottomSheetDialog.show();
    }

    private void setInfoCard(View container, String label, String value) {
        // Tìm các TextView DỰA TRÊN view cha (container)
        TextView tvLabel = container.findViewById(R.id.infoLabel);
        TextView tvValue = container.findViewById(R.id.infoValue);

        // Kiểm tra null để tránh crash nếu find không thấy
        if (tvLabel != null) tvLabel.setText(label);
        if (tvValue != null) tvValue.setText(value != null && !value.isEmpty() ? value : "N/A");
    }

    private void toggleUserStatus(String userId, int position, BottomSheetDialog dialog) {
        RetrofitClient.getApi().toggleUserStatus(userId).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    Toast.makeText(getContext(), response.body().message, Toast.LENGTH_SHORT).show();
                    userAdapter.updateUserAt(position, response.body().data);
                    dialog.dismiss();
                } else {
                    Toast.makeText(getContext(), "Thao tác thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi kết nối server", Toast.LENGTH_SHORT).show();
            }
        });
    }
}