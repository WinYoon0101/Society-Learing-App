package com.example.frontend.ui.docs;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class DocsPagerAdapter extends FragmentStateAdapter {

    public DocsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new UploadedDocsFragment(); // Tab 1: Đã đăng
        }
        return new SavedDocsFragment(); // Tab 2: Đã lưu
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}