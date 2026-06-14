package com.example.frontend.ui.group;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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

    private String groupId;
    private Uri selectedAvatarUri = null;
    private String selectedPrivacy;

    private CircleImageView imgAvatar;
    private EditText etName, etDescription;
    private TextView tvPrivacyLabel;
    private Button btnSave;
    private ImageButton btnBack;

    private GroupRepository repository;
    private final MutableLiveData<Result<GroupDetail>> updateLive = new MutableLiveData<>();

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedAvatarUri = uri;
                    Glide.with(this).load(uri).into(imgAvatar);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_group);

        groupId = getIntent().getStringExtra(EXTRA_GROUP_ID);
        if (groupId == null) { finish(); return; }

        imgAvatar     = findViewById(R.id.imgAvatarPreview);
        etName        = findViewById(R.id.etGroupName);
        etDescription = findViewById(R.id.etDescription);
        tvPrivacyLabel= findViewById(R.id.tvPrivacyLabel);
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

        btnBack.setOnClickListener(v -> finish());

        findViewById(R.id.btnPickAvatar).setOnClickListener(v -> pickImage.launch("image/*"));

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
            btnSave.setEnabled(r.status != Result.Status.LOADING);
            if (r.status == Result.Status.SUCCESS) {
                Toast.makeText(this, "Cập nhật nhóm thành công!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else if (r.status == Result.Status.ERROR) {
                Toast.makeText(this, r.message != null ? r.message : "Có lỗi xảy ra",
                        Toast.LENGTH_SHORT).show();
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
                selectedPrivacy, avatarFile, updateLive);
    }
}