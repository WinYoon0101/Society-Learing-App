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

public class SavedDocsAdapter extends RecyclerView.Adapter<SavedDocsAdapter.ViewHolder> {
    private List<Document> list = new ArrayList<>();
    private final OnSavedDocActionListener listener;

    public interface OnSavedDocActionListener {
        void onUnsaveClick(Document doc);
        void onItemClick(Document doc);
    }

    public SavedDocsAdapter(OnSavedDocActionListener listener) {
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_saved_doc, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Document doc = list.get(position);

        holder.tvTitle.setText(doc.getTitle());
        holder.tvSubtitle.setText(doc.getSubject() + " • " + (doc.getUploaderName() != null ? doc.getUploaderName() : "Hệ thống"));
        holder.tvViews.setText(String.valueOf(doc.getNumberView()));
        holder.tvDownloads.setText(String.valueOf(doc.getNumberDownload()));

        String url = (doc.getFileUrl() != null) ? doc.getFileUrl().toLowerCase() : "";


        holder.ivFileType.setImageTintList(null);
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

            holder.ivFileType.setImageResource(R.drawable.ic_generic_file);
            holder.ivFileType.setColorFilter(Color.parseColor("#6E7E73"));
            holder.iconCard.setCardBackgroundColor(Color.parseColor("#F1F3F0"));
        }

        holder.btnUnsave.setOnClickListener(v -> {
            if (listener != null) listener.onUnsaveClick(doc);
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
        TextView tvTitle, tvSubtitle, tvViews, tvDownloads;
        ImageView ivFileType, btnUnsave;
        MaterialCardView iconCard;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvDocumentTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            tvViews = itemView.findViewById(R.id.tvViews);
            tvDownloads = itemView.findViewById(R.id.tvDownloads);
            ivFileType = itemView.findViewById(R.id.ivFileType);
            iconCard = itemView.findViewById(R.id.iconCard);
            btnUnsave = itemView.findViewById(R.id.btnUnsave);
        }
    }
}