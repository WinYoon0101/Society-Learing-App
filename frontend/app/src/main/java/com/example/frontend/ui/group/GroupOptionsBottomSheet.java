package com.example.frontend.ui.group;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.frontend.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Bottom sheet "Tùy chọn" cho màn hình chi tiết nhóm.
 * Hiển thị danh sách tùy chọn khác nhau cho thành viên thường và admin.
 */
public class GroupOptionsBottomSheet extends BottomSheetDialogFragment {

    // Option ids
    public static final int OPT_MEMBERS         = 1;
    public static final int OPT_MANAGE_CONTENT  = 2;
    public static final int OPT_MANAGE_NOTIF    = 3;
    public static final int OPT_LEAVE           = 4;
    public static final int OPT_APPROVE_POSTS   = 5;
    public static final int OPT_APPROVE_MEMBERS = 6;
    public static final int OPT_SETTINGS        = 7;
    public static final int OPT_DELETE          = 8;

    public interface OnOptionSelectedListener {
        void onOptionSelected(int optionId);
    }

    private static final String ARG_IS_ADMIN = "isAdmin";

    private OnOptionSelectedListener listener;
    private boolean isAdmin;

    public static GroupOptionsBottomSheet newInstance(boolean isAdmin) {
        GroupOptionsBottomSheet f = new GroupOptionsBottomSheet();
        Bundle b = new Bundle();
        b.putBoolean(ARG_IS_ADMIN, isAdmin);
        f.setArguments(b);
        return f;
    }

    public void setOnOptionSelectedListener(OnOptionSelectedListener l) {
        this.listener = l;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_group_options, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) isAdmin = getArguments().getBoolean(ARG_IS_ADMIN, false);

        LinearLayout container = view.findViewById(R.id.optionsContainer);
        LayoutInflater inflater = LayoutInflater.from(getContext());

        addOption(container, inflater, R.drawable.ic_friend, "Tất cả thành viên", OPT_MEMBERS);
        if (isAdmin) {
            addOption(container, inflater, R.drawable.ic_library, "Duyệt bài", OPT_APPROVE_POSTS);
            addOption(container, inflater, R.drawable.ic_profile, "Duyệt thành viên", OPT_APPROVE_MEMBERS);
            addOption(container, inflater, R.drawable.ic_settings, "Cài đặt nhóm", OPT_SETTINGS);
            addOption(container, inflater, R.drawable.ic_delete, "Xóa nhóm", OPT_DELETE);
        } else {
            addOption(container, inflater, R.drawable.ic_library, "Quản lý nội dung", OPT_MANAGE_CONTENT);
            addOption(container, inflater, R.drawable.ic_notify, "Quản lý thông báo", OPT_MANAGE_NOTIF);
            addOption(container, inflater, R.drawable.ic_logout, "Rời nhóm", OPT_LEAVE);
        }
    }

    private void addOption(LinearLayout container, LayoutInflater inflater,
                           int iconRes, String label, int optionId) {
        View row = inflater.inflate(R.layout.item_group_option, container, false);
        ImageView icon = row.findViewById(R.id.imgOptionIcon);
        TextView tv = row.findViewById(R.id.tvOptionLabel);
        icon.setImageResource(iconRes);
        tv.setText(label);
        row.setOnClickListener(v -> {
            if (listener != null) listener.onOptionSelected(optionId);
            dismiss();
        });
        container.addView(row);
    }
}
