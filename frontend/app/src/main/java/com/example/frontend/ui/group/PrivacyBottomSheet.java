package com.example.frontend.ui.group;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.frontend.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class PrivacyBottomSheet extends BottomSheetDialogFragment {

    public interface OnPrivacySelectedListener {
        void onPrivacySelected(String privacy, String label);
    }

    private static final String ARG_CURRENT = "current";

    private OnPrivacySelectedListener listener;
    private String currentPrivacy;

    public static PrivacyBottomSheet newInstance(String currentPrivacy) {
        PrivacyBottomSheet f = new PrivacyBottomSheet();
        Bundle b = new Bundle();
        b.putString(ARG_CURRENT, currentPrivacy);
        f.setArguments(b);
        return f;
    }

    public void setOnPrivacySelectedListener(OnPrivacySelectedListener l) {
        this.listener = l;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            BottomSheetDialog bsd = (BottomSheetDialog) d;
            FrameLayout bs = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bs != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bs);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_privacy, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        currentPrivacy = getArguments() != null
                ? getArguments().getString(ARG_CURRENT, "") : "";

        RadioButton rbPublic = view.findViewById(R.id.rbPublic);
        RadioButton rbPrivate = view.findViewById(R.id.rbPrivate);
        LinearLayout optionPublic = view.findViewById(R.id.optionPublic);
        LinearLayout optionPrivate = view.findViewById(R.id.optionPrivate);

        updateSelection(rbPublic, rbPrivate);

        optionPublic.setOnClickListener(v -> {
            currentPrivacy = "Public";
            updateSelection(rbPublic, rbPrivate);
        });

        optionPrivate.setOnClickListener(v -> {
            currentPrivacy = "Private";
            updateSelection(rbPublic, rbPrivate);
        });

        view.findViewById(R.id.btnDone).setOnClickListener(v -> {
            if (listener != null && !currentPrivacy.isEmpty()) {
                String label = currentPrivacy.equals("Public") ? "Công khai" : "Riêng tư";
                listener.onPrivacySelected(currentPrivacy, label);
            }
            dismiss();
        });

        view.findViewById(R.id.btnPrivacyBack).setOnClickListener(v -> dismiss());
    }

    private void updateSelection(RadioButton rbPublic, RadioButton rbPrivate) {
        rbPublic.setChecked("Public".equals(currentPrivacy));
        rbPrivate.setChecked("Private".equals(currentPrivacy));
    }
}
