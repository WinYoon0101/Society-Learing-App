package com.example.frontend.ui.group;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.frontend.R;
import com.example.frontend.data.model.GroupPost;
import com.example.frontend.data.repository.GroupRepository;
import com.example.frontend.utils.Result;

import java.util.List;

/**
 * Tab "Bài viết" – hiển thị bài viết từ tất cả nhóm người dùng đang tham gia.
 */
public class GroupFeedFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rv;
    private TextView tvEmpty;

    private GroupPostAdapter adapter;
    private GroupRepository repository;
    private final MutableLiveData<Result<List<GroupPost>>> liveData = new MutableLiveData<>();

    private static final int LIMIT = 15;
    private int currentPage = 1;
    private boolean isLastPage = false;
    private boolean isLoadingMore = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_group_feed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        rv           = view.findViewById(R.id.rvGroupFeed);
        tvEmpty      = view.findViewById(R.id.tvEmpty);

        adapter = new GroupPostAdapter();
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        // Infinite scroll
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (!isLastPage && !isLoadingMore && lm != null
                        && lm.findLastVisibleItemPosition() >= adapter.getItemCount() - 3) {
                    currentPage++;
                    loadMore();
                }
            }
        });

        swipeRefresh.setOnRefreshListener(this::refresh);

        repository = new GroupRepository(requireContext());
        liveData.observe(getViewLifecycleOwner(), this::renderState);

        refresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload khi có thay đổi từ nơi khác (đăng bài, tham gia nhóm, chấp nhận lời mời)
        if (GroupState.feedDirty) {
            GroupState.feedDirty = false;
            refresh();
        }
    }

    private void refresh() {
        currentPage = 1;
        isLastPage = false;
        repository.getGroupFeedPosts(currentPage, LIMIT, liveData);
    }

    private void loadMore() {
        isLoadingMore = true;
        repository.getGroupFeedPosts(currentPage, LIMIT, liveData);
    }

    private void renderState(Result<List<GroupPost>> result) {
        if (result == null) return;
        switch (result.status) {
            case LOADING:
                if (currentPage == 1) swipeRefresh.setRefreshing(true);
                break;
            case SUCCESS:
                swipeRefresh.setRefreshing(false);
                isLoadingMore = false;
                List<GroupPost> data = result.data;
                if (currentPage == 1) {
                    adapter.submit(data);
                } else {
                    adapter.appendAll(data);
                }
                if (data == null || data.size() < LIMIT) isLastPage = true;
                tvEmpty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                break;
            case ERROR:
                swipeRefresh.setRefreshing(false);
                isLoadingMore = false;
                Toast.makeText(requireContext(),
                        result.message != null ? result.message : "Có lỗi xảy ra",
                        Toast.LENGTH_SHORT).show();
                tvEmpty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                break;
        }
    }
}