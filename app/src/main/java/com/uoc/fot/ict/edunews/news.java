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
import android.widget.ScrollView; // Explicitly import ScrollView

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.transition.TransitionManager; // For smooth animations

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.card.MaterialCardView; // Import MaterialCardView
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

/**
 * The 'news' (NewsArticleActivity) displays a detailed view of a single news article.
 * It fetches article data from Firestore, handles date formatting, "edited" status,
 * and enables copyable descriptions with clickable links. The UI features an image
 * that, when clicked, smoothly reveals or hides the overlapping news description.
 */
public class news extends AppCompatActivity {

    private static final String TAG = "NewsArticleActivity";

    // UI Elements for Top Bar
    private TextView postTitleTextView;
    private ShapeableImageView profileIconImageView;
    private ImageButton backButton;

    // UI Elements for News Article Content
    private TextView newsArticleTitleTextView;
    private TextView newsArticleDateTimeTextView;
    private TextView editedTextView;
    private TextView newsArticleAuthorTextView;
    private ImageView newsArticleImageView;
    private TextView newsArticleDescriptionTextView;
    private ProgressBar progressBar;
    private MaterialCardView contentCard; // Reference to the MaterialCardView

    // Layout and Animation related
    private ConstraintLayout contentRootLayout; // Parent ConstraintLayout for image and card
    private ConstraintSet overlappedConstraintSet; // State when description overlaps image (initial state from XML)
    private ConstraintSet revealedConstraintSet;   // State when description is below image (after click)
    private boolean isDescriptionOverlappingImage = true; // Current state of the description

