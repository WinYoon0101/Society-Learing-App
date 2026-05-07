package com.example.frontend.ui.profile;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.frontend.R;

public class EditProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.edit_container, new EditProfileFragment())
                    .commit();
        }
    }
}
