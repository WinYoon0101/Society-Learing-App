package com.example.frontend.ui.group;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.frontend.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Bottom sheet "Quản lý thông báo" của nhóm (G-B4).
 * Chọn mức nhận thông báo: tất cả / chỉ nổi bật / tắt. Lưu LOCAL qua GroupState.
 */
public class GroupNotifBottomSheet extends BottomSheetDialogFragment {

    public interface OnLevelSelectedListener {
        void onLevelSelected(String level);
    }

    private static final String ARG_CURRENT = "current";

    private OnLevelSelectedListener listener;
    private String current;

    public static GroupNotifBottomSheet newInstance(String currentLevel) {
        GroupNotifBottomSheet f = new GroupNotifBottomSheet();
        Bundle b = new Bundle();
        b.putString(ARG_CURRENT, currentLevel);
        f.setArguments(b);
        return f;
    }

    public void setOnLevelSelectedListener(OnLevelSelectedListener l) {
        this.listener = l;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_group_notif, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        current = getArguments() != null
                ? getArguments().getString(ARG_CURRENT, GroupState.NOTIF_ALL) : GroupState.NOTIF_ALL;

        RadioButton rbAll = view.findViewById(R.id.rbAll);
        RadioButton rbHighlight = view.findViewById(R.id.rbHighlight);
        RadioButton rbOff = view.findViewById(R.id.rbOff);

        Runnable refresh = () -> {
            rbAll.setChecked(GroupState.NOTIF_ALL.equals(current));
            rbHighlight.setChecked(GroupState.NOTIF_HIGHLIGHT.equals(current));
            rbOff.setChecked(GroupState.NOTIF_OFF.equals(current));
        };
        refresh.run();

        view.findViewById(R.id.optionAll).setOnClickListener(v -> select(GroupState.NOTIF_ALL));
        view.findViewById(R.id.optionHighlight).setOnClickListener(v -> select(GroupState.NOTIF_HIGHLIGHT));
        view.findViewById(R.id.optionOff).setOnClickListener(v -> select(GroupState.NOTIF_OFF));
    }

    private void select(String level) {
        current = level;
        if (listener != null) listener.onLevelSelected(level);
        dismiss();
    }
}
