package com.example.frontend.ui.library;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.data.model.Document;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.ViewHolder> {
    private List<Document> documents = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Document doc);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setList(List<Document> newList) {
        this.documents = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_document, parent, false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Document doc = documents.get(position);

        holder.tvTitle.setText(doc.getTitle());
        holder.tvSubtitle.setText(doc.getSubject() + " • " + doc.getUploaderName());
        holder.tvViews.setText(String.valueOf(doc.getNumberView()));
        holder.tvDownloads.setText(String.valueOf(doc.getNumberDownload()));

        if (doc.getCreatedAt() != null && doc.getCreatedAt().length() > 10) {
            holder.tvTime.setText(doc.getCreatedAt().substring(0, 10));
        }

        String url = doc.getFileUrl().toLowerCase();

        // --- BƯỚC 1: RESET NỀN  ---
        holder.ivFileType.setColorFilter(null);
        holder.iconCard.setCardBackgroundColor(Color.WHITE);

        // --- BƯỚC 2: LOGIC ĐỔI ICON ---

        if (url.contains(".pdf")) {
            holder.ivFileType.setImageResource(R.drawable.ic_pdf);
        }
        else if (url.contains(".doc") || url.contains(".docx")) {
            holder.ivFileType.setImageResource(R.drawable.ic_word);

        }
        else if (url.contains(".ppt") || url.contains(".pptx")) {
            holder.ivFileType.setImageResource(android.R.drawable.ic_menu_slideshow);
            holder.ivFileType.setColorFilter(Color.parseColor("#F57C00"));
            holder.iconCard.setCardBackgroundColor(Color.parseColor("#FFF3E0"));
        }
        else {
            // Mặc định cho các loại khác (Dùng màu xám nhuộm cho icon mặc định)
            holder.ivFileType.setImageResource(R.drawable.ic_generic_file);
            holder.ivFileType.setColorFilter(Color.parseColor("#6E7E73"));
            holder.iconCard.setCardBackgroundColor(Color.parseColor("#F5F5F5"));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(doc);
        });
    }

    @Override
    public int getItemCount() {
        return documents.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle, tvViews, tvDownloads, tvTime;
        ImageView ivFileType;
        MaterialCardView iconCard; // Thêm ánh xạ cho khung icon

        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            tvViews = itemView.findViewById(R.id.tvViews);
            tvDownloads = itemView.findViewById(R.id.tvDownloads);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivFileType = itemView.findViewById(R.id.ivFileType);
            iconCard = itemView.findViewById(R.id.iconCard); // Ánh xạ vào id iconCard trong XML
        }
    }
}