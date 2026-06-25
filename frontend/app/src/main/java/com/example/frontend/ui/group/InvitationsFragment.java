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

/**
 * Tab "Lời mời" – danh sách lời mời tham gia nhóm đang chờ xử lý.
 */
public class InvitationsFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rv;
    private TextView tvEmpty;

    private InvitationAdapter adapter;
    private GroupRepository repository;
    private final MutableLiveData<Result<List<GroupInvitation>>> liveData = new MutableLiveData<>();

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
        rv           = view.findViewById(R.id.rvInvitations);
        tvEmpty      = view.findViewById(R.id.tvEmpty);

        repository = new GroupRepository(requireContext());

        adapter = new InvitationAdapter(new InvitationAdapter.OnRespondListener() {
            @Override
            public void onAccept(GroupInvitation invitation) {
                respond(invitation.getId(), "accept");
            }
            @Override
            public void onDecline(GroupInvitation invitation) {
                respond(invitation.getId(), "decline");
            }
        });

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::load);
        liveData.observe(getViewLifecycleOwner(), this::renderState);

        load();
    }

    private void load() {
        repository.getInvitations(liveData);
    }

    private void respond(String invitationId, String action) {
        MutableLiveData<Result<com.example.frontend.data.model.RequestJoinResult>> respondLive =
                new MutableLiveData<>();
        respondLive.observe(getViewLifecycleOwner(), r -> {
            if (r == null) return;
            if (r.status == Result.Status.SUCCESS) {
                if ("accept".equals(action)) {
                    boolean pending = r.data != null && r.data.isPending();
                    if (pending) {
                        // Nhóm Private: lời mời được chấp nhận → chuyển thành yêu cầu chờ admin duyệt
                        Toast.makeText(requireContext(),
                                "Đã gửi yêu cầu tham gia, chờ quản trị viên duyệt",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Đã tham gia nhóm!", Toast.LENGTH_SHORT).show();
                        GroupState.onJoinedGroup();
                    }
                } else {
                    Toast.makeText(requireContext(), "Đã từ chối lời mời", Toast.LENGTH_SHORT).show();
                }
                load(); // reload list
            } else if (r.status == Result.Status.ERROR) {
                Toast.makeText(requireContext(),
                        r.message != null ? r.message : "Có lỗi xảy ra",
                        Toast.LENGTH_SHORT).show();
            }
        });
        repository.respondToInvitation(invitationId, action, respondLive);
    }

    private void renderState(Result<List<GroupInvitation>> result) {
        if (result == null) return;
        switch (result.status) {
            case LOADING:
                swipeRefresh.setRefreshing(true);
                break;
            case SUCCESS:
                swipeRefresh.setRefreshing(false);
                List<GroupInvitation> data = result.data;
                adapter.submit(data);
                tvEmpty.setVisibility(
                        (data == null || data.isEmpty()) ? View.VISIBLE : View.GONE);
                break;
            case ERROR:
                swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(),
                        result.message != null ? result.message : "Có lỗi xảy ra",
                        Toast.LENGTH_SHORT).show();
                tvEmpty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                break;
        }
    }
}