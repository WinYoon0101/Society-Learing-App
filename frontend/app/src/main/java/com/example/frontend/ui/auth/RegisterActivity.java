package com.example.frontend.ui.auth;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.frontend.R;
import com.example.frontend.data.model.LoginResponse;
import com.example.frontend.ui.main.HomeActivity;
import com.example.frontend.utils.Result;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;

public class RegisterActivity extends AppCompatActivity {

    private RegisterViewModel viewModel;
    private TextInputEditText edtFullName, edtEmail, edtPassword, edtConfirmPassword, edtDateOfBirth;
    private RadioGroup rgGender;
    private MaterialButton btnRegister;
    private TextView tvLoginLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 1. Ánh xạ XML
        edtFullName = findViewById(R.id.edtFullName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        edtDateOfBirth = findViewById(R.id.edtDateOfBirth);
        rgGender = findViewById(R.id.rgGender);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);

        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        // 2. NÚT CHUYỂN VỀ LOGIN
        tvLoginLink.setOnClickListener(v -> {
            finish(); // Tắt màn hình Đăng ký hiện tại, nó sẽ tự lùi về Login
        });

        // 3. HIỆN LỊCH CHỌN NGÀY SINH
        edtDateOfBirth.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    R.style.MyDatePickerStyle,
                    (view, selectedYear, selectedMonth, selectedDay) -> {

                        //  Ngày/Tháng/Năm
                        String date = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);

                        edtDateOfBirth.setText(date);
                    }, year, month, day);

            datePickerDialog.show();

            // Ép màu nút lần nữa cho chắc
            datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.green_main));
            datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.green_main));
        });

        // 4. XỬ LÝ NÚT ĐĂNG KÝ
        btnRegister.setOnClickListener(v -> {
            String fullName = edtFullName.getText().toString().trim();
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();
            String confirmPassword = edtConfirmPassword.getText().toString().trim();
            String dateOfBirth = edtDateOfBirth.getText().toString().trim();

            // Lấy Giới tính
            String gender = "other";
            int selectedGenderId = rgGender.getCheckedRadioButtonId();
            if (selectedGenderId == R.id.rbMale) {
                gender = "male";
            } else if (selectedGenderId == R.id.rbFemale) {
                gender = "female";
            }

            // Kiểm tra lỗi cơ bản
            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || dateOfBirth.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Bắn API
            viewModel.register(fullName, email, password, dateOfBirth, gender);
            observeViewModel();
        });
    }

    private void observeViewModel() {
        viewModel.getAuthResult().observe(this, new Observer<Result<LoginResponse>>() {
            @Override
            public void onChanged(Result<LoginResponse> result) {
                if (result == null) return;

                switch (result.status) {

                    case LOADING:
                        btnRegister.setEnabled(false);
                        btnRegister.setText("Đang xử lý...");
                        break;

                    case SUCCESS:
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Đăng ký");

                        if (result.data != null && result.data.getUser() != null) {

                            String token = result.data.getAccessToken();
                            String username = result.data.getUser().getUsername();
                            String userId = result.data.getUser().getId();
                            String avatar = result.data.getUser().getAvatar();

                            android.util.Log.d("RegisterActivity", "userId saved: [" + userId + "]");

                            // Lưu đầy đủ giống LoginActivity
                            SharedPreferences sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putString("JWT_TOKEN", token);
                            editor.putString("USER_ID", userId);
                            editor.putString("USER_AVATAR", avatar);
                            editor.putString("USERNAME", username);
                            editor.apply();

                            Toast.makeText(RegisterActivity.this,
                                    "Đăng ký thành công! Chào mừng " + username,
                                    Toast.LENGTH_SHORT).show();

                            // ✅ chuyển Home
                            Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();

                        } else {
                            Toast.makeText(RegisterActivity.this,
                                    "Lỗi dữ liệu từ server",
                                    Toast.LENGTH_SHORT).show();
                        }
                        break;

                    case ERROR:
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Đăng ký");
                        Toast.makeText(RegisterActivity.this, result.message, Toast.LENGTH_LONG).show();
                        break;
                }
            }
        });
    }
}