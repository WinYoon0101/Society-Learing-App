package com.example.frontend.ui.docs;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.data.model.Document;
import com.example.frontend.ui.library.ViewDocumentActivity;

public class SavedDocsFragment extends Fragment {

    private RecyclerView recyclerView;
    private SavedDocsAdapter adapter;
    private DocsViewModel viewModel;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_docs_list, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.loading);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        tvEmpty.setText("Bạn chưa lưu tài liệu nào");

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new SavedDocsAdapter(new SavedDocsAdapter.OnSavedDocActionListener() {
            @Override
            public void onUnsaveClick(Document doc) {

                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("Bỏ lưu tài liệu")
                        .setMessage("Bạn muốn bỏ lưu tài liệu này khỏi danh sách?")
                        .setPositiveButton("Bỏ lưu", (dialog, which) -> viewModel.toggleSaveDocument(doc.getId()))
                        .setNegativeButton("Hủy", null);

                AlertDialog dialog = builder.create();


                dialog.setOnShowListener(dialogInterface -> {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#10B981")); // Màu xanh chủ đạo
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#6E7E73")); // Màu xám
                });

                dialog.show();
            }

            @Override
            public void onItemClick(Document doc) {
                Intent intent = new Intent(getContext(), ViewDocumentActivity.class);
                intent.putExtra("FILE_URL", doc.getFileUrl());
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(requireActivity()).get(DocsViewModel.class);

        viewModel.getSavedDocsResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            switch (result.status) {
                case LOADING:
                    progressBar.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    progressBar.setVisibility(View.GONE);
                    if (result.data != null) {
                        adapter.submitList(result.data);
                        tvEmpty.setVisibility(result.data.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                    break;
                case ERROR:
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Lỗi tải: " + result.message, Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        viewModel.getToggleSaveResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result.status == com.example.frontend.utils.Result.Status.SUCCESS) {
                Toast.makeText(getContext(), "Đã bỏ lưu tài liệu!", Toast.LENGTH_SHORT).show();
                viewModel.fetchSavedDocuments(); // Reload lại danh sách sau khi bỏ lưu
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null && adapter.getItemCount() == 0) {
            viewModel.fetchSavedDocuments();
        }
    }
}