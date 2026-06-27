package com.example.frontend.ui.feed;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.frontend.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class FeelingBottomSheet extends BottomSheetDialogFragment {

    public interface ReactionListener {
        void onReactionSelected(String reactionType);
    }

    private ReactionListener reactionListener;

    public static FeelingBottomSheet newInstance() {
        return new FeelingBottomSheet();
    }

    public void setReactionListener(ReactionListener listener) {
        this.reactionListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bottom_sheet_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int[] btnIds = {
                R.id.btnReactLike, R.id.btnReactLove, R.id.btnReactHaha,
                R.id.btnReactWow, R.id.btnReactSad, R.id.btnReactAngry,
                R.id.btnReactLucky, R.id.btnReactLoved, R.id.btnReactSick,
                R.id.btnReactQuestion, R.id.btnReactCool, R.id.btnReactSmart
        };
        String[] types = {
                "Like", "Love", "Haha",
                "Wow", "Sad", "Angry",
                "Lucky", "Loved", "Sick",
                "Question", "Cool", "Smart"
        };

        for (int i = 0; i < btnIds.length; i++) {
            String type = types[i];
            view.findViewById(btnIds[i]).setOnClickListener(v -> {
                if (reactionListener != null) reactionListener.onReactionSelected(type);
                dismiss();
            });
        }
    }
}