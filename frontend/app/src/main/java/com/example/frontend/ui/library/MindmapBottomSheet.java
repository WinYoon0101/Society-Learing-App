package com.example.frontend.ui.library;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.frontend.R;
import com.example.frontend.data.model.MindmapData; // Bạn tự tạo Model tương ứng JSON nhé
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.card.MaterialCardView;

public class MindmapBottomSheet extends BottomSheetDialogFragment {

    private final MindmapData data;

    public MindmapBottomSheet(MindmapData data) {
        this.data = data;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_mindmap_bottom_sheet, container, false);

        TextView tvTopic = view.findViewById(R.id.tvMindmapTopic);
        TextView tvSummary = view.findViewById(R.id.tvMindmapSummary);
        LinearLayout containerNodes = view.findViewById(R.id.containerNodes);

        // Hiển thị Tiêu đề và Tóm tắt
        if (data != null) {
            tvTopic.setText(data.getTopic());
            tvSummary.setText(data.getSummary());

            // Duyệt qua mảng JSON và tạo giao diện động cho từng Ý chính (Node)
            if (data.getNodes() != null) {
                for (int i = 0; i < data.getNodes().size(); i++) {
                    MindmapData.Node node = data.getNodes().get(i);
                    View nodeView = createNodeView(node, i + 1);
                    containerNodes.addView(nodeView);
                }
            }
        }
        return view;
    }

    // Hàm tự động sinh thẻ CardView cho mỗi Ý chính
    private View createNodeView(MindmapData.Node node, int index) {
        MaterialCardView card = new MaterialCardView(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 24);
        card.setLayoutParams(params);
        card.setCardBackgroundColor(Color.parseColor("#F5F7FA"));
        card.setRadius(16f);
        card.setCardElevation(0f);

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        // Tiêu đề nhánh
        TextView tvTitle = new TextView(getContext());
        tvTitle.setText(index + ". " + node.getTitle());
        tvTitle.setTextSize(16f);
        tvTitle.setTextColor(Color.parseColor("#10B981"));
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvTitle);

        // Chi tiết nhánh
        TextView tvDetails = new TextView(getContext());
        tvDetails.setText(node.getDetails());
        tvDetails.setTextSize(14f);
        tvDetails.setTextColor(Color.parseColor("#4B5563"));
        tvDetails.setPadding(0, 12, 0, 0);
        layout.addView(tvDetails);

        card.addView(layout);
        return card;
    }
}