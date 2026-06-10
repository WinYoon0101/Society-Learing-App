package com.example.frontend.ui.group;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.Group;
import com.example.frontend.data.repository.GroupRepository;
import com.example.frontend.utils.FileUtils;
import com.example.frontend.utils.Result;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;

import de.hdodenhof.circleimageview.CircleImageView;

public class CreateGroupBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "CreateGroupBottomSheet";

    public interface OnGroupCreatedListener {
        void onGroupCreated();
    }

    private OnGroupCreatedListener createdListener;

    private CircleImageView imgAvatarPreview;
    private EditText etGroupName;
    private TextView tvPrivacyLabel;
    private Button btnCreate;

    private Uri selectedAvatarUri = null;
    private String selectedPrivacy = "";
    private GroupRepository repository;
    private final MutableLiveData<Result<Group>> createLiveData = new MutableLiveData<>();

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedAvatarUri = uri;
                    Glide.with(requireContext())
                            .load(uri)
                            .skipMemoryCache(true)
                            .into(imgAvatarPreview);
                }
            });

    public static CreateGroupBottomSheet newInstance() {
        return new CreateGroupBottomSheet();
    }

    public void setOnGroupCreatedListener(OnGroupCreatedListener l) {
        this.createdListener = l;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            BottomSheetDialog bsd = (BottomSheetDialog) d;
            FrameLayout bs = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bs != null) {
                bs.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
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
        return inflater.inflate(R.layout.dialog_create_group, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imgAvatarPreview = view.findViewById(R.id.imgAvatarPreview);
        etGroupName = view.findViewById(R.id.etGroupName);
        tvPrivacyLabel = view.findViewById(R.id.tvPrivacyLabel);
        btnCreate = view.findViewById(R.id.btnCreate);

        repository = new GroupRepository(requireContext());

        view.findViewById(R.id.btnClose).setOnClickListener(v -> dismiss());

        view.findViewById(R.id.btnPickAvatar).setOnClickListener(v ->
                pickImageLauncher.launch("image/*"));

        view.findViewById(R.id.btnSelectPrivacy).setOnClickListener(v -> {
            PrivacyBottomSheet privacySheet = PrivacyBottomSheet.newInstance(selectedPrivacy);
            privacySheet.setOnPrivacySelectedListener((privacy, label) -> {
                selectedPrivacy = privacy;
                tvPrivacyLabel.setText(label);
                tvPrivacyLabel.setTextColor(0xFF283633);
            });
            privacySheet.show(getChildFragmentManager(), "PrivacySheet");
        });

        btnCreate.setOnClickListener(v -> submitCreate());

        createLiveData.observe(getViewLifecycleOwner(), this::renderCreateState);
    }

    private void submitCreate() {
        String name = etGroupName.getText().toString().trim();
        if (name.isEmpty()) {
            etGroupName.setError("Vui lòng nhập tên nhóm");
            etGroupName.requestFocus();
            return;
        }
        if (selectedPrivacy.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng chọn quyền riêng tư", Toast.LENGTH_SHORT).show();
            return;
        }

        File avatarFile = null;
        if (selectedAvatarUri != null) {
            avatarFile = FileUtils.getFileFromUri(requireContext(), selectedAvatarUri);
        }

        btnCreate.setEnabled(false);
        repository.createGroup(name, selectedPrivacy, avatarFile, createLiveData);
    }

    private void renderCreateState(Result<Group> result) {
        if (result == null) return;
        switch (result.status) {
            case LOADING:
                btnCreate.setEnabled(false);
                break;
            case SUCCESS:
                btnCreate.setEnabled(true);
                Toast.makeText(requireContext(), "Tạo nhóm thành công!", Toast.LENGTH_SHORT).show();
                if (createdListener != null) createdListener.onGroupCreated();
                dismiss();
                break;
            case ERROR:
                btnCreate.setEnabled(true);
                Toast.makeText(requireContext(),
                        result.message != null ? result.message : "Có lỗi xảy ra",
                        Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
