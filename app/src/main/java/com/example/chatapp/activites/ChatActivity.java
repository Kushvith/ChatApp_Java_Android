package com.example.chatapp.activites;



import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import com.example.chatapp.Adopter.chatAdopter;
import com.example.chatapp.databinding.ActivityChatBinding;
import com.example.chatapp.models.ChatMessage;
import com.example.chatapp.models.User;
import com.example.chatapp.networks.ApiService;
import com.example.chatapp.networks.Apiclient;
import com.example.chatapp.utilities.PreferenceManager;
import com.example.chatapp.utilities.constants;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import com.google.firebase.firestore.QuerySnapshot;

import android.provider.SyncStateContract;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends UserAvailability {
    private ActivityChatBinding binding;
    private User receiverUser;
    private List<ChatMessage> chatMessageList;
    private chatAdopter chatAdopter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private String conservationId = null;
    private Boolean isReceiverAvailability = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListners();
        loadReciverDetails();
        init();
        listMessages();
    }
    private void init(){
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessageList = new ArrayList<>();
        chatAdopter = new chatAdopter(
                chatMessageList,
                getBitmapFromEncodedString(receiverUser.image),
                preferenceManager.getString(constants.KEY_USER_ID)
        );
        binding.chatRecycler.setAdapter(chatAdopter);
        database = FirebaseFirestore.getInstance();

    }
    private void sendMessage(){
        HashMap<String,Object> message = new HashMap<>();
        message.put(constants.KEY_SENDER_ID,preferenceManager.getString(constants.KEY_USER_ID));
        message.put(constants.KEY_RECEIVER_ID,receiverUser.id);
        message.put(constants.KEY_MESSAGE,binding.inputMessage.getText().toString());
        message.put(constants.KEY_TIMESTAMP,new Date());
        database.collection(constants.KEY_COLLECTION_CHATS).add(message);
        if(conservationId !=null){
            updateConservation(binding.inputMessage.getText().toString());
        }else{
            HashMap<String,Object> conversation = new HashMap<>();
            conversation.put(constants.KEY_SENDER_ID,preferenceManager.getString(constants.KEY_USER_ID));
            conversation.put(constants.KEY_SENDER_NAME,preferenceManager.getString(constants.KEY_NAME));
            conversation.put(constants.KEY_RECEIVER_ID,receiverUser.id);
            conversation.put(constants.KEY_RECEVIER_NAME,receiverUser.name);
            conversation.put(constants.KEY_RECEIVER_IMAGE,receiverUser.image);
            conversation.put(constants.KEY_SENDER_IMAGE,preferenceManager.getString(constants.KEY_IMAGE));
            conversation.put(constants.KEY_TIMESTAMP,new Date());
            conversation.put(constants.KEY_MESSAGE,binding.inputMessage.getText().toString());
            addConservation(conversation);
        }
        if(!isReceiverAvailability){
            try{
                JSONArray tokens = new JSONArray();
                tokens.put(receiverUser.token);
                JSONObject data = new JSONObject();
                data.put(constants.KEY_USER_ID,preferenceManager.getString(constants.KEY_USER_ID));
                data.put(constants.KEY_NAME,preferenceManager.getString(constants.KEY_NAME));
                data.put(constants.KEY_FCM_TOKEN,preferenceManager.getString(constants.KEY_FCM_TOKEN));
                data.put(constants.KEY_MESSAGE,binding.inputMessage.getText().toString());

                JSONObject body = new JSONObject();
                body.put(constants.REMOTE_MSG_DATA,data);
                body.put(constants.REMOTE_MSG_REGISTRATION_IDS,tokens);
                sendNotification(body.toString());

            }catch (Exception e){
                showToast(e.getMessage());
            }
        }
        binding.inputMessage.setText(null);
    }
    private void sendNotification(String messageBody){
        Apiclient.getclient().create(ApiService.class).sendMessage(
                constants.getRemoteMsgHeaders(),
                messageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call,@NonNull Response<String> response) {
                if(response.isSuccessful()){
                    try {
                        if(response.body() != null){
                            JSONObject responseJson = new JSONObject(response.body());
                            JSONArray results = responseJson.getJSONArray("results");
                            if(responseJson.getInt("failure")==1){
                                JSONObject error = (JSONObject) results.get(0);
                                showToast(error.getString("error"));
                                return;
                            }
                        }
                    }catch (JSONException e){
                        e.printStackTrace();
                    }
                    showToast("message sent successfully");
                }else {
                    showToast("Error "+response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call,@NonNull Throwable t) {
                showToast(
                        t.getMessage()
                );
            }
        });
    }
    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    private void listenAvailabilityofReciever(){
        database.collection(constants.KEY_COLLECTION_USERS)
                .document(receiverUser.id).addSnapshotListener(ChatActivity.this,((value, error) ->
                {
                    if(error != null){
                        return;
                    }if(value != null) {
                    if (value.getLong(constants.KEY_AVAILABILITY) != null) {
                        int availability = Objects.requireNonNull(
                                value.getLong(constants.KEY_AVAILABILITY)
                        ).intValue();
                        isReceiverAvailability = availability == 1;
                    }
                    receiverUser.token = value.getString(constants.KEY_FCM_TOKEN);
                    if(receiverUser.image == null){
                        receiverUser.image = value.getString(constants.KEY_IMAGE);
                        chatAdopter.setRecieverProfileImage(getBitmapFromEncodedString(receiverUser.image));
                        chatAdopter.notifyItemRangeChanged(0,chatMessageList.size());
                    }
                }
                    if(isReceiverAvailability){
                        binding.TextAvailable.setVisibility(View.VISIBLE);

                    }else {
                        binding.TextAvailable.setVisibility(View.GONE);
                    }

                }));
    }
    private void listMessages(){
        database.collection(constants.KEY_COLLECTION_CHATS)
                .whereEqualTo(constants.KEY_SENDER_ID,preferenceManager.getString(constants.KEY_USER_ID))
                .whereEqualTo(constants.KEY_RECEIVER_ID,receiverUser.id)
                .addSnapshotListener(eventListener);
        database.collection(constants.KEY_COLLECTION_CHATS)
                .whereEqualTo(constants.KEY_SENDER_ID,receiverUser.id)
                .whereEqualTo(constants.KEY_RECEIVER_ID,preferenceManager.getString(constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }
    private final EventListener<QuerySnapshot> eventListener =(value,error)->{
        if(error!=null){
            return;
        }if(value !=null){
            int count = chatMessageList.size();
            for(DocumentChange documentChange : value.getDocumentChanges()){
                if(documentChange.getType() == DocumentChange.Type.ADDED){
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(constants.KEY_MESSAGE);
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(constants.KEY_TIMESTAMP));
                    chatMessage.dateobject = documentChange.getDocument().getDate(constants.KEY_TIMESTAMP);
                    chatMessageList.add(chatMessage);
                }
            }
            Collections.sort(chatMessageList,(obj1,obj2)->obj1.dateobject.compareTo(obj2.dateobject));
            if(count == 0){
                chatAdopter.notifyDataSetChanged();
            }else{
                chatAdopter.notifyItemRangeInserted(chatMessageList.size(),chatMessageList.size());
                binding.chatRecycler.smoothScrollToPosition(chatMessageList.size() -1);
            }
            binding.chatRecycler.setVisibility(View.VISIBLE);

        }
        binding.progressBar.setVisibility(View.GONE);
        if(conservationId == null){
            checkForConservation();
        }
    };
    private Bitmap getBitmapFromEncodedString(String encodedString){
        if(encodedString != null){
            byte[] bytes = Base64.decode(encodedString,Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        }else{
            return  null;
        }

    }
    private void loadReciverDetails(){
        receiverUser = (User) getIntent().getSerializableExtra(constants.KEY_USER);
        binding.textName.setText(receiverUser.name);

    }
    private void setListners(){
        binding.imageback.setOnClickListener(v->onBackPressed());
        binding.layoutSend.setOnClickListener(v->sendMessage());
    }
    private String getReadableDateTime(Date date){
        return new SimpleDateFormat("MMMM dd, YYYY - hh:mm a", Locale.getDefault()).format(date);
    }
    private void addConservation(HashMap<String,Object> conservation){
        database.collection(constants.KEY_CONVERSATIONS)
                .add(conservation)
                .addOnSuccessListener(documentReference -> {
                    conservationId = documentReference.getId();
                });
    }
    private void updateConservation(String message){
        DocumentReference documentReference = database.collection(constants.KEY_CONVERSATIONS).document(conservationId);
        documentReference.update(constants.KEY_MESSAGE,message,
                constants.KEY_TIMESTAMP,new Date());
    }
    private void checkForConservation(){
        if(chatMessageList.size() !=0){
            checkForConservationRecently(
                    preferenceManager.getString(constants.KEY_USER_ID),
                    receiverUser.id
            );
            checkForConservationRecently(
                    receiverUser.id,
                    preferenceManager.getString(constants.KEY_USER_ID)
            );
        }
    }
    private void checkForConservationRecently(String senderId,String receiverId){
        database.collection(constants.KEY_CONVERSATIONS)
                .whereEqualTo(constants.KEY_SENDER_ID,senderId)
                .whereEqualTo(constants.KEY_RECEIVER_ID,receiverId)
                .get()
                .addOnCompleteListener(conversationOnCompleteListner);
    }
    private final OnCompleteListener<QuerySnapshot> conversationOnCompleteListner = task ->{
        if(task.isSuccessful() && task.getResult()!=null&&task.getResult().getDocuments().size()>0){
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conservationId = documentSnapshot.getId();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        listenAvailabilityofReciever();
    }
}