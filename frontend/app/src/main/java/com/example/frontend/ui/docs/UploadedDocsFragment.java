package com.example.frontend.ui.docs;

import android.app.Activity;
import android.content.Intent;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AlertDialog;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.data.model.Document;
import com.example.frontend.ui.library.ViewDocumentActivity;

public class UploadedDocsFragment extends Fragment {

    private RecyclerView recyclerView;
    private UploadedDocsAdapter adapter;
    private DocsViewModel viewModel;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private final ActivityResultLauncher<Intent> editLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    if (viewModel != null) viewModel.fetchMyDocuments(1, 20);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_docs_list, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.loading);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        tvEmpty.setText("Bạn chưa đăng tài liệu nào");

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new UploadedDocsAdapter(new UploadedDocsAdapter.OnDocActionListener() {
            @Override
            public void onDeleteClick(Document doc) {

                // 1. Tạo Builder
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("Xóa tài liệu")
                        .setMessage("Bạn có chắc chắn muốn xóa '" + doc.getTitle() + "'? Hành động này không thể hoàn tác.")
                        .setPositiveButton("Xóa", (dialog, which) -> viewModel.deleteDocument(doc.getId()))
                        .setNegativeButton("Hủy", null);

                // 2. Tạo Dialog từ Builder
                AlertDialog dialog = builder.create();

                // 3. Sử dụng Listener khi Dialog hiện lên để đổi màu nút
                dialog.setOnShowListener(dialogInterface -> {

                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#10B981"));

                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#6E7E73"));
                });

                // 4. Hiển thị Dialog
                dialog.show();
            }

            @Override
            public void onEditClick(Document doc) {
                Intent intent = new Intent(getContext(), EditDocActivity.class);
                intent.putExtra("DOC_DATA", doc);
                editLauncher.launch(intent);
            }

            @Override
            public void onItemClick(Document doc) {
                Intent intent = new Intent(getContext(), ViewDocumentActivity.class);
                intent.putExtra("FILE_URL", doc.getFileUrl());
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);

        // Dùng requireActivity() để share chung ViewModel với Activity/Fragment khác
        viewModel = new ViewModelProvider(requireActivity()).get(DocsViewModel.class);

        viewModel.getMyDocsResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            switch (result.status) {
                case LOADING:
                    progressBar.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    progressBar.setVisibility(View.GONE);
                    if (result.data != null) {
                        adapter.submitList(result.data.getDocuments());
                        tvEmpty.setVisibility(result.data.getDocuments().isEmpty() ? View.VISIBLE : View.GONE);
                    }
                    break;
                case ERROR:
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Lỗi tải: " + result.message, Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        viewModel.getDeleteResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            switch (result.status) {
                case SUCCESS:
                    Toast.makeText(getContext(), "Đã xóa tài liệu!", Toast.LENGTH_SHORT).show();
                    viewModel.fetchMyDocuments(1, 20); // Reload
                    break;
                case ERROR:
                    Toast.makeText(getContext(), "Lỗi xóa: " + result.message, Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        viewModel.fetchMyDocuments(1, 20);
        return view;
    }
}