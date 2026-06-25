package com.example.frontend.ui.library;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;

import androidx.appcompat.widget.PopupMenu;
import android.widget.ImageView;

public class LibraryFragment extends Fragment {
    private LibraryViewModel viewModel;
    private DocumentAdapter adapter;
    private EditText etSearch;
    private ProgressBar progressBar;
    private TextView tvSortLabel;

    private String currentSubject = "";

    private final ActivityResultLauncher<Intent> uploadLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    viewModel.loadDocumentsBySubject(currentSubject);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);

        MaterialCardView[] chips = {
                view.findViewById(R.id.chipAll),
                view.findViewById(R.id.chipIT),
                view.findViewById(R.id.chipEconomy),
                view.findViewById(R.id.chipScience),
                view.findViewById(R.id.chipLaw),
                view.findViewById(R.id.chipOther)
        };

        String[] subjectNames = {"", "IT", "Kinh tế", "Khoa học", "Luật", "Khác"};

        for (int i = 0; i < chips.length; i++) {
            final int index = i;
            chips[i].setOnClickListener(v -> {

                currentSubject = subjectNames[index];

                for (MaterialCardView chip : chips) {
                    chip.setCardBackgroundColor(android.graphics.Color.parseColor("#E8EFE0"));
                    TextView tv = (TextView) chip.getChildAt(0);
                    tv.setTextColor(android.graphics.Color.parseColor("#6E7E73"));
                    tv.setTypeface(null, android.graphics.Typeface.NORMAL);
                }

                chips[index].setCardBackgroundColor(android.graphics.Color.parseColor("#10B981"));
                TextView activeTv = (TextView) chips[index].getChildAt(0);
                activeTv.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
                activeTv.setTypeface(null, android.graphics.Typeface.BOLD);

                viewModel.loadDocumentsBySubject(currentSubject);
            });
        }

        RecyclerView rv = view.findViewById(R.id.recyclerViewDocuments);
        etSearch = view.findViewById(R.id.etSearch);
        progressBar = view.findViewById(R.id.progressBar);
        View btnSort = view.findViewById(R.id.btnSort);
        tvSortLabel = view.findViewById(R.id.tvSortLabel);

        adapter = new DocumentAdapter();
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(LibraryViewModel.class);

        viewModel.getDocuments().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;

            switch (result.status) {
                case LOADING:
                    if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (result.data != null) {
                        adapter.setList(result.data);
                    }
                    break;
                case ERROR:
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), result.message, Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.loadDocuments(s.toString().trim());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSort.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), v);
            popup.getMenuInflater().inflate(R.menu.sort_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                String sortType = "newest";
                String label = "Mới nhất";

                int id = item.getItemId();
                if (id == R.id.sort_oldest) {
                    sortType = "oldest";
                    label = "Cũ nhất";
                } else if (id == R.id.sort_views) {
                    sortType = "views";
                    label = "Xem nhiều";
                } else if (id == R.id.sort_downloads) {
                    sortType = "downloads";
                    label = "Tải nhiều";
                }

                if (tvSortLabel != null) tvSortLabel.setText(label);
                viewModel.loadDocumentsWithSort(sortType);
                return true;
            });
            popup.show();
        });

        // Click bình thường để xem
        adapter.setOnItemClickListener(doc -> {
            if (doc.get_id() != null) {
                viewModel.incrementView(doc.get_id());
            }

            String fileUrl = doc.getFileUrl();
            if (fileUrl == null || fileUrl.isEmpty()) {
                Toast.makeText(getContext(), "Link file bị lỗi!", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(getContext(), ViewDocumentActivity.class);
            intent.putExtra("FILE_URL", fileUrl);
            startActivity(intent);
        });

        // KHI NGƯỜI DÙNG NHẤN GIỮ -> HIỆN BOTTOM SHEET MENU
        adapter.setOnItemLongClickListener(doc -> showBottomSheetMenu(doc));

        viewModel.loadDocuments("");

        view.findViewById(R.id.fabAdd).setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), UploadDocumentActivity.class);
            uploadLauncher.launch(intent);
        });

        return view;
    }

    // ==========================================
    // LOGIC XỬ LÝ BOTTOM SHEET VÀ CÁC ACTION TƯƠNG ỨNG
    // ==========================================
    private void showBottomSheetMenu(Document doc) {
        if (getContext() == null) return;

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_bottom_sheet_actions, null);
        bottomSheetDialog.setContentView(view);

        View actionAiMindmap = view.findViewById(R.id.actionAiMindmap);
        View actionSave = view.findViewById(R.id.actionSave);
        View actionDownload = view.findViewById(R.id.actionDownload);

        // Ánh xạ icon và text của nút Save
        ImageView ivSaveIcon = view.findViewById(R.id.ivSaveIcon);
        TextView tvSaveText = view.findViewById(R.id.tvSaveText);

        // KIỂM TRA TRẠNG THÁI: Giả định model Document có thuộc tính isSaved
        if (doc.isSaved()) {
            ivSaveIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel); // Icon dấu X
            ivSaveIcon.setColorFilter(android.graphics.Color.parseColor("#70777B")); // Màu xám
            tvSaveText.setText("Bỏ lưu");
        } else {
            ivSaveIcon.setImageResource(R.drawable.ic_bookmark);
            ivSaveIcon.setColorFilter(android.graphics.Color.parseColor("#0ea5e9")); // Màu xanh
            tvSaveText.setText("Lưu tài liệu");
        }

        actionAiMindmap.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            handleAiMindmap(doc);
        });

        actionSave.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            handleSaveDocument(doc);
        });

        actionDownload.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            handleDownloadLogic(doc);
        });

        bottomSheetDialog.show();
    }

    private void handleAiMindmap(Document doc) {
        if (doc.get_id() == null) return;

        Toast.makeText(getContext(), "AI đang phân tích tài liệu, vui lòng đợi...", Toast.LENGTH_LONG).show();
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        viewModel.generateMindmap(doc.get_id()).observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;

            switch (result.status) {
                case SUCCESS:
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (result.data != null) {
                        MindmapBottomSheet bottomSheet = new MindmapBottomSheet(result.data);
                        bottomSheet.show(getChildFragmentManager(), "MindmapBottomSheet");
                    }
                    break;
                case ERROR:
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Lỗi AI: " + result.message, Toast.LENGTH_SHORT).show();
                    break;
                case LOADING:
                    break;
            }
        });
    }

    private void handleDownloadLogic(Document doc) {
        String fileUrl = doc.getFileUrl();
        if (fileUrl == null || fileUrl.isEmpty()) {
            Toast.makeText(getContext(), "Tài liệu này không có file để tải!", Toast.LENGTH_SHORT).show();
            return;
        }

        downloadFile(fileUrl, doc.getTitle());

        if (doc.get_id() != null) {
            viewModel.incrementDownload(doc.get_id());
        }
    }

    private void handleSaveDocument(Document doc) {
        if (doc.get_id() == null) return;

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        viewModel.toggleSaveDocument(doc.get_id()).observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;

            switch (result.status) {
                case SUCCESS:
                    if (progressBar != null) progressBar.setVisibility(View.GONE);

                    boolean isSaved = result.data != null && result.data;

                    // QUAN TRỌNG: Ghi đè trạng thái lưu mới vào object hiện tại
                    doc.setSaved(isSaved);

                    if (isSaved) {
                        Toast.makeText(getContext(), "Đã lưu: " + doc.getTitle(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Đã bỏ lưu tài liệu", Toast.LENGTH_SHORT).show();
                    }
                    break;

                case ERROR:
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Lỗi: " + result.message, Toast.LENGTH_SHORT).show();
                    break;

                case LOADING:
                    break;
            }
        });
    }

    // ==========================================

    private void downloadFile(String url, String title) {
        try {
            if (url == null || url.trim().isEmpty()) {
                Toast.makeText(getContext(), "URL tải xuống bị trống!", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1. Đảm bảo dùng HTTPS
            if (url.startsWith("http://")) {
                url = url.replace("http://", "https://");
            } else if (!url.startsWith("http")) {
                url = "https://" + url;
            }

            // Ép Cloudinary tải xuống trực tiếp thay vì xem trước
            if (url.contains("cloudinary.com") && url.contains("/upload/")) {
                if (!url.contains("fl_attachment")) {
                    url = url.replace("/upload/", "/upload/fl_attachment/");
                }
            }

            // 2. Tạo một URL sạch (bỏ query params)  ĐỂ dò tìm đuôi file
            String cleanUrlForExtension = url;
            if (cleanUrlForExtension.contains("?")) {
                cleanUrlForExtension = cleanUrlForExtension.substring(0, cleanUrlForExtension.indexOf("?"));
            }

            String extension = MimeTypeMap.getFileExtensionFromUrl(cleanUrlForExtension);
            if (extension == null || extension.isEmpty()) {
                int lastDotIndex = cleanUrlForExtension.lastIndexOf('.');
                if (lastDotIndex != -1 && lastDotIndex < cleanUrlForExtension.length() - 1) {
                    extension = cleanUrlForExtension.substring(lastDotIndex + 1);
                } else {
                    extension = "pdf"; // Mặc định là PDF
                }
            }

            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            if (mimeType == null) {
                mimeType = "*/*";
            }

            // 3.  Chỉ thay thế các ký tự bị cấm trong hệ thống file bằng dấu gạch dưới
            String fileName = title;
            if (fileName == null || fileName.trim().isEmpty()) {
                fileName = "Tai_Lieu_" + System.currentTimeMillis();
            } else {
                // Lọc các ký tự cấm: \ / : * ? " < > |
                fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
            }

            // Gắn đuôi file vào
            fileName = fileName + "." + extension;

            // 4. Tiến hành tải xuống
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(title);
            request.setDescription("Đang tải tài liệu...");
            request.setMimeType(mimeType);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            DownloadManager manager = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager != null) {
                manager.enqueue(request);
                Toast.makeText(getContext(), "Đang tải xuống: " + fileName, Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Lỗi tải xuống: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}