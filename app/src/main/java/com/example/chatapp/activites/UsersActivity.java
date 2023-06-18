package com.example.chatapp.activites;



import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.chatapp.Adopter.UserAdopter;
import com.example.chatapp.databinding.ActivityUsersBinding;
import com.example.chatapp.listners.UserListner;
import com.example.chatapp.models.User;
import com.example.chatapp.utilities.PreferenceManager;
import com.example.chatapp.utilities.constants;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UsersActivity extends UserAvailability implements UserListner {
    private ActivityUsersBinding binding;
    private PreferenceManager preferenceManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        getUsers();
        setListners();
    }
    private void setListners(){
        binding.imageback.setOnClickListener(v->onBackPressed());
    }
    private void getUsers(){
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(constants.KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task -> {
                            loading(false);
                            String currentUserId = preferenceManager.getString(constants.KEY_USER_ID);
                            if(task.isSuccessful() && task.getResult() != null){
                                List<User> users = new ArrayList<>();
                                for(QueryDocumentSnapshot queryDocumentSnapshot: task.getResult()){
                                    if(currentUserId.equals(queryDocumentSnapshot.getId())){
                                        continue;
                                    }else{
                                        User user = new User();
                                        user.name = queryDocumentSnapshot.getString(constants.KEY_NAME);
                                        user.email = queryDocumentSnapshot.getString(constants.KEY_EMAIL);
                                        user.image = queryDocumentSnapshot.getString(constants.KEY_IMAGE);
                                        user.token = queryDocumentSnapshot.getString(constants.KEY_FCM_TOKEN);
                                        user.id = queryDocumentSnapshot.getId();
                                        users.add(user);
                                    }
                                    if(users.size() > 0){
                                        UserAdopter userAdopter = new UserAdopter(users, this);
                                        binding.userRecyclerView.setAdapter(userAdopter);
                                        binding.userRecyclerView.setVisibility(View.VISIBLE);

                                    }else {
                                        showErrorMessage();
                                    }
                                }
                            }
                }
                );
    }
    private void showErrorMessage(){
        binding.textErrorMessage.setText(String.format("%s","No User available"));
        binding.textErrorMessage.setVisibility(View.VISIBLE);
    }
    private void loading(Boolean isloading){
        if(isloading){
            binding.progressBar.setVisibility(View.VISIBLE);
        }else{
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onUserClicked(User user) {
        Intent intent = new Intent(getApplicationContext(),ChatActivity.class);
        intent.putExtra(constants.KEY_USER,user);
        startActivity(intent);
        finish();
    }
}