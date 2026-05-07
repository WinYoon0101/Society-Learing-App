package com.example.frontend.ui.friend;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // Nhớ thêm thư viện Glide vào Gradle
import com.example.frontend.R;
import com.example.frontend.data.model.Friend;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import android.content.res.ColorStateList;
import android.graphics.Color;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {

    private List<Friend> friendList = new ArrayList<>();
    private boolean isSuggestionList; // True = list Gợi ý, False = list Lời mời
    private OnFriendActionListener listener; // Bộ lắng nghe sự kiện click

    public void updateItemStatus(String userId, boolean isPending) {
        for (int i = 0; i < friendList.size(); i++) {
            if (friendList.get(i).getId().equals(userId)) {
                friendList.get(i).setPending(isPending);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void removeItem(String userId) {
        if (friendList == null) return;

        for (int i = 0; i < friendList.size(); i++) {
            // Kiểm tra đúng ID để xóa
            if (friendList.get(i).getId().equals(userId)) {
                friendList.remove(i);
                notifyItemRemoved(i);
                // Thông báo cho các item phía sau cập nhật lại vị trí (tránh lỗi index)
                notifyItemRangeChanged(i, friendList.size());
                break;
            }
        }
    }

    // 1. TẠO INTERFACE ĐỂ TRUYỀN SỰ KIỆN CLICK VỀ FRAGMENT
    public interface OnFriendActionListener {
        void onAcceptClick(Friend friend);       // Bấm Chấp nhận lời mời
        void onDeclineClick(Friend friend);      // Bấm Xóa/Từ chối lời mời
        void onAddFriendClick(Friend friend);    // Bấm Thêm bạn bè (Gợi ý)
        void onRemoveSuggestClick(Friend friend);// Bấm Gỡ (Gợi ý)
    }

    // 2. SỬA LẠI CONSTRUCTOR: Đòi thêm cái listener
    public FriendAdapter(boolean isSuggestionList, OnFriendActionListener listener) {
        this.isSuggestionList = isSuggestionList;
        this.listener = listener;
    }

    // Hàm cập nhật dữ liệu từ ViewModel
    public void submitList(List<Friend> list) {
        // Tạo một bản sao mới của list để tránh can thiệp trực tiếp vào dữ liệu gốc của ViewModel
        this.friendList = new ArrayList<>(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isSuggestionList ? R.layout.item_friend_suggestion : R.layout.item_friend_request;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        Friend friend = friendList.get(position);
        holder.bind(friend);
    }

    @Override
    public int getItemCount() {
        return friendList != null ? friendList.size() : 0;
    }

    // ==========================================
    // CLASS QUẢN LÝ CÁC VIEW BÊN TRONG 1 Ô
    // ==========================================
    class FriendViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvMutual;
        ImageView imgAvatar;
        MaterialButton btnPositive; // Nút màu xanh (Chấp nhận / Thêm bạn)
        MaterialButton btnNegative; // Nút màu xám (Xóa / Gỡ)

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);

            // Tùy theo loại Layout mà chúng ta tìm đúng ID
            if (isSuggestionList) {
                tvName = itemView.findViewById(R.id.tvNameSuggest);
                tvMutual = itemView.findViewById(R.id.tvMutualFriendsSuggest);
                imgAvatar = itemView.findViewById(R.id.imgAvatarSuggest);
                btnPositive = itemView.findViewById(R.id.btnAddFriend);
                btnNegative = itemView.findViewById(R.id.btnRemoveSuggest);
            } else {
                tvName = itemView.findViewById(R.id.tvNameRequest);
                tvMutual = itemView.findViewById(R.id.tvMutualFriendsRequest);
                imgAvatar = itemView.findViewById(R.id.imgAvatarRequest);
                btnPositive = itemView.findViewById(R.id.btnAccept);
                btnNegative = itemView.findViewById(R.id.btnDecline);
            }
        }

        public void bind(Friend friend) {
            tvName.setText(friend.getUsername());
            tvMutual.setText(friend.getMutualFriends() + " bạn chung");

            Glide.with(itemView.getContext())
                    .load(friend.getAvatar())
                    .placeholder(android.R.drawable.sym_def_app_icon)
                    .error(android.R.drawable.sym_def_app_icon)
                    .centerCrop()
                    .into(imgAvatar);

            if (isSuggestionList) {
                // --- LOGIC CHO DANH SÁCH GỢI Ý ---
                if (friend.isPending()) {
                    btnPositive.setText("Hủy lời mời");
                    btnPositive.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E4E6DF")));
                    btnPositive.setTextColor(Color.parseColor("#1A1A1A"));
                } else {
                    btnPositive.setText("Thêm bạn bè");
                    btnPositive.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D1FAE5")));
                    btnPositive.setTextColor(Color.parseColor("#059669"));
                }

                btnPositive.setOnClickListener(v -> {
                    if (listener != null) {
                        if (friend.isPending()) listener.onDeclineClick(friend);
                        else listener.onAddFriendClick(friend);
                    }
                });

                // Nút Gỡ (CHỈ dành cho gợi ý - Đưa vào trong IF)
                btnNegative.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onRemoveSuggestClick(friend);
                    }
                });

            } else {
                // --- LOGIC CHO DANH SÁCH LỜI MỜI ---
                btnPositive.setOnClickListener(v -> {
                    if (listener != null) listener.onAcceptClick(friend);
                });

                // Nút Xóa (CHỈ dành cho lời mời - Đưa vào trong ELSE)
                btnNegative.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDeclineClick(friend);
                    }
                });
            }

        }



    }
}