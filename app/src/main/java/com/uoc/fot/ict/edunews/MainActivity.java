package com.uoc.fot.ict.edunews;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView welcomeText, nameText, emailText, addressText, mobileText;
    private Button logoutButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeFirebase();
        initializeViews();
        checkUserAuthentication();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    private void initializeViews() {
        welcomeText = findViewById(R.id.welcomeText);
        nameText = findViewById(R.id.nameText);
        emailText = findViewById(R.id.emailText);
        addressText = findViewById(R.id.addressText);
        mobileText = findViewById(R.id.mobileText);
        logoutButton = findViewById(R.id.logoutButton);
        progressBar = findViewById(R.id.progressBar);

        logoutButton.setOnClickListener(v -> performLogout());
    }

    private void checkUserAuthentication() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            redirectToLogin();
        } else {
            showProgress(true);
            fetchUserData(currentUser.getUid());
        }
    }

    private void fetchUserData(String userId) {
        DocumentReference userRef = db.collection("users").document(userId);
        userRef.get().addOnCompleteListener(task -> {
            showProgress(false);

            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    updateUIWithUserData(document);
                } else {
                    showBasicUserInfo();
                    Toast.makeText(this, "Profile data not found", Toast.LENGTH_SHORT).show();
                }
            } else {
                showBasicUserInfo();
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUIWithUserData(DocumentSnapshot document) {
        String name = document.getString("name") != null ? document.getString("name") : "Not provided";
        String email = document.getString("email") != null ? document.getString("email") : "Not provided";
        String address = document.getString("address") != null ? document.getString("address") : "Not provided";
        String mobile = document.getString("mobile") != null ? document.getString("mobile") : "Not provided";

        welcomeText.setText(String.format("Welcome, %s!", name));
        nameText.setText(String.format("Name: %s", name));
        emailText.setText(String.format("Email: %s", email));
        addressText.setText(String.format("Address: %s", address));
        mobileText.setText(String.format("Mobile: %s", mobile));
    }

    private void showBasicUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            welcomeText.setText(String.format("Welcome, %s!", user.getEmail()));
            emailText.setText(String.format("Email: %s", user.getEmail()));
            nameText.setVisibility(View.GONE);
            addressText.setVisibility(View.GONE);
            mobileText.setVisibility(View.GONE);
        }
    }

    private void performLogout() {
        mAuth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        redirectToLogin();
    }

    private void redirectToLogin() {
        startActivity(new Intent(this, SignIn.class));
        finish();
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        welcomeText.setVisibility(show ? View.GONE : View.VISIBLE);
        nameText.setVisibility(show ? View.GONE : View.VISIBLE);
        emailText.setVisibility(show ? View.GONE : View.VISIBLE);
        addressText.setVisibility(show ? View.GONE : View.VISIBLE);
        mobileText.setVisibility(show ? View.GONE : View.VISIBLE);
        logoutButton.setEnabled(!show);
    }
}