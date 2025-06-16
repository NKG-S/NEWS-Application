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
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class news extends AppCompatActivity {

    private static final String TAG = "NewsArticleActivity"; // Changed TAG for clarity

    // UI Elements for Top Bar
    private TextView postTitleTextView; // Top bar title, will show article title
    private ShapeableImageView profileIconImageView; // Top bar profile icon (assuming it exists in XML)

    // UI Elements for News Article Content
    private TextView newsArticleTitleTextView;
    private TextView newsArticleDateTimeTextView;
    private TextView editedTextView; // New: TextView for "Edited" status
    private TextView newsArticleAuthorTextView;
    private ImageView newsArticleImageView; // The image view for the article
    private TextView newsArticleDescriptionTextView;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    // private FirebaseStorage storage; // Not strictly needed for just fetching URLs

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news); // Set the layout for this activity

        // Initialize Firebase instances
        // Firebase Instances
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();
        // storage = FirebaseStorage.getInstance(); // Only if you need to manipulate storage

        // Initialize UI elements by finding them from the layout
        // Top bar elements
        postTitleTextView = findViewById(R.id.postTitle); // Top bar title
        // Ensure profileIconImageView exists in your layout, adding null check for safety
        profileIconImageView = findViewById(R.id.profileIcon); // Top bar profile icon
        // Top bar back button
        ImageButton backButton = findViewById(R.id.backButton); // Top bar back button

        // News article content elements
        newsArticleTitleTextView = findViewById(R.id.newsArticleTitle);
        newsArticleDateTimeTextView = findViewById(R.id.newsArticleDateTime);
        editedTextView = findViewById(R.id.edited); // Initialize the new TextView
        newsArticleAuthorTextView = findViewById(R.id.newsArticleAuthor);
        newsArticleImageView = findViewById(R.id.newsArticleImage);
        newsArticleDescriptionTextView = findViewById(R.id.newsArticleDescription);
        progressBar = findViewById(R.id.progressBar);

        // Set up click listeners for the top bar buttons
        backButton.setOnClickListener(v -> onBackPressed()); // Go back when back button is clicked
        // Only set click listener if profileIconImageView is found in the layout
        if (profileIconImageView != null) {
            profileIconImageView.setOnClickListener(v -> {
                // Navigate to UserInfo activity when profile icon is clicked
                Intent intent = new Intent(this, UserInfo.class);
                startActivity(intent);
            });
        }


        // Get news article ID from the intent that started this activity
        // This ID is crucial for fetching the correct news article data from Firestore
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("NEWS_ARTICLE_ID")) {
            // Data for the current news article
            // The ID of the news document to fetch
            String newsArticleId = intent.getStringExtra("NEWS_ARTICLE_ID");
            fetchNewsArticle(newsArticleId); // Fetch the news article data
        } else {
            // If no article ID is provided, show a toast and close the activity
            Toast.makeText(this, "No news article ID provided.", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Load the current user's profile picture for the top bar icon
        loadCurrentUserProfilePicture();
    }

    /**
     * Fetches the news article data from Firestore using the provided article ID.
     * Populates the UI elements with the fetched data, including handling for the "Edited" status.
     *
     * @param articleId The document ID of the news article in Firestore's "posts" collection.
     */
    private void fetchNewsArticle(String articleId) {
        progressBar.setVisibility(View.VISIBLE); // Show progress bar while fetching
        DocumentReference docRef = db.collection("posts").document(articleId); // Reference to the document

        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                progressBar.setVisibility(View.GONE); // Hide progress bar on success
                if (documentSnapshot.exists()) {
                    // Retrieve data from DocumentSnapshot. Ensure field names match Firestore.
                    String title = documentSnapshot.getString("title");
                    String description = documentSnapshot.getString("description");
                    String imageUrl = documentSnapshot.getString("imageUrl");
                    String postDate = documentSnapshot.getString("postDate");
                    String author = documentSnapshot.getString("author");
                    Boolean edited = documentSnapshot.getBoolean("edited"); // Retrieve 'edited' boolean
                    String editDate = documentSnapshot.getString("editDate"); // Retrieve 'editDate' string

                    // Populate UI elements with fetched data
                    newsArticleTitleTextView.setText(title);
                    newsArticleDescriptionTextView.setText(description);

                    // Display author, handling potential null or empty author
                    if (author != null && !author.isEmpty()) {
                        newsArticleAuthorTextView.setText("By: " + author);
                    } else {
                        newsArticleAuthorTextView.setText("By: Unknown Author"); // Default if author is missing
                    }

                    // Format and display date and time
                    if (postDate != null && !postDate.isEmpty()) {
                        try {
                            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                            Date date = inputFormat.parse(postDate);
                            SimpleDateFormat outputTimeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                            SimpleDateFormat outputDateFormat = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());

                            String formattedTime = outputTimeFormat.format(date);
                            String formattedDate = outputDateFormat.format(date);
                            newsArticleDateTimeTextView.setText(formattedTime + " | " + formattedDate);
                        } catch (ParseException e) {
                            Log.e(TAG, "Error parsing post date: " + postDate, e);
                            newsArticleDateTimeTextView.setText(postDate); // Fallback to raw date string on parse error
                        }
                    } else {
                        newsArticleDateTimeTextView.setText(""); // Clear if no date
                    }

                    // Handle "Edited" status and display
                    if (edited != null && edited && editDate != null && !editDate.isEmpty()) {
                        try {
                            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                            Date date = inputFormat.parse(editDate);
                            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy.MM.dd | hh:mm a", Locale.getDefault());
                            String formattedEditDate = outputFormat.format(date);
                            editedTextView.setText("Edited: " + formattedEditDate);
                            editedTextView.setVisibility(View.VISIBLE); // Make 'Edited' TextView visible
                        } catch (ParseException e) {
                            Log.e(TAG, "Error parsing edit date: " + editDate, e);
                            editedTextView.setText("Edited"); // Fallback to just "Edited"
                            editedTextView.setVisibility(View.VISIBLE); // Make 'Edited' TextView visible
                        }
                    } else {
                        editedTextView.setVisibility(View.GONE); // Hide 'Edited' TextView if not edited or data is missing
                    }


                    // Load image using Glide library
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(news.this)
                                .load(imageUrl)
                                .placeholder(R.drawable.circular_background_grey) // Placeholder while loading
                                .error(R.drawable.cross) // Image to show if loading fails
                                .into(newsArticleImageView);
                    } else {
                        // If no image URL, set a default image (e.g., a generic news icon or a placeholder)
                        newsArticleImageView.setImageResource(R.drawable.renew); // Replace with a suitable default
                    }

                    // Update top bar title to the news article's title
                    postTitleTextView.setText(title);

                } else {
                    // Document does not exist
                    Toast.makeText(news.this, "News article not found.", Toast.LENGTH_SHORT).show();
                    finish(); // Close activity
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressBar.setVisibility(View.GONE); // Hide progress bar on failure
                Toast.makeText(news.this, "Error loading news article: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error fetching document: " + e.getMessage());
                finish(); // Close activity on error
            }
        });
    }

    /**
     * Loads the current user's profile picture into the top bar icon.
     * Fetches the profile picture URL from the "users" collection in Firestore.
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
                                        .placeholder(R.drawable.user) // Default user icon as placeholder
                                        .error(R.drawable.user) // Default user icon on error
                                        .into(profileIconImageView);
                            } else {
                                profileIconImageView.setImageResource(R.drawable.user); // Default icon if URL is empty/null
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
            profileIconImageView.setImageResource(R.drawable.user); // Default icon if no user is logged in
        }
    }

    /**
     * Handles the back button press.
     * Navigates back to the home screen (or previous relevant activity) and clears the activity stack.
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // You might want to navigate back to the home screen or categories
        Intent intent = new Intent(news.this, home.class); // Assuming 'home.class' is your main feed activity
        // These flags ensure that when you go back, you clear any intermediate activities
        // and start a fresh 'home' activity if it's not already at the top.
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Finish the current news activity
    }
}