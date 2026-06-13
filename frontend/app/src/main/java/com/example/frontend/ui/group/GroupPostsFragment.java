package com.example.frontend.ui.group;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.frontend.R;
import com.example.frontend.data.model.Post;
import com.example.frontend.data.remote.ApiClient;
import com.example.frontend.data.remote.ApiService;
import com.example.frontend.data.repository.GroupRepository;
import com.example.frontend.data.model.ReactionRequest;
import com.example.frontend.utils.Result;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.example.frontend.data.model.ApiResponse;

public class GroupPostsFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rv;
    private GroupPostAdapter adapter;
    private GroupRepository repository;
    private final MutableLiveData<Result<List<Post>>> liveData = new MutableLiveData<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_group_posts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        rv = view.findViewById(R.id.rvGroupPosts);

        adapter = new GroupPostAdapter(requireContext());
        adapter.setOnReactionListener((targetId, type) -> sendReaction(targetId, type));

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        repository = new GroupRepository(requireContext());
        liveData.observe(getViewLifecycleOwner(), this::render);

        swipeRefresh.setOnRefreshListener(this::load);
        load();
    }

    private void load() {
        repository.getGroupPosts(1, 20, liveData);
    }

    private void render(Result<List<Post>> result) {
        if (result == null) return;
        switch (result.status) {
            case LOADING:
                if (adapter.getItemCount() == 0) swipeRefresh.setRefreshing(true);
                break;
            case SUCCESS:
                swipeRefresh.setRefreshing(false);
                adapter.submit(result.data);
                break;
            case ERROR:
                swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(),
                        result.message != null ? result.message : "Có lỗi xảy ra",
                        Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void sendReaction(String targetId, String type) {
        ApiService api = ApiClient.getApiService(requireContext());
        ReactionRequest req = new ReactionRequest(targetId, "Posts", type);
        api.toggleReaction(req).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {}
            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {}
        });
    }
}
