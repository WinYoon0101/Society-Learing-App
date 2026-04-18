package com.example.frontend.ui.library;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend.R;
import com.example.frontend.data.model.Document;

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


        String subtitle = doc.getSubject() + " • " + doc.getUploaderName();
        holder.tvSubtitle.setText(subtitle);

        holder.tvViews.setText(String.valueOf(doc.getNumberView()));
        holder.tvDownloads.setText(String.valueOf(doc.getNumberDownload()));

        // Logic đổi icon theo định dạng file (Ví dụ)
        String type = doc.getFileType();
        if (type.contains("video")) {
            holder.ivFileType.setImageResource(android.R.drawable.ic_media_play);
        } else {
            holder.ivFileType.setImageResource(android.R.drawable.ic_menu_agenda);
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

        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            tvViews = itemView.findViewById(R.id.tvViews);
            tvDownloads = itemView.findViewById(R.id.tvDownloads);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivFileType = itemView.findViewById(R.id.ivFileType);
        }
    }
}