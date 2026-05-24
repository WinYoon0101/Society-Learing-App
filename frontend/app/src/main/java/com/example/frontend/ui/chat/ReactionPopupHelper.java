package com.example.frontend.ui.chat;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.example.frontend.R;
import com.example.frontend.data.model.Message;
import com.example.frontend.data.model.Reaction;

import java.util.List;

public final class ReactionPopupHelper {

    public interface OnEmojiSelectedListener {
        void onSelected(String emoji);
    }

    private static final int[] EMOJI_IDS = {
            R.id.emojiHeart, R.id.emojiLaugh, R.id.emojiWow,
            R.id.emojiSad,   R.id.emojiAngry, R.id.emojiLike
    };
    private static final String[] EMOJIS = {
            "❤️", "😆", "😮", "😢", "😡", "👍"
    };

    private ReactionPopupHelper() {}

    public static void show(Context context, View anchor,
                            Message message, String currentUserId,
                            OnEmojiSelectedListener listener) {
        View content = LayoutInflater.from(context)
                .inflate(R.layout.popup_reaction_picker, null);

        PopupWindow popup = new PopupWindow(
                content,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                true);
        popup.setBackgroundDrawable(new ColorDrawable(0));
        popup.setOutsideTouchable(true);
        popup.setElevation(dp(context, 8));

        String myEmoji = findMyEmoji(message, currentUserId);

        for (int i = 0; i < EMOJI_IDS.length; i++) {
            TextView tv = content.findViewById(EMOJI_IDS[i]);
            String emoji = EMOJIS[i];
            if (emoji.equals(myEmoji)) {
                tv.setBackgroundResource(R.drawable.bg_reaction_chip_mine);
            }
            tv.setOnClickListener(v -> {
                if (listener != null) listener.onSelected(emoji);
                popup.dismiss();
            });
        }

        content.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int popupW = content.getMeasuredWidth();
        int popupH = content.getMeasuredHeight();

        int[] anchorLoc = new int[2];
        anchor.getLocationOnScreen(anchorLoc);
        int anchorW = anchor.getWidth();

        Rect frame = new Rect();
        anchor.getWindowVisibleDisplayFrame(frame);

        int x = anchorLoc[0] + (anchorW - popupW) / 2;
        int y = anchorLoc[1] - popupH - dp(context, 4);

        if (x < frame.left + dp(context, 8)) x = frame.left + dp(context, 8);
        if (x + popupW > frame.right - dp(context, 8)) x = frame.right - popupW - dp(context, 8);
        if (y < frame.top + dp(context, 8)) {
            y = anchorLoc[1] + anchor.getHeight() + dp(context, 4);
        }

        popup.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y);
    }

    private static String findMyEmoji(Message message, String currentUserId) {
        if (message == null || currentUserId == null) return null;
        List<Reaction> reactions = message.getReactions();
        if (reactions == null) return null;
        String myId = currentUserId.trim();
        for (Reaction r : reactions) {
            if (r.getUserId() != null && myId.equals(r.getUserId().trim())) {
                return r.getEmoji();
            }
        }
        return null;
    }

    private static int dp(Context context, int dp) {
        return Math.round(context.getResources().getDisplayMetrics().density * dp);
    }
}
