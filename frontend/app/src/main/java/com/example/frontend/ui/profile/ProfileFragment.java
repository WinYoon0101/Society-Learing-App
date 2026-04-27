package com.example.frontend.ui.profile;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

import com.example.frontend.R;
import com.example.frontend.data.model.User;
import com.example.frontend.data.repository.UserRepository;
import com.example.frontend.utils.Result;

import java.io.File;

public class ProfileFragment extends Fragment {

    private static final int PICK_IMAGE = 100;

    private ImageView imgAvatar;
    private TextView tvName, tvBio, tvStats;
    private Button btnEdit;

    private UserRepository repository;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        imgAvatar = view.findViewById(R.id.imgAvatar);
        tvName = view.findViewById(R.id.tvUserName);
        tvBio = view.findViewById(R.id.tvBio);
        tvStats = view.findViewById(R.id.tvStats);
        btnEdit = view.findViewById(R.id.btnEdit);

        repository = new UserRepository(requireContext());

        loadProfile();

        imgAvatar.setOnClickListener(v -> openGallery());
        btnEdit.setOnClickListener(v -> openGallery());

        return view;
    }

    private void loadProfile() {
        repository.getProfile().observe(getViewLifecycleOwner(), result -> {
            if (result.status == Result.Status.SUCCESS && result.data != null) {
                User user = result.data;

                tvName.setText(user.getUsername());
                tvBio.setText("Hello " + user.getUsername());
                tvStats.setText("0 friends • 0 groups");

                Glide.with(this)
                        .load(user.getAvatar())
                        .placeholder(R.drawable.ic_profile)
                        .into(imgAvatar);
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();

            imgAvatar.setImageURI(uri);

            File file = new File(uri.getPath());

            repository.uploadAvatar(file).observe(getViewLifecycleOwner(), result -> {
                if (result.status == Result.Status.SUCCESS) {
                    loadProfile();
                }
            });
        }
    }
}