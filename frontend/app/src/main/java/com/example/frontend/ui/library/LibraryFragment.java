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

    private final ActivityResultLauncher<Intent> uploadLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // Nếu upload thành công, tải lại danh sách ngay lập tức
                    viewModel.loadDocuments("");

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
                // 1. Đổi màu tất cả về xám nhạt (Chưa chọn)
                for (MaterialCardView chip : chips) {
                    chip.setCardBackgroundColor(android.graphics.Color.parseColor("#E8EFE0"));
                    // Tìm TextView bên trong để đổi màu chữ luôn
                    TextView tv = (TextView) chip.getChildAt(0);
                    tv.setTextColor(android.graphics.Color.parseColor("#6E7E73"));
                    tv.setTypeface(null, android.graphics.Typeface.NORMAL);
                }

                // 2. Nhuộm màu xanh cho cái được chọn
                chips[index].setCardBackgroundColor(android.graphics.Color.parseColor("#0A7D21"));
                TextView activeTv = (TextView) chips[index].getChildAt(0);
                activeTv.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
                activeTv.setTypeface(null, android.graphics.Typeface.BOLD);

                // 3. Gọi API lấy dữ liệu
                viewModel.loadDocumentsBySubject(subjectNames[index]);
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
                    // Hiện vòng xoay khi đang lấy dữ liệu
                    if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                    break;

                case SUCCESS:
                    // Ẩn vòng xoay và đổ dữ liệu vào danh sách
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (result.data != null) {
                        adapter.setList(result.data);
                    }
                    break;

                case ERROR:
                    // Ẩn vòng xoay và báo lỗi cho người dùng
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), result.message, Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        // 5. Xử lý Tìm kiếm (Debounce sẽ tốt hơn, nhưng đây là cách cơ bản bạn đang dùng)
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

        // 6. XỬ LÝ NÚT SẮP XẾP (PHẦN MỚI)
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

                // Cập nhật text trên giao diện để người dùng biết đang lọc gì
                if (tvSortLabel != null) tvSortLabel.setText(label);

                // Gọi ViewModel để tải lại (ViewModel sẽ tự giữ Search hiện tại)
                viewModel.loadDocumentsWithSort(sortType);
                return true;
            });
            popup.show();
        });

        // 6. Xử lý khi click vào từng tài liệu
        adapter.setOnItemClickListener(doc -> {
            if (doc.get_id() != null) {
                viewModel.incrementView(doc.get_id());
            }

            String fileUrl = doc.getFileUrl();
            if (fileUrl == null || fileUrl.isEmpty()) {
                Toast.makeText(getContext(), "Link file bị lỗi!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Gửi URL sang màn hình xem trong app
            Intent intent = new Intent(getContext(), ViewDocumentActivity.class);
            intent.putExtra("FILE_URL", fileUrl);
            startActivity(intent);
        });
        // 7. Tải dữ liệu mặc định khi vừa mở màn hình
        viewModel.loadDocuments("");

        // 8. Xử lý sự kiện khi click vào nút Thêm tài liệu
        view.findViewById(R.id.fabAdd).setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), UploadDocumentActivity.class);
            uploadLauncher.launch(intent);
        });


        return view;
    }

    private void downloadFile(String url, String title) {
        // 1. Lấy đuôi file từ URL
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension == null || extension.isEmpty()) extension = "pdf";

        // 2. Đặt tên file = Tên tài liệu + đuôi
        String fileName = title.replaceAll("\\s+", "_") + "." + extension;

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("Tải về: " + title);
        request.setMimeType(MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        DownloadManager manager = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager != null) {
            manager.enqueue(request);
            Toast.makeText(getContext(), "Đang tải xuống " + fileName, Toast.LENGTH_SHORT).show();
        }
    }
}