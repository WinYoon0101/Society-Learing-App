package com.example.frontend.ui.docs;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.data.model.Document;

import java.util.ArrayList;
import java.util.List;

public class DocsAdapter extends RecyclerView.Adapter<DocsAdapter.ViewHolder> {
    private List<Document> list = new ArrayList<>();
    private final OnDocActionListener listener;

    public interface OnDocActionListener {
        void onDeleteClick(Document doc);
        void onEditClick(Document doc);
        void onItemClick(Document doc);
    }

    public DocsAdapter(OnDocActionListener listener) {
        this.listener = listener;
    }

    // --- HÀM SUBMITLIST HOÀN CHỈNH ---
    public void submitList(List<Document> newList) {
        if (newList == null) {
            int size = list.size();
            list.clear();
            notifyItemRangeRemoved(0, size);
            return;
        }

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() { return list.size(); }
            @Override
            public int getNewListSize() { return newList.size(); }
            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return list.get(oldPos).getId().equals(newList.get(newPos).getId());
            }
            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                return list.get(oldPos).equals(newList.get(newPos));
            }
        });

        list.clear();
        list.addAll(newList);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_docs, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Document doc = list.get(position);
        holder.tvTitle.setText(doc.getTitle());

        // Bạn có thể set ảnh đại diện tài liệu ở đây nếu có (PDF/Word icon)

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(doc);
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(doc);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(doc);
        });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    // --- VIEWHOLDER HOÀN CHỈNH ---
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle, tvViews, tvDownloads;
        ImageView ivFileType, btnDelete, btnEdit;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Map đúng ID từ file XML mới
            tvTitle = itemView.findViewById(R.id.tvDocumentTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            tvViews = itemView.findViewById(R.id.tvViews);
            tvDownloads = itemView.findViewById(R.id.tvDownloads);
            ivFileType = itemView.findViewById(R.id.ivFileType);

            btnDelete = itemView.findViewById(R.id.btnDeleteDoc); // Nút xóa
            btnEdit = itemView.findViewById(R.id.btnEditDoc);     // Nút sửa
        }
    }
}