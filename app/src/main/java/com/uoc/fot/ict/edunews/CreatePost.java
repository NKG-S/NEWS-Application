package com.uoc.fot.ict.edunews;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch; // NEW IMPORT for Switch
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot; // For fetching username
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class CreatePost extends AppCompatActivity {

    private static final String TAG = "CreatePost";

    // UI Elements
    private ImageButton backButton, pickImageButton, clearImageButton;
    private ShapeableImageView profileIcon;
    private TextInputEditText titleInput, descriptionInput;
    private AutoCompleteTextView categoryInput;
    private ImageView postImagePreview;
    private Button submitButton;
    private ProgressBar progressBar;
    private TextInputLayout titleInputLayout, categoryInputLayout, descriptionInputLayout;
    private Switch anonymousToggle; // NEW: Anonymous post toggle

    // Firebase instances
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private Uri imageUri;

    // Activity Result Launcher for picking images
    private ActivityResultLauncher<Intent> pickImageLauncher;

    // To store the current user's username for non-anonymous posts
    private String currentUserName = "User"; // Default or fallback username

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        initializeFirebase();
        initializeViews();
        setupCategoryDropdown();
        setupListeners();
        loadCurrentUserProfileData(); // Modified to load both profile picture and username
        updateImageButtonsVisibility();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        profileIcon = findViewById(R.id.profileIcon);
        titleInput = findViewById(R.id.TitleInput);
        categoryInput = findViewById(R.id.CategoryInput);
        descriptionInput = findViewById(R.id.DescriptionInput);
        postImagePreview = findViewById(R.id.postImagePreview);
        pickImageButton = findViewById(R.id.pickImageButton);
        submitButton = findViewById(R.id.submitButton);
        progressBar = findViewById(R.id.progressBar);
        clearImageButton = findViewById(R.id.clearImageButton);
        anonymousToggle = findViewById(R.id.anonymousToggle); // NEW: Initialize the Switch

        titleInputLayout = findViewById(R.id.titleInputLayout);
        categoryInputLayout = findViewById(R.id.categoryInputLayout);
        descriptionInputLayout = findViewById(R.id.descriptionInputLayout);

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        if (imageUri != null) {
                            Glide.with(this)
                                    .load(imageUri)
                                    .placeholder(R.drawable.rounded_transparent_white_background)
                                    .error(R.drawable.rounded_transparent_white_background)
                                    .into(postImagePreview);
                            updateImageButtonsVisibility();
                        }
                    } else {
                        Toast.makeText(this, "Image selection cancelled.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> navigateToHome());
        pickImageButton.setOnClickListener(v -> openImagePicker());
        submitButton.setOnClickListener(v -> createNewPost());
        profileIcon.setOnClickListener(v -> navigateToUserProfile());
        clearImageButton.setOnClickListener(v -> clearSelectedImage());

        categoryInput.setOnClickListener(v -> categoryInput.showDropDown());
        categoryInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                categoryInput.showDropDown();
            }
        });
    }

    private void navigateToHome() {
        startActivity(new Intent(this, home.class));
        finish();
    }

    private void navigateToUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Intent intent = new Intent(this, UserInfo.class);
            intent.putExtra("userId", currentUser.getUid());
            startActivity(intent);
        } else {
            Toast.makeText(this, "Please log in to view profile.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Loads the current user's profile picture and username from Firestore.
     */
    private void loadCurrentUserProfileData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Load profile picture
                            String profilePictureUrl = documentSnapshot.getString("profilePictureUrl");
                            if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(profilePictureUrl)
                                        .placeholder(R.drawable.user)
                                        .error(R.drawable.user)
                                        .into(profileIcon);
                            } else {
                                profileIcon.setImageResource(R.drawable.user);
                            }

                            // Load username for non-anonymous posts
                            String username = documentSnapshot.getString("username");
                            if (username != null && !username.isEmpty()) {
                                currentUserName = username;
                            } else {
                                currentUserName = "Unknown User"; // Fallback if username not found in Firestore
                            }
                        } else {
                            profileIcon.setImageResource(R.drawable.user);
                            currentUserName = "Unknown User";
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to load profile data for top bar: " + e.getMessage());
                        profileIcon.setImageResource(R.drawable.user);
                        currentUserName = "Unknown User";
                    });
        } else {
            profileIcon.setImageResource(R.drawable.user);
            currentUserName = "Guest User"; // For truly unauthenticated access, though usually this page requires auth.
        }
    }

    /**
     * Sets up the dropdown list for post categories using a string array from resources.
     * This makes the categories easily modifiable in `arrays.xml`.
     * Depends on: `categoryInput`.
     */
    private void setupCategoryDropdown() {
        // Load categories from the string-array resource defined in arrays.xml
        String[] categoriesArray = getResources().getStringArray(R.array.post_categories);

        // Create an ArrayAdapter using the loaded array and a custom dropdown item layout
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.dropdown_item, // Ensure you have this layout defined (e.g., a simple TextView)
                categoriesArray // Use the array loaded from resources
        );
        categoryInput.setAdapter(adapter);
        categoryInput.setThreshold(1); // Show dropdown after 1 character typed (or immediately on click)
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        pickImageLauncher.launch(Intent.createChooser(intent, "Select Image"));
    }

    private void clearSelectedImage() {
        imageUri = null;
        postImagePreview.setImageResource(R.drawable.rounded_transparent_white_background);
        updateImageButtonsVisibility();
    }

    private void updateImageButtonsVisibility() {
        pickImageButton.setVisibility(imageUri == null ? View.VISIBLE : View.GONE);
        clearImageButton.setVisibility(imageUri != null ? View.VISIBLE : View.GONE);
    }

    private void createNewPost() {
        // Clear previous errors
        titleInputLayout.setError(null);
        categoryInputLayout.setError(null);
        descriptionInputLayout.setError(null);

        String title = Objects.requireNonNull(titleInput.getText()).toString().trim();
        String category = Objects.requireNonNull(categoryInput.getText()).toString().trim();
        String description = Objects.requireNonNull(descriptionInput.getText()).toString().trim();
        boolean isAnonymous = anonymousToggle.isChecked(); // NEW: Get state of anonymous toggle

        boolean cancel = false;

        if (title.isEmpty()) {
            titleInputLayout.setError("Title is required.");
            Toast.makeText(this, "Title is required.", Toast.LENGTH_SHORT).show();
            cancel = true;
        }
        if (category.isEmpty()) {
            categoryInputLayout.setError("Category is required.");
            Toast.makeText(this, "Category is required.", Toast.LENGTH_SHORT).show();
            cancel = true;
        }
        if (description.isEmpty()) {
            descriptionInputLayout.setError("Description is required.");
            Toast.makeText(this, "Description is required.", Toast.LENGTH_SHORT).show();
            cancel = true;
        }
        if (imageUri == null) {
            Toast.makeText(this, "Please select an image for the post.", Toast.LENGTH_SHORT).show();
            cancel = true;
        }

        if (cancel) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        submitButton.setEnabled(false);

        // Pass isAnonymous to the image upload function
        uploadImageToFirebase(title, category, description, isAnonymous);
    }

    /**
     * Uploads the selected image to Firebase Storage.
     *
     * @param title       Post title.
     * @param category    Post category.
     * @param description Post description.
     * @param isAnonymous Whether the post should be anonymous.
     */
    private void uploadImageToFirebase(String title, String category, String description, boolean isAnonymous) {
        if (imageUri == null) {
            handleUploadFailure(new Exception("Image URI is null during upload."));
            return;
        }

        StorageReference storageRef = storage.getReference()
                .child("post_images/" + UUID.randomUUID().toString());

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        storageRef.getDownloadUrl().addOnSuccessListener(uri ->
                                // Pass isAnonymous to savePostToFirestore
                                savePostToFirestore(title, category, description, uri.toString(), isAnonymous)
                        ).addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to get download URL: " + e.getMessage());
                            handleUploadFailure(new Exception("Failed to get image download URL."));
                        })
                )
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Image upload failed: " + e.getMessage());
                    handleUploadFailure(new Exception("Image upload failed."));
                });
    }

    /**
     * Saves the post data (including image URL and anonymity status) to Firebase Firestore.
     *
     * @param title       Post title.
     * @param category    Post category.
     * @param description Post description.
     * @param imageUrl    URL of the uploaded image.
     * @param isAnonymous Whether the post should be anonymous.
     */
    private void savePostToFirestore(@NonNull String title, @NonNull String category, @NonNull String description, @NonNull String imageUrl, boolean isAnonymous) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            handleUploadFailure(new Exception("User not logged in"));
            return;
        }

        Map<String, Object> post = new HashMap<>();
        post.put("title", title);
        post.put("category", category);
        post.put("description", description);
        post.put("imageUrl", imageUrl);
        // FIX: Set author based on isAnonymous toggle
        post.put("author", isAnonymous ? "Anonymous" : currentUserName);
        post.put("isAnonymousPost", isAnonymous); // NEW: Store anonymity status in Firestore
        post.put("postDate", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        post.put("userId", user.getUid());
        post.put("edited", false);
        post.put("editDate", "");

        db.collection("posts")
                .add(post)
                .addOnSuccessListener(ref -> {
                    progressBar.setVisibility(View.GONE);
                    submitButton.setEnabled(true);
                    Toast.makeText(this, "Post created successfully!", Toast.LENGTH_SHORT).show();
                    clearFields();
                })
                .addOnFailureListener(this::handleUploadFailure);
    }

    private void handleUploadFailure(Exception e) {
        progressBar.setVisibility(View.GONE);
        submitButton.setEnabled(true);
        String errorMessage = "An error occurred.";
        if (e != null && e.getMessage() != null) {
            errorMessage += " " + e.getMessage();
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Post creation failed: " + errorMessage, e);
    }

    private void clearFields() {
        titleInput.setText("");
        categoryInput.setText("");
        descriptionInput.setText("");
        anonymousToggle.setChecked(false); // Reset toggle to false after post
        clearSelectedImage();

        titleInputLayout.setError(null);
        categoryInputLayout.setError(null);
        descriptionInputLayout.setError(null);
    }
}