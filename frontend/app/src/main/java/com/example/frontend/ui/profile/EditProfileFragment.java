package com.example.frontend.ui.profile;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.frontend.R;

import com.example.frontend.data.model.UpdateProfile;
import com.example.frontend.data.model.User;
import com.example.frontend.data.repository.UserRepository;
import com.example.frontend.utils.Result;
import com.example.frontend.utils.SimpleTextWatcher;

import java.util.Calendar;

public class EditProfileFragment extends Fragment {

    private EditText edtBio, edtLocation, edtHometown, edtGender, edtBirthday;
    private Button btnSave;
    private ImageButton btnBack;
    private boolean isChanged = false;
    private boolean isLoading = false;

    private UserRepository repository;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        edtBio = view.findViewById(R.id.edtBio);
        edtLocation = view.findViewById(R.id.edtLocation);
        edtHometown = view.findViewById(R.id.edtHometown);
        edtGender = view.findViewById(R.id.edtGender);
        edtBirthday = view.findViewById(R.id.edtBirthday);
        btnSave = view.findViewById(R.id.btnSave);
        btnBack = view.findViewById(R.id.btnBack);

        repository = new UserRepository(requireContext());

        loadData();
        setupChangeListener();
        edtBirthday.setFocusable(false);
        edtBirthday.setClickable(true);
        edtBirthday.setInputType(0);

        edtBirthday.setOnClickListener(v -> {

            Calendar c = Calendar.getInstance();

            new DatePickerDialog(requireContext(),
                    (picker, year, month, day) -> {

                        String formatted = String.format("%04d-%02d-%02d",
                                year, month + 1, day);

                        edtBirthday.setText(formatted);

                    },
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
        edtGender.setFocusable(false);
        edtGender.setClickable(true);
        edtGender.setInputType(0);

        edtGender.setOnClickListener(v -> {

            String[] options = {"Nam", "Nữ"};

            new AlertDialog.Builder(requireContext())
                    .setTitle("Chọn giới tính")
                    .setItems(options, (dialog, which) -> {

                        if (which == 0) {
                            edtGender.setText("Nam");
                        } else {
                            edtGender.setText("Nữ");
                        }

                    })
                    .show();
        });
        btnSave.setOnClickListener(v -> confirmSave());
        btnBack.setOnClickListener(v->showConfirmExit());

        return view;
    }
    private void loadData() {
        isLoading = true;
        repository.getProfile().observe(getViewLifecycleOwner(), r -> {
            if (r.status == Result.Status.SUCCESS && r.data != null) {

                User u = r.data;

                edtBio.setText(u.getBio());
                edtLocation.setText(u.getLocation());
                edtHometown.setText(u.getHometown());
                edtGender.setText(u.getGender());
                edtBirthday.setText(u.getBirthday());

                isChanged = false;
                isLoading = false;
            }
        });
    }
    private void setupChangeListener() {

        TextWatcher watcher = new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isLoading) {
                    isChanged = true;
                }
            }
        };

        edtBio.addTextChangedListener(watcher);
        edtLocation.addTextChangedListener(watcher);
        edtHometown.addTextChangedListener(watcher);
        edtGender.addTextChangedListener(watcher);
        edtBirthday.addTextChangedListener(watcher);
    }

    private void showConfirmExit() {
        if (isChanged == false){
            requireActivity().finish();
        }
        else{
            new AlertDialog.Builder(requireContext())
                    .setTitle("Lưu thay đổi?")
                    .setPositiveButton("Lưu", (d, w) -> saveData())
                    .setNeutralButton("Thoát", (d, w) -> {
                        requireActivity().finish();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        }
    }
    private void confirmSave() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Lưu thay đổi?")
                .setPositiveButton("Lưu", (d, w) -> saveData())
                .setNegativeButton("Hủy", null)
                .show();
    }
    private void saveData() {

        String genderInput = edtGender.getText().toString().trim();

        String gender;
        if (genderInput.equalsIgnoreCase("Nam")) {
            gender = "Nam";
        } else {
            gender = "Nữ";
        }

        UpdateProfile req = new UpdateProfile(
                edtBio.getText().toString(),
                edtLocation.getText().toString(),
                edtHometown.getText().toString(),
                gender,
                edtBirthday.getText().toString()
        );

        repository.updateProfile(req)
                .observe(getViewLifecycleOwner(), r -> {
                    if (r.status == Result.Status.SUCCESS) {
                        isChanged = false;
                        requireActivity().finish();
                    }
                });
        Log.d("DEBUG", "DOB gửi = " + edtBirthday.getText().toString());
    }
}