package com.example.chatapp.Adopter;

import android.graphics.Bitmap;

import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.databinding.ItemContainerRecentConservationBinding;
import com.example.chatapp.listners.ConservationListner;
import com.example.chatapp.models.ChatMessage;
import com.example.chatapp.models.User;

import java.util.List;

public class RecentConversationAdopter extends  RecyclerView.Adapter<RecentConversationAdopter.conservationViewHolder>{
    private final List<ChatMessage> chatMessageList;
    private  final ConservationListner conservationListner;
    public RecentConversationAdopter(List<ChatMessage> chatMessageList, ConservationListner conservationListner) {
        this.chatMessageList = chatMessageList;
        this.conservationListner = conservationListner;
    }

    @NonNull
    @Override
    public conservationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return  new conservationViewHolder(
          ItemContainerRecentConservationBinding.inflate(
                  LayoutInflater.from(parent.getContext()),
                  parent,
                  false
          )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull conservationViewHolder holder, int position) {
        holder.setData(chatMessageList.get(position));
    }

    @Override
    public int getItemCount() {
        return chatMessageList.size();
    }

    class conservationViewHolder extends RecyclerView.ViewHolder{
        ItemContainerRecentConservationBinding binding;

        public conservationViewHolder(ItemContainerRecentConservationBinding itemContainerRecentConservationBinding) {
            super(itemContainerRecentConservationBinding.getRoot());
            binding = itemContainerRecentConservationBinding;
        }
        void  setData(ChatMessage chatMessage){
            binding.textName.setText(chatMessage.conservationName);
            binding.RecentMessage.setText(chatMessage.message);
            binding.getRoot().setOnClickListener(v->{
                User user = new User();
                user.id = chatMessage.conservationId;
                user.name = chatMessage.conservationName;
                user.image = chatMessage.conservationImage;
                conservationListner.onConversationClicked(user);
            });
            binding.imageProfile.setImageBitmap(getConservationImage(chatMessage.conservationImage));
        }
    }
    private Bitmap getConservationImage(String encodedImage){
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
    }
}
