package com.example.admin.ui.posts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.admin.data.model.Post;
import com.example.admin.data.remote.ApiService;
import com.example.admin.data.remote.RetrofitClient;

import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.example.admin.data.model.User;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class PostsFragment extends Fragment {
    private RecyclerView rvPosts;
    private ProgressBar pbLoading;
    private RadioGroup rgFilter;

    private PostAdapter adapter;
    private ApiService apiService;
    private ToxicScanner toxicScanner;

    private List<Post> allPosts = new ArrayList<>();
    private List<Post> displayPosts = new ArrayList<>();

    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private boolean showToxicOnly = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_posts, container, false);

        rvPosts = view.findViewById(R.id.rvPosts);
        pbLoading = view.findViewById(R.id.pbLoading);
        rgFilter = view.findViewById(R.id.rgFilter);

        apiService = RetrofitClient.getApi();
        toxicScanner = new ToxicScanner(requireContext());

        setupRecyclerView();

        rgFilter.setOnCheckedChangeListener((group, checkedId) -> {
            showToxicOnly = (checkedId == R.id.rbToxic);
            applyFilter();
        });

        fetchPosts();
        return view;
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        rvPosts.setLayoutManager(layoutManager);

        adapter = new PostAdapter(new PostAdapter.OnPostActionListener() {
            @Override
            public void onDeleteClick(Post post, int position) {
                // Thêm hộp thoại xác nhận
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Xóa bài viết")
                        .setMessage("Bạn có chắc chắn muốn xóa bài viết này không? Hành động này không thể hoàn tác.")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            // Gọi API Xóa
                            apiService.deletePostByAdmin(post.getId()).enqueue(new Callback<ApiResponse<Object>>() {
                                @Override
                                public void onResponse(Call<ApiResponse<Object>> c, Response<ApiResponse<Object>> r) {
                                    if (r.isSuccessful()) {
                                        displayPosts.remove(position);
                                        adapter.notifyItemRemoved(position);
                                        allPosts.remove(post);
                                        Toast.makeText(getContext(), "Đã xóa bài viết", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getContext(), "Lỗi khi xóa bài viết", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                @Override
                                public void onFailure(Call<ApiResponse<Object>> c, Throwable t) {
                                    Toast.makeText(getContext(), "Mất kết nối mạng!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }

            @Override
            public void onBanUserClick(Post post, int position) {
                if (post.getAuthor() == null) return;

                String authorName = post.getAuthor().getUsername();
                boolean isCurrentlyActive = post.getAuthor().isActive();

                // Xác định tên hành động
                String actionName = isCurrentlyActive ? "Khóa" : "Mở khóa";
                String confirmMessage = isCurrentlyActive
                        ? "Bạn muốn khóa tài khoản của người dùng: " + authorName + "?"
                        : "Bạn muốn mở khóa tài khoản cho người dùng: " + authorName + "?";

                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(actionName + " tài khoản")
                        .setMessage(confirmMessage)
                        .setPositiveButton("Xác nhận", (dialog, which) -> {

                            // Gọi API Khóa/Mở khóa
                            apiService.toggleUserStatus(post.getAuthor().getId()).enqueue(new Callback<ApiResponse<User>>() {
                                @Override
                                public void onResponse(Call<ApiResponse<User>> c, Response<ApiResponse<User>> r) {
                                    if (r.isSuccessful()) {
                                        // 1. Lấy trạng thái mới
                                        boolean newState = !isCurrentlyActive;

                                        // 2. Tìm tất cả các bài viết của người này để cập nhật nút thành Mở khóa/Khóa cùng lúc
                                        for (int i = 0; i < displayPosts.size(); i++) {
                                            if (displayPosts.get(i).getAuthor() != null &&
                                                    displayPosts.get(i).getAuthor().getId().equals(post.getAuthor().getId())) {

                                                displayPosts.get(i).getAuthor().setActive(newState);
                                                adapter.notifyItemChanged(i); // Refresh giao diện item
                                            }
                                        }
                                        Toast.makeText(getContext(), "Đã " + actionName.toLowerCase() + " người dùng: " + authorName, Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getContext(), "Lỗi khi xử lý thao tác", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                @Override
                                public void onFailure(Call<ApiResponse<User>> c, Throwable t) {
                                    Toast.makeText(getContext(), "Mất kết nối mạng!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }

            @Override
            public void onApproveClick(Post post, int position) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Bỏ qua cảnh báo")
                        .setMessage("Xác nhận bài viết này an toàn và không vi phạm?")
                        .setPositiveButton("Xác nhận", (dialog, which) -> {
                            // Cập nhật local state: Đánh dấu là an toàn
                            post.setToxicLocally(false);

                            if (showToxicOnly) {
                                // Nếu đang ở tab "Vi phạm", xóa bài khỏi danh sách hiển thị
                                displayPosts.remove(position);
                                adapter.notifyItemRemoved(position);
                            } else {
                                // Nếu đang ở tab "Tất cả", chỉ cần update UI để ẩn tag đỏ đi
                                adapter.notifyItemChanged(position);
                            }
                            Toast.makeText(getContext(), "Đã xác nhận bài viết an toàn", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        });

        rvPosts.setAdapter(adapter);

        // Kỹ thuật phân trang
        rvPosts.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 && !isLoading && !isLastPage) {
                    if ((layoutManager.getChildCount() + layoutManager.findFirstVisibleItemPosition()) >= layoutManager.getItemCount()) {
                        currentPage++;
                        fetchPosts();
                    }
                }
            }
        });
    }

    private void fetchPosts() {
        isLoading = true;
        pbLoading.setVisibility(View.VISIBLE);

        apiService.getAllPostsAdmin(currentPage, 20).enqueue(new Callback<ApiResponse<List<Post>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Post>>> call, Response<ApiResponse<List<Post>>> response) {
                isLoading = false; pbLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<Post> newPosts = response.body().getData();
                    if (newPosts == null || newPosts.isEmpty()) {
                        isLastPage = true; return;
                    }
                    allPosts.addAll(newPosts);
                    applyFilter();
                    startScanningPosts(newPosts);
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Post>>> call, Throwable t) {
                isLoading = false; pbLoading.setVisibility(View.GONE);
            }
        });
    }

    private void applyFilter() {
        displayPosts.clear();
        for (Post p : allPosts) {
            if (!showToxicOnly || (p.isScanned() && p.isToxicLocally())) displayPosts.add(p);
        }
        adapter.setPosts(displayPosts);
    }

    private void startScanningPosts(List<Post> postsToScan) {
        for (Post post : postsToScan) {
            if (post.getContent() != null && !post.getContent().isEmpty()) {
                toxicScanner.scanPost(post.getContent(), (label, confidence, labelName) -> {
                    post.setScanned(true);
                    post.setToxicLocally(label == 1 || label == 2);
                    if (post.isToxicLocally()) post.setToxicLabel(labelName);

                    // Cập nhật giao diện bài viết vừa quét xong (Không refresh toàn bộ)
                    int index = displayPosts.indexOf(post);
                    if (index != -1) adapter.notifyItemChanged(index);
                    else if (showToxicOnly && post.isToxicLocally()) applyFilter();
                });
            } else {
                post.setScanned(true);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (toxicScanner != null) toxicScanner.close();
    }
}