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

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {

    private List<Friend> friendList = new ArrayList<>();
    private boolean isSuggestionList; // True = list Gợi ý, False = list Lời mời
    private OnFriendActionListener listener; // Bộ lắng nghe sự kiện click

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
        this.friendList = list;
        notifyDataSetChanged(); // Vẽ lại giao diện
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
            // 1. Gán Tên và Bạn chung
            tvName.setText(friend.getUsername());
            tvMutual.setText(friend.getMutualFriends() + " bạn chung");

            // 2. Load Avatar bằng thư viện Glide
            Glide.with(itemView.getContext())
                    .load(friend.getAvatar()) // URL ảnh từ API
                    .placeholder(android.R.drawable.sym_def_app_icon) // Ảnh mặc định đang load
                    .error(android.R.drawable.sym_def_app_icon)       // Ảnh mặc định nếu URL lỗi/null
                    .centerCrop()
                    .into(imgAvatar);

            // 3. Bắt sự kiện bấm Nút Xanh (Positive)
            btnPositive.setOnClickListener(v -> {
                if (listener != null) {
                    if (isSuggestionList) {
                        listener.onAddFriendClick(friend); // Gợi ý -> Thêm bạn
                    } else {
                        listener.onAcceptClick(friend);    // Lời mời -> Chấp nhận
                    }
                }
            });

            // 4. Bắt sự kiện bấm Nút Xám (Negative)
            btnNegative.setOnClickListener(v -> {
                if (listener != null) {
                    if (isSuggestionList) {
                        listener.onRemoveSuggestClick(friend); // Gợi ý -> Gỡ
                    } else {
                        listener.onDeclineClick(friend);       // Lời mời -> Từ chối
                    }
                }
            });
        }
    }
}