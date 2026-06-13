package com.example.frontend.ui.group;

import android.content.Intent;
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
import com.example.frontend.data.model.Group;
import com.example.frontend.data.repository.GroupRepository;
import com.example.frontend.utils.Result;

import java.util.List;

public class MyGroupsFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rv;
    private TextView tvEmpty;
    private View btnCreate;

    private GroupAdapter adapter;
    private GroupRepository repository;
    private final MutableLiveData<Result<List<Group>>> liveData = new MutableLiveData<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_groups, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        rv           = view.findViewById(R.id.rvMyGroups);
        tvEmpty      = view.findViewById(R.id.tvEmpty);
        btnCreate    = view.findViewById(R.id.btnCreateGroup);

        // Click vào nhóm → mở GroupDetailActivity
        adapter = new GroupAdapter(group -> {
            Intent i = new Intent(requireContext(), GroupDetailActivity.class);
            i.putExtra(GroupDetailActivity.EXTRA_GROUP_ID, group.getId());
            i.putExtra(GroupDetailActivity.EXTRA_GROUP_NAME, group.getGroupName());
            startActivity(i);
        });

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        btnCreate.setOnClickListener(v -> {
            CreateGroupBottomSheet sheet = CreateGroupBottomSheet.newInstance();
            sheet.setOnGroupCreatedListener(this::loadGroups);
            sheet.show(getParentFragmentManager(), CreateGroupBottomSheet.TAG);
        });

        swipeRefresh.setOnRefreshListener(this::loadGroups);

        repository = new GroupRepository(requireContext());
        liveData.observe(getViewLifecycleOwner(), this::renderState);

        loadGroups();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload khi quay lại từ GroupDetail (sau khi join / edit)
        loadGroups();
    }

    private void loadGroups() {
        repository.getMyGroups(liveData);
    }

    private void renderState(Result<List<Group>> result) {
        if (result == null) return;
        switch (result.status) {
            case LOADING:
                if (adapter.getItemCount() == 0) swipeRefresh.setRefreshing(true);
                break;
            case SUCCESS:
                swipeRefresh.setRefreshing(false);
                List<Group> data = result.data;
                adapter.submit(data);
                tvEmpty.setVisibility(
                        (data == null || data.isEmpty()) ? View.VISIBLE : View.GONE);
                break;
            case ERROR:
                swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(),
                        result.message != null ? result.message : "Có lỗi xảy ra",
                        Toast.LENGTH_SHORT).show();
                if (adapter.getItemCount() == 0) tvEmpty.setVisibility(View.VISIBLE);
                break;
        }
    }
}