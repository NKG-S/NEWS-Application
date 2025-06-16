package com.uoc.fot.ict.edunews;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class DevInfo extends AppCompatActivity {

    private static final String TAG = "DevInfoActivity";

    private ShapeableImageView profileIconImageView;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dev_info);

        // Initialize Firebase
        // Firebase
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize UI components
        ImageButton backButton = findViewById(R.id.backButton);
        TextView postTitleTextView = findViewById(R.id.postTitle);
        profileIconImageView = findViewById(R.id.profileIcon);
        Button exitButton = findViewById(R.id.Exit);

        // Set the post title
        postTitleTextView.setText("Developer information"); //

        // Load current user profile picture
        loadCurrentUserProfilePicture();

        // Set up back button click listener
        backButton.setOnClickListener(v -> handleBackNavigation());

        // Set up Exit button click listener
        exitButton.setOnClickListener(v -> handleExitApplication());

        // Handle system back button press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });
    }

    private void handleBackNavigation() {
        // Navigate to HomeActivity and clear back stack
        // Assuming 'home.class' is your main HomeActivity.
        // If it's `HomeActivity.class`, please change accordingly based on your project structure.
        Intent intent = new Intent(this, home.class); // Assuming 'home' is your HomeActivity class
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void handleExitApplication() {
        finishAffinity();
    }

    private void loadCurrentUserProfilePicture() {
        if (currentUser != null) {
            String userId = currentUser.getUid();
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String profilePictureUrl = documentSnapshot.getString("profilePictureUrl");
                            if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(profilePictureUrl)
                                        .placeholder(R.drawable.user)
                                        .error(R.drawable.user)
                                        .into(profileIconImageView);
                            } else {
                                profileIconImageView.setImageResource(R.drawable.user);
                            }
                        } else {
                            profileIconImageView.setImageResource(R.drawable.user);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to load profile picture for top bar: " + e.getMessage());
                        profileIconImageView.setImageResource(R.drawable.user);
                    });
        } else {
            profileIconImageView.setImageResource(R.drawable.user);
            Log.d(TAG, "No user logged in, showing default profile icon.");
        }
    }
}