package com.example.frontend.ui.feed;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.data.model.ReactionItem;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class ReactionListBottomSheet extends BottomSheetDialogFragment {

    private RecyclerView rvReactionUsers;
    private TabLayout tabLayoutReactions;
    private UserReactionAdapter adapter;
    private ReactionViewModel viewModel;

    // Lưu ý quan trọng: Phải luôn khởi tạo List rỗng
    private List<ReactionItem> fullList = new ArrayList<>();
    private String targetId;

    public static ReactionListBottomSheet newInstance(String targetId) {
        ReactionListBottomSheet fragment = new ReactionListBottomSheet();
        Bundle args = new Bundle();
        args.putString("TARGET_ID", targetId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feed_bottom_sheet__reaction, container, false);

        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        rvReactionUsers = view.findViewById(R.id.rvReactionUsers);
        tabLayoutReactions = view.findViewById(R.id.tabLayoutReactions);

        rvReactionUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UserReactionAdapter(getContext(), new ArrayList<>());
        rvReactionUsers.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ReactionViewModel.class);
        viewModel.init(requireContext());

        viewModel.getReactionList().observe(getViewLifecycleOwner(), reactionItems -> {
            if (reactionItems != null) {
                // CHỐNG MẤT DATA BƯỚC 1: Bọc new ArrayList để không dính tham chiếu
                fullList = new ArrayList<>(reactionItems);

                // Mặc định hiển thị tab Tất cả
                adapter.updateData(new ArrayList<>(fullList));
                updateDynamicTabs();
            }
        });

        if (getArguments() != null) {
            targetId = getArguments().getString("TARGET_ID");
            viewModel.fetchReactions(targetId);
        }

        tabLayoutReactions.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getTag() != null) {
                    filterListByTag((String) tab.getTag());
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        return view;
    }

    private void updateDynamicTabs() {
        int all = fullList.size();
        int like = 0, love = 0, haha = 0, wow = 0, sad = 0, angry = 0;

        for (ReactionItem item : fullList) {
            if (item.getType() == null) continue;
            String type = item.getType().trim().toLowerCase();

            switch (type) {
                case "like": like++; break;
                case "love": love++; break;
                case "haha": haha++; break;
                case "wow":  wow++; break;
                case "sad":  sad++; break;
                case "angry": angry++; break;
            }
        }

        tabLayoutReactions.removeAllTabs();
        addTab("Tất cả " + all, 0, "All");

        if (like > 0) addTab(" " + like, R.drawable.ic_like_color, "Like");
        if (love > 0) addTab(" " + love, R.drawable.ic_love, "Love");
        if (haha > 0) addTab(" " + haha, R.drawable.ic_haha, "Haha");
        if (wow > 0)  addTab(" " + wow,  R.drawable.ic_wow,  "Wow");
        if (sad > 0)  addTab(" " + sad,  R.drawable.ic_sad,  "Sad");
        if (angry > 0) addTab(" " + angry, R.drawable.ic_angry, "Angry");
    }

    private void addTab(String text, int iconResId, String tag) {
        TabLayout.Tab tab = tabLayoutReactions.newTab().setText(text);
        if (iconResId != 0) tab.setIcon(iconResId);
        tab.setTag(tag);
        tabLayoutReactions.addTab(tab);
    }

    private void filterListByTag(String tag) {
        if (tag == null || tag.equalsIgnoreCase("All")) {
            // CHỐNG MẤT DATA BƯỚC 2: Truyền List mới để tái sử dụng List gốc
            adapter.updateData(new ArrayList<>(fullList));
            return;
        }

        List<ReactionItem> filteredList = new ArrayList<>();

        for (ReactionItem item : fullList) {
            String typeFromBackend = item.getType() != null ? item.getType().trim() : "";

            // So sánh bỏ qua khác biệt HOA/thường và khoảng trắng
            if (typeFromBackend.equalsIgnoreCase(tag.trim())) {
                filteredList.add(item);
            }
        }

        adapter.updateData(filteredList);
    }
}