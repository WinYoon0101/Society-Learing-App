package com.example.frontend.ui.feed;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.frontend.R;

import java.util.List;

public final class ReactionUiHelper {

    private ReactionUiHelper() {}

    public static int getReactionIconRes(String type) {
        if (type == null) return R.drawable.ic_like_color;
        switch (type) {
            case "Love": return R.drawable.ic_love;
            case "Haha": return R.drawable.ic_haha;
            case "Wow": return R.drawable.ic_wow;
            case "Sad": return R.drawable.ic_sad;
            case "Angry": return R.drawable.ic_angry;
            case "Like":
            default:
                return R.drawable.ic_like_color;
        }
    }

    public static void bindReactionButton(ImageView icon, TextView label, String reactionType) {
        if (icon != null) {
            icon.setImageResource(reactionType == null ? R.drawable.ic_like : getReactionIconRes(reactionType));
        }
        if (label != null) {
            label.setText(reactionType != null ? reactionType : "Thích");
        }
    }

    public static void bindTopReactions(
            View layoutTopReactions,
            ImageView imgReact1,
            ImageView imgReact2,
            TextView tvReactionCount,
            int reactionCount,
            List<String> topReactions
    ) {
        if (layoutTopReactions == null) return;

        if (reactionCount <= 0) {
            layoutTopReactions.setVisibility(View.GONE);
            return;
        }

        layoutTopReactions.setVisibility(View.VISIBLE);
        if (tvReactionCount != null) {
            tvReactionCount.setText(String.valueOf(reactionCount));
        }

        if (imgReact1 != null) imgReact1.setVisibility(View.GONE);
        if (imgReact2 != null) imgReact2.setVisibility(View.GONE);

        if (topReactions == null || topReactions.isEmpty()) return;

        if (imgReact1 != null) {
            imgReact1.setVisibility(View.VISIBLE);
            imgReact1.setImageResource(getReactionIconRes(topReactions.get(0)));
        }

        if (topReactions.size() > 1 && imgReact2 != null) {
            imgReact2.setVisibility(View.VISIBLE);
            imgReact2.setImageResource(getReactionIconRes(topReactions.get(1)));
        }
    }
}
