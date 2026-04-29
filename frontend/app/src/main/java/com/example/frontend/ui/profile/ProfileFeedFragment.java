import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.data.model.ApiResponse;
import com.example.frontend.data.model.Post;
import com.example.frontend.data.repository.PostRepository;
import com.example.frontend.ui.profile.ProfilePostAdapter;

import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFeedFragment extends Fragment {

    private RecyclerView rvPosts;
    private ProfilePostAdapter adapter;
    private PostRepository repository;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile_feed, container, false);

        rvPosts = view.findViewById(R.id.rvPosts);

        rvPosts.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new ProfilePostAdapter(getContext(), new ArrayList<>());
        rvPosts.setAdapter(adapter);

        repository = new PostRepository(getContext());

        loadMyPosts();

        return view;
    }

    private void loadMyPosts() {
        repository.fetchAllPosts(new Callback<ApiResponse<List<Post>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Post>>> call, Response<ApiResponse<List<Post>>> response) {

                if (response.isSuccessful() && response.body() != null) {

                    List<Post> allPosts = response.body().data;
                    List<Post> myPosts = new ArrayList<>();

                    String myId = getCurrentUserId();

                    for (Post post : allPosts) {
                        if (post.getAuthorId() != null &&
                                post.getAuthorId().getId().equals(myId)) {

                            myPosts.add(post);
                        }
                    }

                    if (myPosts.isEmpty()) {
                        rvPosts.setVisibility(View.GONE);
                    } else {
                        rvPosts.setVisibility(View.VISIBLE);
                        adapter.updateData(myPosts);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Post>>> call, Throwable t) {

            }
            private String getCurrentUserId() {
                return getContext()
                        .getSharedPreferences("USER", Context.MODE_PRIVATE)
                        .getString("userId", "");
            }
        });
    }
}

