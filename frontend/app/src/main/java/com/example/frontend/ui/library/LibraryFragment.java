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

import java.util.ArrayList;

public class LibraryFragment extends Fragment {
    private LibraryViewModel viewModel;
    private DocumentAdapter adapter;
    private EditText etSearch;
    private ProgressBar progressBar; // Thêm ProgressBar

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

        // 1. Ánh xạ View từ XML
        RecyclerView rv = view.findViewById(R.id.recyclerViewDocuments);
        etSearch = view.findViewById(R.id.etSearch);
        progressBar = view.findViewById(R.id.progressBar); // Đã thêm vào XML ở bước trước

        // 2. Thiết lập RecyclerView & Adapter
        adapter = new DocumentAdapter();
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        // 3. Khởi tạo ViewModel (Sử dụng ViewModelProvider chuẩn)
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

        // 5. Xử lý sự kiện Tìm kiếm
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Mỗi khi người dùng gõ hoặc xóa 1 ký tự, hàm này sẽ chạy
                String query = s.toString().trim();
                viewModel.loadDocuments(query); // Gọi API tìm kiếm ngay lập tức
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 6. Xử lý khi click vào từng tài liệu
        adapter.setOnItemClickListener(doc -> {
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