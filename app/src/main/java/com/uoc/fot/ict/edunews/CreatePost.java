package com.uoc.fot.ict.edunews;

import android.app.Activity; // For Activity.RESULT_OK
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory; // NEW IMPORT: For BitmapFactory
import android.net.Uri;
import android.os.Build; // NEW IMPORT: For Build.VERSION_CODES
import android.os.Bundle;
import android.provider.MediaStore; // Still useful for MediaStore.Images.Media.EXTERNAL_CONTENT_URI
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.graphics.ImageDecoder; // NEW IMPORT: For ImageDecoder

import androidx.activity.result.ActivityResultLauncher; // NEW IMPORT
import androidx.activity.result.contract.ActivityResultContracts; // NEW IMPORT
import androidx.annotation.NonNull; // For @NonNull annotations if needed
import androidx.appcompat.app.AppCompatActivity; // Base Activity
import com.bumptech.glide.Glide; // For image loading
import com.google.android.material.imageview.ShapeableImageView; // If used in XML
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout; // NEW IMPORT: If you want to use setError on TextInputLayout
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.io.InputStream; // NEW IMPORT: For InputStream for bitmap decoding
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects; // For Objects.requireNonNull
import java.util.UUID; // For unique image filenames

public class CreatePost extends AppCompatActivity {

    private static final String TAG = "CreatePost"; // Tag for logging

    // UI Elements
    private ImageButton backButton, pickImageButton, clearImageButton;
    private ShapeableImageView profileIcon; // Assumed to be in XML based on usage
    private TextInputEditText titleInput, descriptionInput;
    private AutoCompleteTextView categoryInput;
    private ImageView postImagePreview;
    private Button submitButton;
    private ProgressBar progressBar;
    private TextInputLayout titleInputLayout, categoryInputLayout, descriptionInputLayout; // NEW: For error handling

    // Firebase instances
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private Uri imageUri; // Currently selected image URI

