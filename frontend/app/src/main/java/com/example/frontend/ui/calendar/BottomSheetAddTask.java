package com.example.frontend.ui.calendar;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.frontend.R;
import com.example.frontend.data.model.Task;
import com.example.frontend.data.remote.CreateTaskRequest;
import com.example.frontend.data.repository.TaskRepository;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BottomSheetAddTask extends BottomSheetDialogFragment {
    public interface OnTaskSavedListener{

        void onTaskSaved();

    }
    public BottomSheetAddTask() {
    }
    private Calendar calendar = Calendar.getInstance();
    private String selectedPriority = "daily";
    private EditText edtTitle;
    private EditText edtNote;

    private TextView txtDate;
    private TextView txtTime;

    private TextView txtDaily;
    private TextView txtImportant;
    private TextView txtUrgent;

    private MaterialCardView cardDaily;
    private MaterialCardView cardImportant;
    private MaterialCardView cardUrgent;

    private MaterialButton btnSave;
    private ImageButton btnClose;
    private TaskRepository repository;
    private OnTaskSavedListener listener;
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(
                R.layout.bottom_sheet_add_task,
                container,
                false
        );
    }
    @Override
    public void onViewCreated(@NonNull View view,@Nullable Bundle savedInstanceState) {
        edtTitle = view.findViewById(R.id.edtTitle);
        edtNote = view.findViewById(R.id.edtNote);
        txtDate = view.findViewById(R.id.txtDate);
        txtTime = view.findViewById(R.id.txtTime);
        txtDaily = view.findViewById(R.id.txtDaily);
        txtImportant = view.findViewById(R.id.txtImportant);
        txtUrgent = view.findViewById(R.id.txtUrgent);
        cardDaily = view.findViewById(R.id.cardDaily);
        cardImportant = view.findViewById(R.id.cardImportant);
        cardUrgent = view.findViewById(R.id.cardUrgent);
        btnSave = view.findViewById(R.id.btnSave);
        btnClose = view.findViewById(R.id.btnClose);
        repository = new TaskRepository(requireContext());

        txtDate.setOnClickListener(v -> {DatePickerDialog dialog = new DatePickerDialog(requireContext(), (view1, year, month, day) -> {calendar.set(year, month, day);
            SimpleDateFormat sdf =new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            txtDate.setText(sdf.format(calendar.getTime()));},
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });
        txtTime.setOnClickListener(v -> {
            TimePickerDialog dialog = new TimePickerDialog(requireContext(), (view1, hour, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            txtTime.setText(String.format(Locale.getDefault(),"%02d:%02d",hour,minute));},
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true);
            dialog.show();

        });
        cardDaily.setOnClickListener(v -> {selectedPriority = "daily";updatePriorityUI();});
        cardImportant.setOnClickListener(v -> {selectedPriority = "medium";updatePriorityUI();});
        cardUrgent.setOnClickListener(v -> {selectedPriority = "high";updatePriorityUI();});
        btnSave.setOnClickListener(v -> {String title = edtTitle.getText().toString().trim();String description = edtNote.getText().toString().trim();
            if(title.isEmpty()){
                edtTitle.setError("Nhập tiêu đề");
                return;
            }
            if(txtDate.getText().toString().isEmpty()){
                Toast.makeText(requireContext(),"Chọn ngày", Toast.LENGTH_SHORT).show();
                return;
            }
            if(txtTime.getText().toString().isEmpty()){
                Toast.makeText(requireContext(),"Chọn giờ",Toast.LENGTH_SHORT).show();
                return;
            }
            saveTask(title,description);
        });
        btnClose.setOnClickListener(v -> dismiss());
        updatePriorityUI();
    }
    private void updatePriorityUI() {

        resetCard(cardDaily, txtDaily);
        resetCard(cardImportant, txtImportant);
        resetCard(cardUrgent, txtUrgent);
        switch (selectedPriority) {

            case "daily":
                selectCard(cardDaily, txtDaily,
                        "#D1FAE5",
                        "#10B981");
                break;

            case "medium":
                selectCard(cardImportant, txtImportant,
                        "#FEF3C7",
                        "#F59E0B");
                break;

            case "high":
                selectCard(cardUrgent, txtUrgent,
                        "#FEE2E2",
                        "#EF4444");
                break;
        }
    }
    private void resetCard(MaterialCardView card,TextView txt) {
        card.setCardBackgroundColor(Color.WHITE);
        card.setStrokeColor(Color.parseColor("#E5E7EB"));
        txt.setTextColor(Color.parseColor("#6B7280"));

    }
    private void selectCard(MaterialCardView card,TextView txt,String bg,String color) {
        card.setCardBackgroundColor(Color.parseColor(bg));
        card.setStrokeColor(Color.parseColor(color));
        txt.setTextColor(Color.parseColor(color));
    }
    private void saveTask(String title,String description){
        SimpleDateFormat sdf =new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        String dueDate = sdf.format(calendar.getTime());
        CreateTaskRequest request =new CreateTaskRequest(title,description,dueDate,selectedPriority);
        repository.createTask(request).enqueue(new Callback<Task>() {
            @Override
            public void onResponse(Call<Task> call, Response<Task> response) {
                if(response.isSuccessful()){
                    Toast.makeText(requireContext(),"Đã thêm công việc",Toast.LENGTH_SHORT).show();
                    dismiss();
                }else{
                    Toast.makeText(requireContext(),"Thêm thất bại",Toast.LENGTH_SHORT).show();}
                    }
                    @Override
                    public void onFailure(Call<Task> call, Throwable t) {
                        Toast.makeText(requireContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    public void setOnTaskSavedListener(OnTaskSavedListener listener){
        this.listener = listener;
    }
}
