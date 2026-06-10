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

            CreatePostFragment fragment = new CreatePostFragment();
            if (groupId != null && !groupId.isEmpty()) {
                Bundle args = new Bundle();
                args.putString("groupId", groupId);
                fragment.setArguments(args);
            }

            getSupportFragmentManager()
                    .beginTransaction()

                    .replace(R.id.fragment_container, fragment)

                    .commit();
        }
    }
}