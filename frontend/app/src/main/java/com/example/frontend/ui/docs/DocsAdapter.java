package com.example.frontend.ui.docs;

import android.graphics.Color;
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
import com.google.android.material.card.MaterialCardView;

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
        // Lưu ý: Đảm bảo tên file layout là item_docs (hoặc item_my_document tùy bạn đặt)
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_docs, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Document doc = list.get(position);

        // 1. Đổ dữ liệu text
        holder.tvTitle.setText(doc.getTitle());
        holder.tvSubtitle.setText(doc.getSubject() + " • " + (doc.getUploaderName() != null ? doc.getUploaderName() : "Hệ thống"));
        holder.tvViews.setText(String.valueOf(doc.getNumberView()));
        holder.tvDownloads.setText(String.valueOf(doc.getNumberDownload()));

        if (doc.getCreatedAt() != null && doc.getCreatedAt().length() > 10) {
            holder.tvTime.setText(doc.getCreatedAt().substring(0, 10));
        }

        // 2. Logic đổi Icon và Màu sắc dựa trên đuôi file URL
        String url = (doc.getFileUrl() != null) ? doc.getFileUrl().toLowerCase() : "";

        // Reset về mặc định trước khi check
        holder.ivFileType.setColorFilter(null);
        holder.iconCard.setCardBackgroundColor(Color.parseColor("#F1F3F0"));

        if (url.contains(".pdf")) {
            holder.ivFileType.setImageResource(R.drawable.ic_pdf);
            holder.ivFileType.setColorFilter(Color.parseColor("#E53935"));
            holder.iconCard.setCardBackgroundColor(Color.parseColor("#FFEBEE"));
        }
        else if (url.contains(".doc") || url.contains(".docx")) {
            holder.ivFileType.setImageResource(R.drawable.ic_word);
            holder.ivFileType.setColorFilter(Color.parseColor("#1E88E5"));
            holder.iconCard.setCardBackgroundColor(Color.parseColor("#E3F2FD"));
        }
        else if (url.contains(".ppt") || url.contains(".pptx")) {
            holder.ivFileType.setImageResource(android.R.drawable.ic_menu_slideshow);
            holder.ivFileType.setColorFilter(Color.parseColor("#F57C00"));
            holder.iconCard.setCardBackgroundColor(Color.parseColor("#FFF3E0"));
        }
        else {
            holder.ivFileType.setImageResource(android.R.drawable.ic_menu_agenda);
            holder.ivFileType.setColorFilter(Color.parseColor("#6E7E73"));
            holder.iconCard.setCardBackgroundColor(Color.parseColor("#F1F3F0"));
        }

        // 3. Sự kiện Click
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
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle, tvViews, tvDownloads, tvTime;
        ImageView ivFileType, btnDelete, btnEdit;
        MaterialCardView iconCard;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ đúng ID từ XML
            tvTitle = itemView.findViewById(R.id.tvDocumentTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            tvViews = itemView.findViewById(R.id.tvViews);
            tvDownloads = itemView.findViewById(R.id.tvDownloads);
            tvTime = itemView.findViewById(R.id.tvTime);

            ivFileType = itemView.findViewById(R.id.ivFileType);
            iconCard = itemView.findViewById(R.id.iconCard);

            btnDelete = itemView.findViewById(R.id.btnDeleteDoc);
            btnEdit = itemView.findViewById(R.id.btnEditDoc);
        }
    }
}