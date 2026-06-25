package com.example.frontend.ui.group;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.GroupDetail;
import com.example.frontend.data.repository.GroupRepository;
import com.example.frontend.utils.FileUtils;
import com.example.frontend.utils.Result;

import java.io.File;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditGroupActivity extends AppCompatActivity {

    public static final String EXTRA_GROUP_ID   = "groupId";
    public static final String EXTRA_GROUP_NAME = "groupName";
    public static final String EXTRA_DESCRIPTION = "description";
    public static final String EXTRA_PRIVACY    = "privacy";
    public static final String EXTRA_AVATAR_URL = "avatarUrl";
    public static final String EXTRA_COVER_URL  = "coverUrl";
    public static final String EXTRA_REQUIRE_APPROVAL = "requirePostApproval";

    private String groupId;
    private Uri selectedAvatarUri = null;
    private Uri selectedCoverUri = null;
    private String selectedPrivacy;

    private CircleImageView imgAvatar;
    private ImageView imgCoverPreview;
    private EditText etName, etDescription;
    private TextView tvPrivacyLabel;
    private androidx.appcompat.widget.SwitchCompat switchRequireApproval;
    private Button btnSave;
    private ImageButton btnBack;

    private GroupRepository repository;
    private final MutableLiveData<Result<GroupDetail>> updateLive = new MutableLiveData<>();
    private final MutableLiveData<Result<Object>> coverLive = new MutableLiveData<>();

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedAvatarUri = uri;
                    Glide.with(this).load(uri).into(imgAvatar);
                }
            });

    private final ActivityResultLauncher<String> pickCover =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedCoverUri = uri;
                    Glide.with(this).load(uri).into(imgCoverPreview);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_group);

        groupId = getIntent().getStringExtra(EXTRA_GROUP_ID);
        if (groupId == null) { finish(); return; }

        imgAvatar     = findViewById(R.id.imgAvatarPreview);
        imgCoverPreview = findViewById(R.id.imgCoverPreview);
        etName        = findViewById(R.id.etGroupName);
        etDescription = findViewById(R.id.etDescription);
        tvPrivacyLabel= findViewById(R.id.tvPrivacyLabel);
        switchRequireApproval = findViewById(R.id.switchRequireApproval);
        btnSave       = findViewById(R.id.btnSave);
        btnBack       = findViewById(R.id.btnBack);

        repository = new GroupRepository(this);

        // Điền dữ liệu hiện tại
        etName.setText(getIntent().getStringExtra(EXTRA_GROUP_NAME));
        etDescription.setText(getIntent().getStringExtra(EXTRA_DESCRIPTION));
        selectedPrivacy = getIntent().getStringExtra(EXTRA_PRIVACY);
        tvPrivacyLabel.setText("Public".equals(selectedPrivacy) ? "🌐 Công khai" : "🔒 Riêng tư");
        String avatarUrl = getIntent().getStringExtra(EXTRA_AVATAR_URL);
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this).load(avatarUrl).placeholder(R.drawable.ic_group).into(imgAvatar);
        }
        String coverUrl = getIntent().getStringExtra(EXTRA_COVER_URL);
        if (coverUrl != null && !coverUrl.isEmpty()) {
            Glide.with(this).load(coverUrl)
                    .placeholder(R.drawable.bg_group_cover_default).into(imgCoverPreview);
        }
        switchRequireApproval.setChecked(getIntent().getBooleanExtra(EXTRA_REQUIRE_APPROVAL, false));

        btnBack.setOnClickListener(v -> finish());

        findViewById(R.id.btnPickAvatar).setOnClickListener(v -> pickImage.launch("image/*"));
        findViewById(R.id.btnPickCover).setOnClickListener(v -> pickCover.launch("image/*"));

        findViewById(R.id.btnSelectPrivacy).setOnClickListener(v -> {
            PrivacyBottomSheet sheet = PrivacyBottomSheet.newInstance(selectedPrivacy);
            sheet.setOnPrivacySelectedListener((privacy, label) -> {
                selectedPrivacy = privacy;
                tvPrivacyLabel.setText(label);
            });
            sheet.show(getSupportFragmentManager(), "PrivacySheet");
        });

        btnSave.setOnClickListener(v -> submit());

        updateLive.observe(this, r -> {
            if (r == null) return;
            if (r.status == Result.Status.SUCCESS) {
                // Thông tin (tên/mô tả/privacy/avatar) đã lưu; nếu có chọn ảnh bìa thì upload tiếp
                if (selectedCoverUri != null) {
                    File coverFile = FileUtils.getFileFromUri(this, selectedCoverUri);
                    repository.updateGroupCover(groupId, coverFile, coverLive);
                } else {
                    Toast.makeText(this, "Cập nhật nhóm thành công!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                }
            } else if (r.status == Result.Status.ERROR) {
                btnSave.setEnabled(true);
                Toast.makeText(this, r.message != null ? r.message : "Có lỗi xảy ra",
                        Toast.LENGTH_SHORT).show();
            }
        });

        coverLive.observe(this, r -> {
            if (r == null) return;
            if (r.status == Result.Status.SUCCESS) {
                Toast.makeText(this, "Cập nhật nhóm thành công!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else if (r.status == Result.Status.ERROR) {
                btnSave.setEnabled(true);
                // Thông tin đã lưu, chỉ ảnh bìa lỗi
                Toast.makeText(this, "Đã lưu nhóm nhưng đổi ảnh bìa thất bại: "
                        + (r.message != null ? r.message : ""), Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    private void submit() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            etName.setError("Vui lòng nhập tên nhóm");
            etName.requestFocus();
            return;
        }

        File avatarFile = null;
        if (selectedAvatarUri != null) {
            avatarFile = FileUtils.getFileFromUri(this, selectedAvatarUri);
        }

        btnSave.setEnabled(false);
        repository.updateGroup(groupId, name,
                etDescription.getText().toString().trim(),
                selectedPrivacy, switchRequireApproval.isChecked(), avatarFile, updateLive);
    }
}