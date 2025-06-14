package com.uoc.fot.ict.edunews;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

    public class news extends AppCompatActivity {

        private static final String TAG = "NewsArticleActivity"; // Changed TAG for clarity

        // UI Elements
        private TextView postTitleTextView; // Top bar title
        private ShapeableImageView profileIconImageView; // Top bar profile icon
        private ImageButton backButton; // Top bar back button

        private TextView newsArticleTitleTextView;
        private TextView newsArticleDateTimeTextView;
        private TextView newsArticleAuthorTextView;
        private ImageView newsArticleImageView;
        private TextView newsArticleDescriptionTextView;
        private ProgressBar progressBar;

        // Firebase Instances
        private FirebaseAuth mAuth;
        private FirebaseFirestore db;
        private FirebaseUser currentUser;
        // private FirebaseStorage storage; // Not strictly needed for just fetching URLs

        // Data for the current news article
        private String newsArticleId; // The ID of the news document to fetch

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_news);

            // Initialize Firebase
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
            currentUser = mAuth.getCurrentUser();
            // storage = FirebaseStorage.getInstance(); // Only if you need to manipulate storage

            // Initialize UI elements
            postTitleTextView = findViewById(R.id.postTitle); // Top bar title
            profileIconImageView = findViewById(R.id.profileIcon); // Top bar profile icon
            backButton = findViewById(R.id.backButton); // Top bar back button

            newsArticleTitleTextView = findViewById(R.id.newsArticleTitle);
            newsArticleDateTimeTextView = findViewById(R.id.newsArticleDateTime);
            newsArticleAuthorTextView = findViewById(R.id.newsArticleAuthor);
            newsArticleImageView = findViewById(R.id.newsArticleImage);
            newsArticleDescriptionTextView = findViewById(R.id.newsArticleDescription);
            progressBar = findViewById(R.id.progressBar);

            // Set up listeners for the top bar
            backButton.setOnClickListener(v -> onBackPressed());
            profileIconImageView.setOnClickListener(v -> {
                // Navigate to UserInfo activity
                Intent intent = new Intent(this, UserInfo.class);
                startActivity(intent);
            });

            // Get news article ID from intent
            // Assuming the ID is passed from a previous activity (e.g., from RecyclerView item click)
            Intent intent = getIntent();
            if (intent != null && intent.hasExtra("NEWS_ARTICLE_ID")) {
                newsArticleId = intent.getStringExtra("NEWS_ARTICLE_ID");
                fetchNewsArticle(newsArticleId);
            } else {
                Toast.makeText(this, "No news article ID provided.", Toast.LENGTH_SHORT).show();
                finish(); // Close activity if no ID
            }

            // Load current user's profile picture for the top bar icon
            loadCurrentUserProfilePicture();
        }

        /**
         * Fetches the news article data from Firestore.
         * @param articleId The document ID of the news article in Firestore.
         */
        private void fetchNewsArticle(String articleId) {
            progressBar.setVisibility(View.VISIBLE);
            DocumentReference docRef = db.collection("posts").document(articleId); // Assuming collection name is "posts"

            docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    progressBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        // Retrieve data from DocumentSnapshot
                        String title = documentSnapshot.getString("title"); // Use "title" for article title
                        String description = documentSnapshot.getString("description");
                        String imageUrl = documentSnapshot.getString("imageUrl");
                        String postDate = documentSnapshot.getString("postDate");
                        String author = documentSnapshot.getString("author");

                        // Populate UI
                        newsArticleTitleTextView.setText(title);
                        newsArticleDescriptionTextView.setText(description);
                        newsArticleAuthorTextView.setText("By: " + author); //

                        // Format date and time
                        if (postDate != null && !postDate.isEmpty()) {
                            try {
                                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                                Date date = inputFormat.parse(postDate);
                                SimpleDateFormat outputTimeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault()); // e.g., 12:44 PM
                                SimpleDateFormat outputDateFormat = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()); // e.g., 2025.06.14
                                String formattedTime = outputTimeFormat.format(date);
                                String formattedDate = outputDateFormat.format(date);
                                newsArticleDateTimeTextView.setText(formattedTime + " | " + formattedDate); //
                            } catch (ParseException e) {
                                Log.e(TAG, "Error parsing date: " + postDate, e);
                                newsArticleDateTimeTextView.setText(postDate); // Fallback to raw date
                            }
                        } else {
                            newsArticleDateTimeTextView.setText("");
                        }

                        // Load image using Glide
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(news.this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.circular_background_grey) // Placeholder image
                                    .error(R.drawable.cross) // Error image
                                    .into(newsArticleImageView);
                        } else {
                            newsArticleImageView.setImageResource(R.drawable.facebook); // Default image
                        }

                        // Update top bar title (optional, based on image you sent for "World Art Day")
                        postTitleTextView.setText(title); // This will set the top bar title to the news article's title

                    } else {
                        Toast.makeText(news.this, "News article not found.", Toast.LENGTH_SHORT).show();
                        finish(); // Close activity if document doesn't exist
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(news.this, "Error loading news article: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error fetching document: " + e.getMessage());
                    finish(); // Close activity on error
                }
            });
        }

        /**
         * Loads the current user's profile picture into the top bar icon.
         */
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
                                            .placeholder(R.drawable.user) // Default user icon
                                            .error(R.drawable.user)
                                            .into(profileIconImageView);
                                } else {
                                    profileIconImageView.setImageResource(R.drawable.user); // Default icon
                                }
                            } else {
                                profileIconImageView.setImageResource(R.drawable.user); // Default icon if user doc not found
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to load profile picture for top bar: " + e.getMessage());
                            profileIconImageView.setImageResource(R.drawable.user); // Fallback to default icon on error
                        });
            } else {
                profileIconImageView.setImageResource(R.drawable.user); // Default icon if no user logged in
            }
        }

        @Override
        public void onBackPressed() {
            super.onBackPressed();
            // You might want to navigate back to the home screen or categories
            Intent intent = new Intent(news.this, home.class); // Or CategoryNews.class if coming from there
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }