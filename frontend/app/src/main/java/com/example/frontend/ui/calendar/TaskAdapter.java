package com.example.frontend.ui.calendar;

import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.data.model.Task;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList = new ArrayList<>();

    private OnTaskActionListener listener;

    public interface OnTaskActionListener {
        void onTaskClick(Task task);
        void onDeleteClick(Task task);
        void onToggleStatus(Task task);
    }

    public void setOnTaskActionListener(OnTaskActionListener listener) {
        this.listener = listener;
    }

    public void setTasks(List<Task> tasks) {
        this.taskList = tasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);

        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {

        Task task = taskList.get(position);

        holder.txtTitle.setText(task.getTitle());
        holder.txtDescription.setText(task.getDescription());

        formatDateTime(task.getDueDate(), holder);

        //--------------------------
        // Priority Color
        //--------------------------

        switch (task.getPriority()) {

            case "daily":
                holder.viewPriority.setBackgroundColor(Color.parseColor("#10B981"));
                break;

            case "medium":
                holder.viewPriority.setBackgroundColor(Color.parseColor("#F59E0B"));
                break;

            case "high":
                holder.viewPriority.setBackgroundColor(Color.parseColor("#EF4444"));
                break;

            default:
                holder.viewPriority.setBackgroundColor(Color.GRAY);
                break;
        }

        //--------------------------
        // Status
        //--------------------------

        boolean completed = "completed".equals(task.getStatus());

        holder.cbDone.setOnCheckedChangeListener(null);
        holder.cbDone.setChecked(completed);

        if (completed) {

            holder.txtTitle.setPaintFlags(
                    holder.txtTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        } else {

            holder.txtTitle.setPaintFlags(
                    holder.txtTitle.getPaintFlags() &
                            (~Paint.STRIKE_THRU_TEXT_FLAG));
        }

        //--------------------------
        // Checkbox
        //--------------------------

        holder.cbDone.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (listener != null) {

                listener.onToggleStatus(task);

            }

        });

        //--------------------------
        // Delete
        //--------------------------

        holder.btnDelete.setOnClickListener(v -> {

            if (listener != null) {

                listener.onDeleteClick(task);

            }

        });

        //--------------------------
        // Card click
        //--------------------------

        holder.itemView.setOnClickListener(v -> {

            if (listener != null) {

                listener.onTaskClick(task);

            }

        });

    }

    @Override
    public int getItemCount() {

        return taskList == null ? 0 : taskList.size();

    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {

        TextView txtTitle;
        TextView txtDescription;
        TextView txtDate;
        TextView txtTime;

        CheckBox cbDone;

        View viewPriority;

        ImageButton btnDelete;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);

            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtDescription = itemView.findViewById(R.id.txtDescription);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtTime = itemView.findViewById(R.id.txtTime);

            cbDone = itemView.findViewById(R.id.cbDone);

            viewPriority = itemView.findViewById(R.id.viewPriority);

            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    private void formatDateTime(String isoDate, TaskViewHolder holder) {

        try {

            SimpleDateFormat input =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                            Locale.getDefault());

            Date date = input.parse(isoDate);

            SimpleDateFormat dateFormat =
                    new SimpleDateFormat("dd/MM/yyyy",
                            Locale.getDefault());

            SimpleDateFormat timeFormat =
                    new SimpleDateFormat("HH:mm",
                            Locale.getDefault());

            holder.txtDate.setText(dateFormat.format(date));
            holder.txtTime.setText(timeFormat.format(date));

        } catch (ParseException e) {

            holder.txtDate.setText("-");
            holder.txtTime.setText("-");

        }

    }

}