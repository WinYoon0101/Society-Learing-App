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
    private OnDownloadClickListener downloadListener;
    private OnAiMindmapClickListener aiListener;

    public interface OnAiMindmapClickListener {
        void onAiClick(Document doc);
    }

    public interface OnItemClickListener {
        void onItemClick(Document doc);
    }

    public interface OnDownloadClickListener {
        void onDownloadClick(Document doc);
    }

    public void setOnAiMindmapClickListener(OnAiMindmapClickListener listener) {
        this.aiListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnDownloadClickListener(OnDownloadClickListener listener) {
        this.downloadListener = listener;
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
        holder.tvViews.setText(String.valueOf(doc.getNumberView()));
        holder.tvDownloads.setText(String.valueOf(doc.getNumberDownload()));

        // --- BƯỚC 1: LOGIC GỘP SUBTITLE VÀ FORMAT NGÀY THÁNG ---
        String subtitle = doc.getSubject() + " • " + doc.getUploaderName();

        if (doc.getCreatedAt() != null && doc.getCreatedAt().length() >= 10) {
            String rawDate = doc.getCreatedAt().substring(0, 10); // Lấy "YYYY-MM-DD"
            String formattedDate = formatDate(rawDate); // Chuyển thành "DD/MM/YYYY"
            subtitle += " • " + formattedDate;
        }

        holder.tvSubtitle.setText(subtitle);

        // --- BƯỚC 2: LOGIC ĐỔI ICON THEO ĐỊNH DẠNG FILE ---
        String url = doc.getFileUrl() != null ? doc.getFileUrl().toLowerCase() : "";

        // Reset nền trước khi kiểm tra
        holder.ivFileType.setColorFilter(null);
        holder.iconCard.setCardBackgroundColor(Color.WHITE);

        if (url.contains(".pdf")) {
            holder.ivFileType.setImageResource(R.drawable.ic_pdf);
        } else if (url.contains(".doc") || url.contains(".docx")) {
            holder.ivFileType.setImageResource(R.drawable.ic_word);
        } else if (url.contains(".ppt") || url.contains(".pptx")) {
            holder.ivFileType.setImageResource(android.R.drawable.ic_menu_slideshow);
            holder.ivFileType.setColorFilter(Color.parseColor("#F57C00"));
            holder.iconCard.setCardBackgroundColor(Color.parseColor("#FFF3E0"));
        } else {
            // Mặc định cho các loại khác
            holder.ivFileType.setImageResource(R.drawable.ic_generic_file);
            holder.ivFileType.setColorFilter(Color.parseColor("#6E7E73"));
            holder.iconCard.setCardBackgroundColor(Color.parseColor("#F5F5F5"));
        }

        // --- BƯỚC 3: XỬ LÝ SỰ KIỆN CLICK ---
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(doc);
        });

        holder.btnDownload.setOnClickListener(v -> {
            if (downloadListener != null) {
                downloadListener.onDownloadClick(doc);
            }
        });

        holder.btnAiMindmap.setOnClickListener(v -> {
            if (aiListener != null) {
                aiListener.onAiClick(doc);
            }
        });
    }

    @Override
    public int getItemCount() {
        return documents.size();
    }

    // --- HÀM HELPER: CHUYỂN ĐỔI "YYYY-MM-DD" THÀNH "DD/MM/YYYY" ---
    private String formatDate(String dbDate) {
        try {
            String[] parts = dbDate.split("-");
            if (parts.length == 3) {
                return parts[2] + "/" + parts[1] + "/" + parts[0];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dbDate;
    }


    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle, tvViews, tvDownloads;
        ImageView ivFileType, btnDownload, btnAiMindmap;
        MaterialCardView iconCard;

        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            tvViews = itemView.findViewById(R.id.tvViews);
            tvDownloads = itemView.findViewById(R.id.tvDownloads);

            ivFileType = itemView.findViewById(R.id.ivFileType);
            iconCard = itemView.findViewById(R.id.iconCard);
            btnDownload = itemView.findViewById(R.id.btnDownload);
            btnAiMindmap = itemView.findViewById(R.id.btnAiMindmap);
        }
    }
}