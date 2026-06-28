package com.example.frontend.ui.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.Friend;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.VH> {
    private final List<Friend> items;
    public interface OnFriendClick{
        void onClick(Friend friend);
    }
    private final OnFriendClick listener;
    public FriendAdapter(List<Friend> items, OnFriendClick listener){
        this.items=items;
        this.listener=listener;
    }
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        View v= LayoutInflater.from(parent.getContext()).inflate(R.layout.item_profile_friend, parent, false);
        return new VH(v);
    }
    @Override
    public void onBindViewHolder(@NonNull VH holder, int position){
        Friend friend=items.get(position);
        holder.tvName.setText(friend.getUsername());
        Glide.with(holder.img).load(friend.getAvatar()).placeholder(R.drawable.ic_user).into(holder.img);
        holder.itemView.setOnClickListener(v->{
            if(listener!=null){
                listener.onClick(friend);
            }
        });
        holder.btnUnfriend.setVisibility(View.GONE);
    }
    @Override
    public int getItemCount(){
        return items.size();
    }
    class VH extends RecyclerView.ViewHolder{
        CircleImageView img;
        TextView tvName;
        MaterialButton btnUnfriend;
        VH(View v){
            super(v);
            img=v.findViewById(R.id.imgFriendAvatar);
            tvName=v.findViewById(R.id.tvFriendName);
            btnUnfriend=v.findViewById(R.id.btnUnfriend);
        }
    }
}
