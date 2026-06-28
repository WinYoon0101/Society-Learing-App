package com.example.frontend.ui.feed;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.User;
import com.example.frontend.ui.profile.ProfileNavigationHelper;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class TaggedUsersBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_IDS = "tag_ids";
    private static final String ARG_NAMES = "tag_names";
    private static final String ARG_AVATARS = "tag_avatars";

    public static TaggedUsersBottomSheet newInstance(List<User> users) {
        TaggedUsersBottomSheet fragment = new TaggedUsersBottomSheet();
        Bundle args = new Bundle();
        ArrayList<String> ids = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> avatars = new ArrayList<>();

        if (users != null) {
            for (User user : users) {
                if (user == null) continue;
                ids.add(user.getId() != null ? user.getId() : "");
                names.add(user.getUsername() != null ? user.getUsername() : "Người dùng");
                avatars.add(user.getAvatar() != null ? user.getAvatar() : "");
            }
        }

        args.putStringArrayList(ARG_IDS, ids);
        args.putStringArrayList(ARG_NAMES, names);
        args.putStringArrayList(ARG_AVATARS, avatars);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tagged_users_bottom_sheet, container, false);
        RecyclerView rvTaggedUsers = view.findViewById(R.id.rvTaggedUsers);
        rvTaggedUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTaggedUsers.setAdapter(new TaggedUserAdapter(requireContext(), readUsers()));
        return view;
    }

    private List<TaggedUser> readUsers() {
        Bundle args = getArguments();
        ArrayList<String> ids = args != null ? args.getStringArrayList(ARG_IDS) : null;
        ArrayList<String> names = args != null ? args.getStringArrayList(ARG_NAMES) : null;
        ArrayList<String> avatars = args != null ? args.getStringArrayList(ARG_AVATARS) : null;

        List<TaggedUser> users = new ArrayList<>();
        int count = names != null ? names.size() : 0;
        for (int i = 0; i < count; i++) {
            String id = ids != null && i < ids.size() ? ids.get(i) : "";
            String name = names.get(i);
            String avatar = avatars != null && i < avatars.size() ? avatars.get(i) : "";
            users.add(new TaggedUser(id, name, avatar));
        }
        return users;
    }

    private static class TaggedUser {
        final String id;
        final String name;
        final String avatar;

        TaggedUser(String id, String name, String avatar) {
            this.id = id;
            this.name = name;
            this.avatar = avatar;
        }
    }

    private class TaggedUserAdapter extends RecyclerView.Adapter<TaggedUserAdapter.VH> {
        private final Context context;
        private final List<TaggedUser> users;

        TaggedUserAdapter(Context context, List<TaggedUser> users) {
            this.context = context;
            this.users = users;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_tagged_user, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            TaggedUser user = users.get(position);
            holder.tvName.setText(user.name);
            Glide.with(context).load(user.avatar).placeholder(R.drawable.ic_user).into(holder.imgAvatar);
            holder.itemView.setOnClickListener(v -> {
                ProfileNavigationHelper.openProfile(context, user.id, user.name, user.avatar);
                dismiss();
            });
        }

        @Override
        public int getItemCount() {
            return users != null ? users.size() : 0;
        }

        class VH extends RecyclerView.ViewHolder {
            CircleImageView imgAvatar;
            TextView tvName;

            VH(@NonNull View itemView) {
                super(itemView);
                imgAvatar = itemView.findViewById(R.id.imgTaggedAvatar);
                tvName = itemView.findViewById(R.id.tvTaggedName);
            }
        }
    }
}
