package com.example.frontend.ui.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.data.model.User;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

/** Danh sách thành viên group; click 1 thành viên → Xem trang cá nhân / Đổi biệt danh. */
public class GroupMembersBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "GroupMembersBottomSheet";

    public static GroupMembersBottomSheet newInstance() {
        return new GroupMembersBottomSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_chat_members, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton btnClose = view.findViewById(R.id.btnChatMembersClose);
        TextView tvTitle = view.findViewById(R.id.tvChatMembersTitle);
        RecyclerView rv = view.findViewById(R.id.rvChatMembers);

        btnClose.setOnClickListener(v -> dismiss());

        final ChatDetailFragment host = getHost2();
        if (host == null) {
            dismiss();
            return;
        }

        List<User> members = host.getMembers() != null ? host.getMembers() : new ArrayList<>();
        tvTitle.setText("Thành viên (" + members.size() + ")");

        ChatMemberAdapter adapter = new ChatMemberAdapter(members, host.getCurrentUserIdValue(),
                new ChatMemberAdapter.MemberCallback() {
                    @Override
                    public String displayName(User member) {
                        return host.getMemberDisplayName(member);
                    }

                    @Override
                    public void onMemberClick(User member) {
                        showMemberActions(host, member);
                    }
                });
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);
    }

    private void showMemberActions(ChatDetailFragment host, User member) {
        String name = host.getMemberDisplayName(member);

        boolean isSelf = member.getId() != null
                && member.getId().equals(host.getCurrentUserIdValue());
        boolean canKick = host.isCurrentUserAdmin() && !isSelf;

        final List<String> actions = new ArrayList<>();
        actions.add("Xem trang cá nhân");
        actions.add("Đổi biệt danh");
        if (canKick) actions.add("Xóa khỏi nhóm");

        new AlertDialog.Builder(requireContext())
                .setTitle(name)
                .setItems(actions.toArray(new String[0]), (d, which) -> {
                    String action = actions.get(which);
                    if ("Xem trang cá nhân".equals(action)) {
                        host.openProfileFor(member);
                    } else if ("Đổi biệt danh".equals(action)) {
                        host.showNicknameDialogFor(member);
                    } else if ("Xóa khỏi nhóm".equals(action)) {
                        host.kickMember(member);
                    }
                    dismiss();
                })
                .show();
    }

    @Nullable
    private ChatDetailFragment getHost2() {
        Fragment parent = getParentFragment();
        return parent instanceof ChatDetailFragment ? (ChatDetailFragment) parent : null;
    }
}
