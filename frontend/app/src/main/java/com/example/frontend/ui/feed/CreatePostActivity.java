package com.example.frontend.ui.feed;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.frontend.R;

/**
 * Activity wrapper cho CreatePostFragment.
 * Truyền extra "groupId" nếu đăng bài vào nhóm.
 */
public class CreatePostActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        if (savedInstanceState == null) {
            String groupId = getIntent().getStringExtra("groupId");

            String scannedContent = getIntent().getStringExtra("SCANNED_CONTENT");

            CreatePostFragment fragment = new CreatePostFragment();
            Bundle args = new Bundle();
            if (groupId != null && !groupId.isEmpty()) {
                args.putString("groupId", groupId);
            }

            // Thêm SCANNED_CONTENT vào Bundle
            if (scannedContent != null && !scannedContent.isEmpty()) {
                args.putString("SCANNED_CONTENT", scannedContent);
            }

            // Nếu Bundle có chứa dữ liệu thì truyền cho Fragment
            if (!args.isEmpty()) {
                fragment.setArguments(args);
            }

            getSupportFragmentManager()
                    .beginTransaction()

                    .replace(R.id.fragment_container, fragment)

                    .commit();
        }
    }
}