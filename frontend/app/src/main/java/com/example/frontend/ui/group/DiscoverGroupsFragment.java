package com.example.frontend.ui.group;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.example.frontend.data.model.Group;
import com.example.frontend.data.repository.GroupRepository;
import com.example.frontend.utils.Result;

import java.util.List;

/**
 * Tab "Khám phá" – tìm kiếm và tham gia nhóm Public.
 */
public class DiscoverGroupsFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rv;
    private TextView tvEmpty;
    private EditText edtSearch;

    private DiscoverGroupAdapter adapter;
    private GroupRepository repository;
    private final MutableLiveData<Result<List<Group>>> liveData = new MutableLiveData<>();

    private static final int LIMIT = 20;
    private int currentPage = 1;
    private boolean isLastPage = false;
    private boolean isLoadingMore = false;
    private String currentSearch = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_discover_groups, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        rv           = view.findViewById(R.id.rvDiscover);
        tvEmpty      = view.findViewById(R.id.tvEmpty);
        edtSearch     = view.findViewById(R.id.edtSearch);

        repository = new GroupRepository(requireContext());

        adapter = new DiscoverGroupAdapter(group -> {
            // Click "Tham gia" (Public) / "Yêu cầu tham gia" (Private)
            MutableLiveData<Result<com.example.frontend.data.model.RequestJoinResult>> joinLive =
                    new MutableLiveData<>();
            joinLive.observe(getViewLifecycleOwner(), r -> {
                if (r == null) return;
                if (r.status == Result.Status.SUCCESS) {
                    boolean pending = r.data != null && r.data.isPending();
                    if (pending) {
                        Toast.makeText(requireContext(),
                                "Đã gửi yêu cầu tham gia, chờ duyệt", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Đã tham gia nhóm!", Toast.LENGTH_SHORT).show();
                        GroupState.onJoinedGroup();
                    }
                    // refresh: nhóm đã join → biến mất; nhóm Private đã gửi yêu cầu → ở lại với nút "Đã gửi yêu cầu"
                    refresh();
                } else if (r.status == Result.Status.ERROR) {
                    Toast.makeText(requireContext(),
                            r.message != null ? r.message : "Có lỗi xảy ra",
                            Toast.LENGTH_SHORT).show();
                }
            });
            repository.requestJoinGroup(group.getId(), joinLive);
        });

        // Click vào item → xem trước chi tiết nhóm (chưa là thành viên)
        adapter.setOnGroupClickListener(group -> {
            android.content.Intent i = new android.content.Intent(requireContext(), GroupDetailActivity.class);
            i.putExtra(GroupDetailActivity.EXTRA_GROUP_ID, group.getId());
            i.putExtra(GroupDetailActivity.EXTRA_GROUP_NAME, group.getGroupName());
            startActivity(i);
        });

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

        // Tìm kiếm
        edtSearch.addTextChangedListener(new TextWatcher() {
            private final android.os.Handler h = new android.os.Handler();
            private Runnable r;
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (r != null) h.removeCallbacks(r);
                r = () -> {
                    currentSearch = s.toString().trim();
                    refresh();
                };
                h.postDelayed(r, 400);
            }
        });

        swipeRefresh.setOnRefreshListener(this::refresh);
        liveData.observe(getViewLifecycleOwner(), this::renderState);

        refresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload khi có thay đổi (đã join nơi khác, đánh dấu "không quan tâm")
        if (GroupState.discoverDirty) {
            GroupState.discoverDirty = false;
            refresh();
        }
    }

    private void refresh() {
        currentPage = 1;
        isLastPage = false;
        repository.discoverGroups(currentSearch, currentPage, LIMIT, liveData);
    }

    /** Lọc bỏ các nhóm đã đánh dấu "không quan tâm" (local). */
    private List<Group> filterNotInterested(List<Group> data) {
        if (data == null) return new java.util.ArrayList<>();
        java.util.Set<String> hidden = GroupState.getNotInterested(requireContext());
        if (hidden.isEmpty()) return data;
        List<Group> out = new java.util.ArrayList<>();
        for (Group g : data) {
            if (g.getId() == null || !hidden.contains(g.getId())) out.add(g);
        }
        return out;
    }

    private void loadMore() {
        isLoadingMore = true;
        repository.discoverGroups(currentSearch, currentPage, LIMIT, liveData);
    }

    private void renderState(Result<List<Group>> result) {
        if (result == null) return;
        switch (result.status) {
            case LOADING:
                if (currentPage == 1) swipeRefresh.setRefreshing(true);
                break;
            case SUCCESS:
                swipeRefresh.setRefreshing(false);
                isLoadingMore = false;
                List<Group> data = result.data;
                // isLastPage tính theo dữ liệu gốc từ server (trước khi lọc local)
                if (data == null || data.size() < LIMIT) isLastPage = true;
                List<Group> visible = filterNotInterested(data);
                if (currentPage == 1) adapter.submit(visible);
                else adapter.appendAll(visible);
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