    // Firebase instances
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // Article Data
    private String currentArticleId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);

        initializeFirebase();
        initializeViews();
        setupOnBackPressedCallback();
        setupListeners(); // Setup listeners after views are initialized

        // Get news article ID from the intent that started this activity
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("NEWS_ARTICLE_ID")) {
            currentArticleId = intent.getStringExtra("NEWS_ARTICLE_ID");
            fetchNewsArticle(currentArticleId); // Always attempt to fetch the current article
        } else {
            // If no article ID is provided at all, show a toast and close the activity
            Toast.makeText(this, "No news article ID provided to display.", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Load the current user's profile picture for the top bar icon
        loadCurrentUserProfilePicture();

        // Important: Setup ConstraintSet states after the layout has been measured,
        // so that the initial XML-defined constraints are captured correctly.
        newsArticleImageView.post(this::setupContentLayoutStates);
    }

    /**
     * Initializes Firebase authentication and Firestore instances.
     */
    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();
    }

    /**
     * Initializes all UI elements by finding them from the layout file.
     */
    private void initializeViews() {
        // Top bar elements
        postTitleTextView = findViewById(R.id.postTitle);
        profileIconImageView = findViewById(R.id.profileIcon);
        backButton = findViewById(R.id.backButton);

        // News article content elements
        newsArticleTitleTextView = findViewById(R.id.newsArticleTitle);
        newsArticleDateTimeTextView = findViewById(R.id.newsArticleDateTime);
        editedTextView = findViewById(R.id.edited);
        newsArticleAuthorTextView = findViewById(R.id.newsArticleAuthor);
        newsArticleImageView = findViewById(R.id.newsArticleImage);
        newsArticleDescriptionTextView = findViewById(R.id.newsArticleDescription);
        progressBar = findViewById(R.id.progressBar);
        contentCard = findViewById(R.id.contentCard); // Initialize MaterialCardView
        contentRootLayout = findViewById(R.id.contentRootLayout); // Initialize parent ConstraintLayout
    }

    /**
     * Registers an OnBackPressedCallback to handle the device's back button press.
     * It navigates the user back to the home screen and clears the activity stack.
     */
    private void setupOnBackPressedCallback() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true /* enabled */) {
            @Override
            public void handleOnBackPressed() {
                Log.d(TAG, "Back button pressed, handling via dispatcher.");
                // Navigate back to the home screen, clearing intermediate activities
                Intent intent = new Intent(news.this, home.class); // Assuming 'home.class' is your main feed activity
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish(); // Finish the current news activity
            }
        });
    }

    /**
     * Sets up click listeners for various UI elements like back button, profile icon,
     * and the news article image for description reveal/hide.
     */
    private void setupListeners() {
        // Top bar back button
        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Top bar profile icon
        if (profileIconImageView != null) {
            profileIconImageView.setOnClickListener(v -> {
                Intent intent = new Intent(this, UserInfo.class);
                startActivity(intent);
            });
        }

        // News article image click listener for description reveal/hide animation
        newsArticleImageView.setOnClickListener(v -> toggleDescriptionVisibility());
    }

    /**
     * Sets up the initial ConstraintSet states for the content card animation.
     * This method captures the initial state (from XML's -40dp margin) as 'overlappedConstraintSet'
     * and then defines the 'revealed' state where the description is fully below the image.
     */
    private void setupContentLayoutStates() {
        overlappedConstraintSet = new ConstraintSet();
        revealedConstraintSet = new ConstraintSet();

        // Capture the current (initial) state of the contentRootLayout, which includes
        // the XML defined layout_marginTop="-40dp" for contentCard.
        overlappedConstraintSet.clone(contentRootLayout);

        // Define the 'revealed' state based on the 'overlapped' state.
        revealedConstraintSet.clone(overlappedConstraintSet);

        // Modify 'revealedConstraintSet': contentCard simply below the image with a positive margin.
        // Clear the existing top constraint from contentCard relative to newsArticleImage.
        revealedConstraintSet.clear(R.id.contentCard, ConstraintSet.TOP);
        // Then, connect contentCard's top to newsArticleImage's bottom with a positive margin.
        revealedConstraintSet.connect(R.id.contentCard, ConstraintSet.TOP,
                R.id.newsArticleImage, ConstraintSet.BOTTOM, dpToPx(16)); // 16dp margin below image

        // The initial state is already set by the XML, so no need to apply overlappedConstraintSet here.
        isDescriptionOverlappingImage = true; // Confirm initial state
    }

    /**
     * Toggles the visibility of the description by animating the content card's position.
     * It smoothly moves the card down to reveal the full image, or back up to overlap.
     */
    private void toggleDescriptionVisibility() {
        // Begin a smooth transition for the layout changes
        TransitionManager.beginDelayedTransition(contentRootLayout);

        if (isDescriptionOverlappingImage) {
            // Currently overlapping (initial state), move description down to reveal image
            revealedConstraintSet.applyTo(contentRootLayout);
        } else {
            // Currently revealed, move description up to overlap image (back to initial state)
            overlappedConstraintSet.applyTo(contentRootLayout);
        }
        isDescriptionOverlappingImage = !isDescriptionOverlappingImage; // Toggle the state
    }

    /**
     * Converts density-independent pixels (dp) to pixels (px).
     * @param dp The dp value to convert.
     * @return The converted pixel value.
     */
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f); // +0.5f for rounding to nearest int
    }

    /**
     * Fetches the news article data from Firestore using the provided article ID.
     * Populates the UI elements with the fetched data.
     * This method includes checks to prevent Glide from loading images into a destroyed activity.
     * @param articleId The document ID of the news article in Firestore's "posts" collection.
     */
    private void fetchNewsArticle(String articleId) {
        progressBar.setVisibility(View.VISIBLE); // Show progress bar
        DocumentReference docRef = db.collection("posts").document(articleId);

        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                progressBar.setVisibility(View.GONE); // Hide progress bar on success

                // Crucial: Check if the activity is still valid before performing UI operations
                if (isFinishing() || isDestroyed()) {
                    Log.d(TAG, "Activity is no longer valid, skipping UI updates after fetch.");
                    return;
                }

                if (documentSnapshot.exists()) {
                    // Retrieve data from DocumentSnapshot, ensuring field names match Firestore.
                    String title = documentSnapshot.getString("title");
                    String description = documentSnapshot.getString("description");
                    String imageUrl = documentSnapshot.getString("imageUrl");
                    String postDate = documentSnapshot.getString("postDate");
                    String author = documentSnapshot.getString("author");
                    Boolean edited = documentSnapshot.getBoolean("edited");
                    String editDate = documentSnapshot.getString("editDate");
                    Boolean isAnonymousPost = documentSnapshot.getBoolean("isAnonymousPost");

                    // Populate UI elements
                    newsArticleTitleTextView.setText(title);
                    newsArticleDescriptionTextView.setText(description);

                    // Display author, considering anonymous status
                    String authorToDisplay = "Unknown Author";
                    if (isAnonymousPost != null && isAnonymousPost) {
                        authorToDisplay = "Anonymous";
                    } else if (author != null && !author.isEmpty()) {
                        authorToDisplay = author;
                    }
                    newsArticleAuthorTextView.setText("By: " + authorToDisplay);

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
                            newsArticleDateTimeTextView.setVisibility(View.VISIBLE);
                        } catch (ParseException e) {
                            Log.e(TAG, "Error parsing post date: " + postDate, e);
                            newsArticleDateTimeTextView.setText(postDate); // Fallback to raw date string
                            newsArticleDateTimeTextView.setVisibility(View.VISIBLE);
                        }
                    } else {
                        newsArticleDateTimeTextView.setVisibility(View.GONE);
                    }

                    // Handle "Edited" status and display
                    if (edited != null && edited && editDate != null && !editDate.isEmpty()) {
                        try {
                            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                            Date date = inputFormat.parse(editDate);
                            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy.MM.dd | hh:mm a", Locale.getDefault());
                            String formattedEditDate = outputFormat.format(date);
                            editedTextView.setText("Edited: " + formattedEditDate);
                            editedTextView.setVisibility(View.VISIBLE);
                        } catch (ParseException e) {
                            Log.e(TAG, "Error parsing edit date: " + editDate, e);
                            editedTextView.setText("Edited"); // Fallback
                            editedTextView.setVisibility(View.VISIBLE);
                        }
                    } else {
                        editedTextView.setVisibility(View.GONE);
                    }

                    // Load image using Glide library
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(news.this)
                                .load(imageUrl)
                                .placeholder(R.drawable.circular_background_grey) // Placeholder
                                .error(R.drawable.cross) // Error image
                                .into(newsArticleImageView);
                    } else {
                        newsArticleImageView.setImageResource(R.drawable.renew); // Default image if no URL
                    }

                    // Update top bar title
                    postTitleTextView.setText(title);

                } else {
                    Toast.makeText(news.this, "News article not found.", Toast.LENGTH_SHORT).show();
                    finish(); // Close activity
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressBar.setVisibility(View.GONE); // Hide progress bar on failure

                // Crucial: Check if the activity is still valid before showing Toast/logging
                if (isFinishing() || isDestroyed()) {
                    Log.d(TAG, "Activity is no longer valid, skipping error UI updates.");
                    return;
                }

                Toast.makeText(news.this, "Error loading news article: " + (e != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error fetching document: " + (e != null ? e.getMessage() : "Unknown error"), e);
                finish(); // Close activity on error
            }
        });
    }

    /**
     * Loads the current user's profile picture into the top bar icon.
     * Fetches the URL from Firestore's "users" collection.
     * Includes checks to prevent Glide from loading images into a destroyed activity.
     */
    private void loadCurrentUserProfilePicture() {
        if (currentUser != null) {
            String userId = currentUser.getUid();
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        // Crucial: Check if the activity is still valid before performing UI operations
                        if (isFinishing() || isDestroyed()) {
                            Log.d(TAG, "Activity is no longer valid, skipping profile picture load.");
                            return;
                        }

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
                        Log.e(TAG, "Failed to load profile picture for top bar: " + (e != null ? e.getMessage() : "Unknown error"));
                        // Even on failure, check activity state before setting image
                        if (!isFinishing() && !isDestroyed()) {
                            profileIconImageView.setImageResource(R.drawable.user);
                        }
                    });
        } else {
            // No user logged in, set default icon if activity is still valid
            if (!isFinishing() && !isDestroyed()) {
                profileIconImageView.setImageResource(R.drawable.user);
            }
        }
    }
}
