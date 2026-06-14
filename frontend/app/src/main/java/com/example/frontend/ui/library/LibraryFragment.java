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
import com.google.android.material.card.MaterialCardView;

import androidx.appcompat.widget.PopupMenu;

import java.util.ArrayList;

public class LibraryFragment extends Fragment {
    private LibraryViewModel viewModel;
    private DocumentAdapter adapter;
    private EditText etSearch;
    private ProgressBar progressBar;
    private TextView tvSortLabel;

    private String currentSubject = ""; // Lưu tab đang chọn

    private final ActivityResultLauncher<Intent> uploadLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // Nếu upload thành công, tải lại danh sách theo tab hiện tại
                    if (currentSubject.isEmpty()) {
                        viewModel.loadDocuments("");
                    } else {
                        viewModel.loadDocumentsBySubject(currentSubject);
                    }
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
                view.findViewById(R.id.chipLaw)
        };

        String[] subjectNames = {"", "IT", "Kinh tế", "Khoa học", "Luật"};

        for (int i = 0; i < chips.length; i++) {
            final int index = i;
            chips[i].setOnClickListener(v -> {

                currentSubject = subjectNames[index]; // Lưu lại tab hiện tại

                // 1. Đổi màu tất cả về xám nhạt (Chưa chọn)
                for (MaterialCardView chip : chips) {
                    chip.setCardBackgroundColor(android.graphics.Color.parseColor("#E8EFE0"));
                    // Tìm TextView bên trong để đổi màu chữ luôn
                    TextView tv = (TextView) chip.getChildAt(0);
                    tv.setTextColor(android.graphics.Color.parseColor("#6E7E73"));
                    tv.setTypeface(null, android.graphics.Typeface.NORMAL);
                }

                // 2. Nhuộm màu xanh cho cái được chọn
                chips[index].setCardBackgroundColor(android.graphics.Color.parseColor("#10B981"));
                TextView activeTv = (TextView) chips[index].getChildAt(0);
                activeTv.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
                activeTv.setTypeface(null, android.graphics.Typeface.BOLD);

                // 3. Gọi API lấy dữ liệu
                if (currentSubject.isEmpty()) {
                    viewModel.loadDocuments("");
                } else {
                    viewModel.loadDocumentsBySubject(currentSubject);
                }
            });
        }

        // 1. Ánh xạ View từ XML
        RecyclerView rv = view.findViewById(R.id.recyclerViewDocuments);
        etSearch = view.findViewById(R.id.etSearch);
        progressBar = view.findViewById(R.id.progressBar);
        View btnSort = view.findViewById(R.id.btnSort);
        tvSortLabel = view.findViewById(R.id.tvSortLabel);

        // 2. Thiết lập RecyclerView & Adapter
        adapter = new DocumentAdapter();
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        // 3. Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(LibraryViewModel.class);

        // 4. Quan sát dữ liệu từ LiveData (Xử lý các trạng thái của Result)
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

        // 5. Xử lý Tìm kiếm
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

        // 6. XỬ LÝ NÚT SẮP XẾP
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

        // 7. Xử lý khi click vào từng tài liệu để xem
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

        // 8. XỬ LÝ KHI CLICK NÚT TẢI XUỐNG
        adapter.setOnDownloadClickListener(doc -> {
            String fileUrl = doc.getFileUrl();
            if (fileUrl == null || fileUrl.isEmpty()) {
                Toast.makeText(getContext(), "Tài liệu này không có file để tải!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Gọi hàm tải file
            downloadFile(fileUrl, doc.getTitle());

            // Sau khi tải xong, tăng lượt tải xuống lên 1
            if (doc.get_id() != null) {
                viewModel.incrementDownload(doc.get_id());
            }
        });

        // 9. Tải dữ liệu mặc định khi vừa mở màn hình
        viewModel.loadDocuments("");

        // 10. Xử lý sự kiện khi click vào nút Thêm tài liệu
        view.findViewById(R.id.fabAdd).setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), UploadDocumentActivity.class);
            uploadLauncher.launch(intent);
        });

        return view;
    }

    private void downloadFile(String url, String title) {
        try {
            if (url == null || url.trim().isEmpty()) {
                Toast.makeText(getContext(), "URL tải xuống bị trống!", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1. Chuẩn hóa URL (Sửa lỗi http/https)
            if (url.startsWith("http://")) {
                url = url.replace("http://", "https://");
            } else if (!url.startsWith("http")) {
                url = "https://" + url;
            }

            // ==========================================================
            // 2. ĐẶC TRỊ CLOUDINARY & LỌC QUERY PARAMETER CAUSING CRASH
            // ==========================================================
            // Xóa sạch cái đuôi ?f=.pdf hay bất kỳ thứ gì sau dấu ?
            if (url.contains("?")) {
                url = url.substring(0, url.indexOf("?"));
            }

            // Ép Cloudinary gửi trả file dưới dạng "Tải xuống" thay vì "Xem trước"
            if (url.contains("cloudinary.com") && url.contains("/upload/")) {
                if (!url.contains("fl_attachment")) {
                    url = url.replace("/upload/", "/upload/fl_attachment/");
                }
            }
            // ==========================================================

            // 3. Lấy định dạng đuôi file
            String extension = MimeTypeMap.getFileExtensionFromUrl(url);
            if (extension == null || extension.isEmpty()) {
                int lastDotIndex = url.lastIndexOf('.');
                if (lastDotIndex != -1 && lastDotIndex < url.length() - 1) {
                    extension = url.substring(lastDotIndex + 1);
                } else {
                    extension = "pdf";
                }
            }

            // 4. Lấy MimeType
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            if (mimeType == null) {
                mimeType = "*/*";
            }

            // 5. Lọc tên file an toàn
            String safeTitle = title.replaceAll("[^a-zA-Z0-9\\s-]", "");
            if (safeTitle.trim().isEmpty()) safeTitle = "Tai_Lieu";
            String fileName = safeTitle.replaceAll("\\s+", "_") + "." + extension;

            // ... (Phần DownloadManager.Request bên dưới của bạn giữ nguyên không cần đổi)
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