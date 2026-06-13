package com.example.frontend.ui.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.frontend.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Bottom sheet to pick a friend and start a new chat.
 * UI-only for now: friend list + click-to-start-chat will be wired
 * once BE exposes the friend-list API (see CHAT_CONTEXT.md TODO).
 */
public class SelectFriendBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "SelectFriendBottomSheet";

    public static SelectFriendBottomSheet newInstance() {
        return new SelectFriendBottomSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_select_friend, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton btnClose = view.findViewById(R.id.btnSelectFriendClose);
        EditText etSearch = view.findViewById(R.id.etSearchFriend);

        btnClose.setOnClickListener(v -> dismiss());

        // Search input is wired UI-side only — filtering will be added once
        // the friend-list data source exists. Leaving the field interactive
        // so the visual design can be reviewed end-to-end.
        etSearch.setOnFocusChangeListener((v, hasFocus) -> { /* no-op */ });
    }
}
