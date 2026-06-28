package com.example.frontend.ui.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.frontend.R;
import com.example.frontend.data.model.Media;

import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.VH>{
    private final List<Media> photos;
    public PhotoAdapter(List<Media> photos){
        this.photos=photos;
    }
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        View v= LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo_grid, parent, false);
        return new VH(v);
    }
    @Override
    public void onBindViewHolder(@NonNull VH holder, int position){
        Glide.with(holder.img).load(photos.get(position).getUrl()).centerCrop().placeholder(R.drawable.bg_cover_default).into(holder.img);
    }
    @Override
    public int getItemCount(){
        return photos.size();
    }
    class VH extends RecyclerView.ViewHolder{
        ImageView img;
        VH(View v){
            super(v);
            img=v.findViewById(R.id.imgPhoto);
        }
    }
}
