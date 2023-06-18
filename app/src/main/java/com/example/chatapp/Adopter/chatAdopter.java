package com.example.chatapp.Adopter;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.databinding.ItemContainerReciverBinding;
import com.example.chatapp.databinding.ItemContainerSendMessageBinding;
import com.example.chatapp.models.ChatMessage;

import java.util.List;

public class chatAdopter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    private final List<ChatMessage> chatMessageList;
    private Bitmap recieverProfileImage;
    private final String senderId;
    public static final int View_Type_Sent = 1;
    public static  final int View_Type_Recevied = 2;

public void setRecieverProfileImage(Bitmap bitmap){
    recieverProfileImage = bitmap;
}
    public chatAdopter(List<ChatMessage> chatMessageList, Bitmap recieverProfileImage, String senderId) {
        this.chatMessageList = chatMessageList;
        this.recieverProfileImage = recieverProfileImage;
        this.senderId = senderId;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == View_Type_Sent){
            return new SendMessageViewHolder(
                    ItemContainerSendMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        }else {
           return new ReceivedMessageViewHolder( ItemContainerReciverBinding.inflate(
                    LayoutInflater.from(parent.getContext()),parent,false
            ));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(getItemViewType(position) == View_Type_Sent){
            ((SendMessageViewHolder) holder).setData(chatMessageList.get(position));

        }else{

            ((ReceivedMessageViewHolder) holder).setData(chatMessageList.get(position),recieverProfileImage);
        }
    }

    @Override
    public int getItemCount() {
        return chatMessageList.size();
    }

    @Override
    public int getItemViewType(int position) {
        if(chatMessageList.get(position).senderId.equals(senderId)){
            return View_Type_Sent;
        }
        else{
            return View_Type_Recevied;
        }
    }

    static class SendMessageViewHolder extends RecyclerView.ViewHolder{
        private final ItemContainerSendMessageBinding binding;
        SendMessageViewHolder(ItemContainerSendMessageBinding itemContainerSendMessageBinding){
            super(itemContainerSendMessageBinding.getRoot());
            binding = itemContainerSendMessageBinding;
        }
        void setData(ChatMessage chatMessage){
            binding.textMessage.setText(chatMessage.message);
            binding.textDateTime.setText(chatMessage.dateTime);
        }
    }
    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder{
         private final ItemContainerReciverBinding binding;
         ReceivedMessageViewHolder(ItemContainerReciverBinding itemContainerReciverBinding){
             super(itemContainerReciverBinding.getRoot());
             binding = itemContainerReciverBinding;
         }
         void setData(ChatMessage chatMessage,Bitmap receiverProfileImage){
             binding.textMessage.setText(chatMessage.message);
             binding.textDateTime.setText(chatMessage.dateTime);
             if(receiverProfileImage !=null){
             binding.imageProfile.setImageBitmap(receiverProfileImage);
         }}
    }
}
