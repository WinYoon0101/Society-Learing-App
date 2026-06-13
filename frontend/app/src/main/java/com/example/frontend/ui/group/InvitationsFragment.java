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
import com.example.frontend.data.model.GroupInvitation;
import com.example.frontend.data.repository.GroupRepository;
import com.example.frontend.utils.Result;

import java.util.List;

public class InvitationsFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rv;
    private TextView tvEmpty;
    private InvitationAdapter adapter;
    private GroupRepository repository;
    private final MutableLiveData<Result<List<GroupInvitation>>> liveData = new MutableLiveData<>();
    private final MutableLiveData<Result<Void>> respondLiveData = new MutableLiveData<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_invitations, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        rv = view.findViewById(R.id.rvInvitations);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        adapter = new InvitationAdapter();
        adapter.setOnRespondListener(new InvitationAdapter.OnRespondListener() {
            @Override
            public void onAccept(GroupInvitation inv, int position) {
                respond(inv.getId(), "accept", position,
                        "Đã tham gia nhóm " + (inv.getGroup() != null ? inv.getGroup().getGroupName() : ""));
            }

            @Override
            public void onDecline(GroupInvitation inv, int position) {
                respond(inv.getId(), "decline", position, "Đã từ chối lời mời");
            }
        });

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        repository = new GroupRepository(requireContext());
        liveData.observe(getViewLifecycleOwner(), this::render);

        swipeRefresh.setOnRefreshListener(this::load);
        load();
    }

    private void load() {
        repository.getInvitations(liveData);
    }

    private void render(Result<List<GroupInvitation>> result) {
        if (result == null) return;
        switch (result.status) {
            case LOADING:
                if (adapter.getItemCount() == 0) swipeRefresh.setRefreshing(true);
                break;
            case SUCCESS:
                swipeRefresh.setRefreshing(false);
                adapter.submit(result.data);
                updateEmptyState();
                break;
            case ERROR:
                swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(),
                        result.message != null ? result.message : "Có lỗi xảy ra",
                        Toast.LENGTH_SHORT).show();
                updateEmptyState();
                break;
        }
    }

    private void respond(String invitationId, String action, int position, String successMsg) {
        MutableLiveData<Result<Void>> live = new MutableLiveData<>();
        live.observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result.status == Result.Status.SUCCESS) {
                adapter.removeAt(position);
                updateEmptyState();
                Toast.makeText(requireContext(), successMsg, Toast.LENGTH_SHORT).show();
            } else if (result.status == Result.Status.ERROR) {
                Toast.makeText(requireContext(),
                        result.message != null ? result.message : "Thao tác thất bại",
                        Toast.LENGTH_SHORT).show();
            }
        });
        repository.respondToInvitation(invitationId, action, live);
    }

    private void updateEmptyState() {
        tvEmpty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }
}
