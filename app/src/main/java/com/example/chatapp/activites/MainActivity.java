package com.example.chatapp.activites;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;

import android.view.View;
import android.widget.Toast;


import com.example.chatapp.Adopter.RecentConversationAdopter;
import com.example.chatapp.databinding.ActivityMainBinding;
import com.example.chatapp.listners.ConservationListner;
import com.example.chatapp.models.ChatMessage;
import com.example.chatapp.models.User;
import com.example.chatapp.utilities.PreferenceManager;
import com.example.chatapp.utilities.constants;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends UserAvailability implements ConservationListner {
    private ActivityMainBinding binding;
    private PreferenceManager preferenceManager;
    private List<ChatMessage> conservation;
    private RecentConversationAdopter recentConversationAdopter;
    private FirebaseFirestore database;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        loadUserDetails();
        getToken();
        init();
        setListners();
        ListenConservations();
    }
    private void setListners(){
        binding.imageSignout.setOnClickListener(v-> signout());
        binding.fabNewChat.setOnClickListener(v->startActivity(new Intent(getApplicationContext(),UsersActivity.class)));

    }
    private  void ListenConservations(){
        database.collection(constants.KEY_CONVERSATIONS)
                .whereEqualTo(constants.KEY_SENDER_ID,preferenceManager.getString(constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
        database.collection(constants.KEY_CONVERSATIONS)
                .whereEqualTo(constants.KEY_RECEIVER_ID,preferenceManager.getString(constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }
    final EventListener<QuerySnapshot> eventListener = ((value, error) -> {
        if(error !=null){
            return;
        } else if (value != null) {
            for(DocumentChange documentChange : value.getDocumentChanges()){
                if(documentChange.getType() == DocumentChange.Type.ADDED)
                {
                    String senderId = documentChange.getDocument().getString(constants.KEY_SENDER_ID);
                    String recieverId = documentChange.getDocument().getString(constants.KEY_RECEIVER_ID);
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = senderId;
                    chatMessage.receiverId = recieverId;
                    if(preferenceManager.getString(constants.KEY_USER_ID).equals(senderId)){
                        chatMessage.conservationImage = documentChange.getDocument().getString(constants.KEY_RECEIVER_IMAGE);
                        chatMessage.conservationName = documentChange.getDocument().getString(constants.KEY_RECEVIER_NAME);
                        chatMessage.conservationId = documentChange.getDocument().getString(constants.KEY_RECEIVER_ID);
                    }else{
                        chatMessage.conservationImage = documentChange.getDocument().getString(constants.KEY_SENDER_IMAGE);
                        chatMessage.conservationName = documentChange.getDocument().getString(constants.KEY_SENDER_NAME);
                        chatMessage.conservationId = documentChange.getDocument().getString(constants.KEY_SENDER_ID);
                    }
                    chatMessage.message = documentChange.getDocument().getString(constants.KEY_MESSAGE);
                    chatMessage.dateobject = documentChange.getDocument().getDate(constants.KEY_TIMESTAMP);
                    conservation.add(chatMessage);
                } else if (documentChange.getType() == DocumentChange.Type.MODIFIED) {
                    for (int i =0;i< conservation.size();i++){
                        String SenderId = documentChange.getDocument().getString(constants.KEY_SENDER_ID);
                        String ReceiverId = documentChange.getDocument().getString(constants.KEY_RECEIVER_ID);
                        if(conservation.get(i).senderId.equals(SenderId) && conservation.get(i).receiverId.equals(ReceiverId)){
                            conservation.get(i).message = documentChange.getDocument().getString(constants.KEY_MESSAGE);
                            conservation.get(i).dateobject = documentChange.getDocument().getDate(constants.KEY_TIMESTAMP);
                            break;
                        }
                    }
                    
                }
                Collections.sort(conservation,(obj1,obj2)->
                    obj2.dateobject.compareTo(obj1.dateobject)
                );
                recentConversationAdopter.notifyDataSetChanged();
                binding.Recentrecycler.smoothScrollToPosition(0);
                binding.Recentrecycler.setVisibility(View.VISIBLE);
                binding.progressBar.setVisibility(View.GONE);
            }
        }
    });
    private void init(){
        conservation = new ArrayList<>();
        recentConversationAdopter = new RecentConversationAdopter(conservation, this);
        binding.Recentrecycler.setAdapter(recentConversationAdopter);
        database = FirebaseFirestore.getInstance();
    }
    private void loadUserDetails(){
        binding.TextName.setText(preferenceManager.getString(constants.KEY_NAME));
        byte[] bytes = Base64.decode(preferenceManager.getString(constants.KEY_IMAGE),Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
    }
    private void showToast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
    private void getToken(){
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(v -> updateToken(v));
    }
    private void updateToken(String token){
        preferenceManager.putString(constants.KEY_FCM_TOKEN,token);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database.collection(constants.KEY_COLLECTION_USERS).document(
                preferenceManager.getString(constants.KEY_USER_ID)
        );
        documentReference.update(constants.KEY_FCM_TOKEN,token)

                .addOnFailureListener(e-> showToast("unable to update token"));

    }
    private void signout() {
       showToast("signing out");
       FirebaseFirestore database = FirebaseFirestore.getInstance();
       DocumentReference documentReference = database.collection(constants.KEY_COLLECTION_USERS)
               .document(preferenceManager.getString(constants.KEY_USER_ID));
        HashMap<String,Object> updates = new HashMap<>();
        updates.put(constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(v -> {
                    preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(), SigninActivity.class));
                }).addOnFailureListener(e -> showToast("Unable to signOut..."));
    }

    @Override
    public void onConversationClicked(User user) {
        Intent intent = new Intent(getApplicationContext(),ChatActivity.class);
        intent.putExtra(constants.KEY_USER,user);
        startActivity(intent);
    }
}