    // Activity Result Launcher for picking images (replaces deprecated startActivityForResult)
    private ActivityResultLauncher<Intent> pickImageLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        initializeFirebase();
        initializeViews();
        setupCategoryDropdown(); // Setup dropdown *before* listeners
        setupListeners();
        loadCurrentUserProfilePicture();
        updateImageButtonsVisibility(); // Initial visibility update
    }

    /**
     * Initializes Firebase authentication, Firestore, and Storage instances.
     */
    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    /**
     * Initializes all UI elements by finding them from the layout file.
     */
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

        // NEW: Initialize TextInputLayouts for more granular error control
        titleInputLayout = findViewById(R.id.titleInputLayout); // Assuming you have this in your XML
        categoryInputLayout = findViewById(R.id.categoryInputLayout); // Assuming you have this in your XML
        descriptionInputLayout = findViewById(R.id.descriptionInputLayout); // Assuming you have this in your XML

        // Initialize the ActivityResultLauncher
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData(); // Get the URI of the selected image
                        if (imageUri != null) {
                            // Use Glide to load the image into the ImageView directly from the URI
                            Glide.with(this)
                                    .load(imageUri)
                                    .placeholder(R.drawable.rounded_transparent_white_background) // Placeholder while loading
                                    .error(R.drawable.rounded_transparent_white_background) // Image to show if loading fails
                                    .into(postImagePreview);
                            updateImageButtonsVisibility(); // Update button visibility after selection
                        }
                    } else {
                        Toast.makeText(this, "Image selection cancelled.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /**
     * Sets up click listeners for various UI elements.
     */
    private void setupListeners() {
        backButton.setOnClickListener(v -> navigateToHome());
        pickImageButton.setOnClickListener(v -> openImagePicker());
        submitButton.setOnClickListener(v -> createNewPost());
        profileIcon.setOnClickListener(v -> navigateToUserProfile());
        clearImageButton.setOnClickListener(v -> clearSelectedImage());

        // Ensure category dropdown shows when clicked/focused
        categoryInput.setOnClickListener(v -> categoryInput.showDropDown());
        categoryInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                categoryInput.showDropDown();
            }
        });
    }

    /**
     * Navigates the user back to the home screen.
     */
    private void navigateToHome() {
        startActivity(new Intent(this, home.class));
        finish(); // Finish current activity
    }

    /**
     * Navigates the user to their profile information screen.
     */
    private void navigateToUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Intent intent = new Intent(this, UserInfo.class);
            intent.putExtra("userId", currentUser.getUid()); // Pass user ID if UserInfo needs it
            startActivity(intent);
        } else {
            Toast.makeText(this, "Please log in to view profile.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Loads the current user's profile picture from Firestore and displays it.
     */
    private void loadCurrentUserProfilePicture() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
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
                                        .error(R.drawable.user) // Fallback icon on error
                                        .into(profileIcon);
                            } else {
                                profileIcon.setImageResource(R.drawable.user); // Default icon if URL is null or empty
                            }
                        } else {
                            profileIcon.setImageResource(R.drawable.user); // Default icon if user doc not found
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to load profile picture for top bar: " + e.getMessage());
                        profileIcon.setImageResource(R.drawable.user); // Fallback to default icon on error
                    });
        } else {
            profileIcon.setImageResource(R.drawable.user); // Default icon if no user logged in
        }
    }

    /**
     * Sets up the dropdown list for post categories.
     */
    private void setupCategoryDropdown() {
        List<String> categories = Arrays.asList(
                "Business", "Crime", "Editorials", "Political", "Sports",
                "Social", "International", "Technology", "Health", "Education",
                "Environment", "Art & Culture", "Science", "Lifestyle", "Travel"
        );

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.dropdown_item, // Ensure you have this layout defined (e.g., a simple TextView)
                categories
        );
        categoryInput.setAdapter(adapter);
        categoryInput.setThreshold(1); // Show dropdown after 1 character typed
    }

    /**
     * Opens the image picker intent using the ActivityResultLauncher.
     * This replaces the deprecated `startActivityForResult`.
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        pickImageLauncher.launch(Intent.createChooser(intent, "Select Image"));
    }

    /**
     * Clears the currently selected image and resets the preview.
     */
    private void clearSelectedImage() {
        imageUri = null; // Clear the URI
        postImagePreview.setImageResource(R.drawable.rounded_transparent_white_background); // Set to your default placeholder
        updateImageButtonsVisibility(); // Update button visibility
    }

    /**
     * Updates the visibility of the "pick image" and "clear image" buttons
     * based on whether an image is currently selected.
     */
    private void updateImageButtonsVisibility() {
        pickImageButton.setVisibility(imageUri == null ? View.VISIBLE : View.GONE);
        clearImageButton.setVisibility(imageUri != null ? View.VISIBLE : View.GONE);
    }

    /**
     * Gathers input, validates it, and proceeds to create a new post.
     */
    private void createNewPost() {
        // Clear previous errors
        titleInputLayout.setError(null);
        categoryInputLayout.setError(null);
        descriptionInputLayout.setError(null);

        String title = Objects.requireNonNull(titleInput.getText()).toString().trim();
        String category = Objects.requireNonNull(categoryInput.getText()).toString().trim();
        String description = Objects.requireNonNull(descriptionInput.getText()).toString().trim();

        boolean cancel = false; // Flag to indicate if any validation failed

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
            return; // Exit if any validation failed
        }

        progressBar.setVisibility(View.VISIBLE);
        submitButton.setEnabled(false); // Disable button to prevent multiple submissions

        uploadImageToFirebase(title, category, description);
    }

    /**
     * Uploads the selected image to Firebase Storage.
     *
     * @param title       Post title.
     * @param category    Post category.
     * @param description Post description.
     */
    private void uploadImageToFirebase(String title, String category, String description) {
        if (imageUri == null) {
            handleUploadFailure(new Exception("Image URI is null during upload."));
            return;
        }

        StorageReference storageRef = storage.getReference()
                .child("post_images/" + UUID.randomUUID().toString()); // Generate a unique name for the image

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        // Get the download URL after successful upload
                        storageRef.getDownloadUrl().addOnSuccessListener(uri ->
                                savePostToFirestore(title, category, description, uri.toString())
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
     * Saves the post data (including image URL) to Firebase Firestore.
     *
     * @param title       Post title.
     * @param category    Post category.
     * @param description Post description.
     * @param imageUrl    URL of the uploaded image.
     */
    private void savePostToFirestore(@NonNull String title, @NonNull String category, @NonNull String description, @NonNull String imageUrl) {
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
        post.put("author", user.getDisplayName() != null ? user.getDisplayName() : "Anonymous"); // Author name
        post.put("postDate", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())); // Current date/time
        post.put("userId", user.getUid()); // User ID
        post.put("edited", false); // Not edited initially
        post.put("editDate", ""); // Empty edit date initially

        db.collection("posts")
                .add(post)
                .addOnSuccessListener(ref -> {
                    progressBar.setVisibility(View.GONE);
                    submitButton.setEnabled(true);
                    Toast.makeText(this, "Post created successfully!", Toast.LENGTH_SHORT).show();
                    clearFields(); // Clear form fields after successful post
                })
                .addOnFailureListener(this::handleUploadFailure);
    }

    /**
     * Handles failures during image upload or Firestore save.
     *
     * @param e The exception that occurred.
     */
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

    /**
     * Clears all input fields and resets the image preview.
     */
    private void clearFields() {
        titleInput.setText("");
        categoryInput.setText("");
        descriptionInput.setText("");
        clearSelectedImage(); // Also clears imageUri and updates button visibility

        // Clear errors from TextInputLayouts
        titleInputLayout.setError(null);
        categoryInputLayout.setError(null);
        descriptionInputLayout.setError(null);
    }
}