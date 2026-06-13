package com.example.frontend.ui.group;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.data.model.Group;
import com.example.frontend.data.repository.GroupRepository;
import com.example.frontend.utils.Result;

import java.util.List;

public class DiscoverGroupsFragment extends Fragment {

    private RecyclerView rv;
    private TextView tvEmpty;
    private DiscoverGroupAdapter adapter;
    private GroupRepository repository;
    private final MutableLiveData<Result<List<Group>>> liveData = new MutableLiveData<>();
    private final MutableLiveData<Result<Void>> joinLiveData = new MutableLiveData<>();

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private String currentQuery = "";

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

        rv = view.findViewById(R.id.rvDiscover);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        EditText etSearch = view.findViewById(R.id.etSearch);

        adapter = new DiscoverGroupAdapter();
        adapter.setOnJoinListener((group, position) -> {
            repository.joinGroup(group.getId(), joinLiveData);
            joinLiveData.observe(getViewLifecycleOwner(), result -> {
                if (result == null) return;
                if (result.status == Result.Status.SUCCESS) {
                    adapter.removeAt(position);
                    Toast.makeText(requireContext(), "Đã tham gia nhóm " + group.getGroupName(), Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                } else if (result.status == Result.Status.ERROR) {
                    Toast.makeText(requireContext(),
                            result.message != null ? result.message : "Tham gia thất bại",
                            Toast.LENGTH_SHORT).show();
                }
            });
        });

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        rv.setAdapter(adapter);

        repository = new GroupRepository(requireContext());
        liveData.observe(getViewLifecycleOwner(), this::render);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                debounceHandler.removeCallbacksAndMessages(null);
                debounceHandler.postDelayed(() -> {
                    currentQuery = s.toString().trim();
                    load();
                }, 400);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        load();
    }

    private void load() {
        repository.discoverGroups(currentQuery, 1, 20, liveData);
    }

    private void render(Result<List<Group>> result) {
        if (result == null) return;
        switch (result.status) {
            case SUCCESS:
                adapter.submit(result.data);
                updateEmptyState();
                break;
            case ERROR:
                Toast.makeText(requireContext(),
                        result.message != null ? result.message : "Có lỗi xảy ra",
                        Toast.LENGTH_SHORT).show();
                updateEmptyState();
                break;
            default:
                break;
        }
    }

    private void updateEmptyState() {
        tvEmpty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        debounceHandler.removeCallbacksAndMessages(null);
    }
}
