package com.uoc.fot.ict.edunews;

import android.content.Intent;
import android.graphics.Bitmap; // Kept for potential future use, not directly used by current image loading
import android.net.Uri;
import android.os.Build; // For ImageDecoder (API 28+)
import android.os.Bundle;
import android.provider.MediaStore; // Kept for potential future use, not directly used by current image loading
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.ImageDecoder; // For ImageDecoder (API 28+)

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class EditPost extends AppCompatActivity {

    private static final String TAG = "EditPost";

    // UI Elements
    private ImageButton backButton, pickImageButton, clearImageButton, editOrCancelButton;
    private ShapeableImageView profileIcon; // Assuming you have this in your XML for completeness
    private TextInputEditText titleInput, descriptionInput;
    private AutoCompleteTextView categoryInput;
    private ImageView postImagePreview;
    private Button submitButton, deleteButton;
    private ProgressBar progressBar;
    private TextView postTitleTextView; // To change text in top bar
    private TextInputLayout categoryInputLayout; // Declare TextInputLayout for category

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    // Post Data
    private Uri imageUri; // Currently selected image URI (could be new or existing)
    private String postId;
    private String originalImageUrl; // To store the URL of the image fetched from Firestore
    private boolean isEditMode = false; // To track current mode

    // Activity Result Launcher for image picking
    private ActivityResultLauncher<Intent> pickImageLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);

        initializeFirebase();
        initializeViews();
        setupCategoryDropdown();

        // Initialize Activity Result Launcher
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData(); // Get the URI of the selected image
                        if (imageUri != null) {
                            // Use Glide to load the image into the ImageView directly from the URI
                            Glide.with(this)
                                    .load(imageUri)
                                    .placeholder(R.drawable.rounded_transparent_white_background) // Placeholder while loading
                                    .error(R.drawable.rounded_transparent_white_background) // Image to show if loading fails
                                    .into(postImagePreview);
                            updateImageButtonsVisibility();
                        }
                    } else {
                        Toast.makeText(this, "Image selection cancelled.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Register OnBackPressedCallback for modern back press handling
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                Log.d(TAG, "Back button pressed, handling via dispatcher.");
                // Custom logic for onBackPressed: If in edit mode, ask to discard changes
                if (isEditMode) {
                    new AlertDialog.Builder(EditPost.this)
                            .setTitle("Discard Changes?")
                            .setMessage("You have unsaved changes. Do you want to discard them and exit edit mode?")
                            .setPositiveButton("Discard", (dialog, which) -> {
                                isEditMode = false;
                                loadPostData(postId); // Reload original data to revert
                                toggleEditMode(false); // Switch to view mode (which will also reset UI elements)
                                Toast.makeText(EditPost.this, "Changes discarded.", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", null) // Do nothing on cancel
                            .show();
                } else {
                    // If not in edit mode, simply navigate back to MyPosts
                    navigateToMyPosts();
                }
            }
        });

        setupListeners();

        // Get post ID from intent
        Intent intent = getIntent();
        postId = intent.getStringExtra("postId");

        if (postId != null && !postId.isEmpty()) {
            loadPostData(postId);
        } else {
            Toast.makeText(this, "Post ID is missing.", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if no post ID
        }

        // Initial state is view mode
        toggleEditMode(false); // Call with false to set initial view mode
    }

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
        postTitleTextView = findViewById(R.id.postTitle);
        editOrCancelButton = findViewById(R.id.editOrCancelButton);

        titleInput = findViewById(R.id.TitleInput);
        descriptionInput = findViewById(R.id.DescriptionInput);

        categoryInputLayout = findViewById(R.id.categoryInputLayout);
        categoryInput = findViewById(R.id.CategoryInput);

        postImagePreview = findViewById(R.id.postImagePreview);
        pickImageButton = findViewById(R.id.pickImageButton);
        submitButton = findViewById(R.id.submitButton);
        progressBar = findViewById(R.id.progressBar);
        clearImageButton = findViewById(R.id.clearImageButton);
        deleteButton = findViewById(R.id.deleteButton);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        if (profileIcon != null) {
            profileIcon.setOnClickListener(v -> navigateToUserProfile());
        }
        pickImageButton.setOnClickListener(v -> openImagePicker());
        clearImageButton.setOnClickListener(v -> clearSelectedImage());
        submitButton.setOnClickListener(v -> updatePost());
        deleteButton.setOnClickListener(v -> confirmDeletePost());
        editOrCancelButton.setOnClickListener(v -> toggleEditMode(!isEditMode));

        categoryInput.setOnClickListener(v -> {
            if (isEditMode) {
                categoryInput.showDropDown();
            }
        });
        categoryInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && isEditMode) {
                categoryInput.showDropDown();
            }
        });
    }

    private void navigateToUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Intent intent = new Intent(this, UserInfo.class); // Full path to your UserInfo activity
            intent.putExtra("userId", currentUser.getUid());
            startActivity(intent);
        } else {
            Toast.makeText(this, "Please log in to view profile.", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToMyPosts() {
        startActivity(new Intent(this, MyPosts.class));
        finish(); // Finish EditPost activity to remove it from the back stack
    }

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
        categoryInput.setThreshold(1);
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
        // In edit mode:
        // pickImageButton is visible only if no image is currently selected (imageUri is null)
        pickImageButton.setVisibility(isEditMode && imageUri == null ? View.VISIBLE : View.GONE);
        // clearImageButton is visible only if an image IS currently selected (imageUri is not null)
        clearImageButton.setVisibility(isEditMode && imageUri != null ? View.VISIBLE : View.GONE);

        // postImagePreview should always be visible to show either the loaded image or the default placeholder.
        // Its source is handled by Glide or setImageResource.
        postImagePreview.setVisibility(View.VISIBLE);
        // Ensure placeholder is set if no image is present (especially when transitioning from loaded image to no image)
        if (imageUri == null && !isEditMode) { // If not in edit mode and no image, show default
            postImagePreview.setImageResource(R.drawable.rounded_transparent_white_background);
        }
    }

    /**
     * Toggles the UI elements between view mode and edit mode.
     * @param enableEdit true to enable edit mode, false to enable view mode.
     */
    private void toggleEditMode(boolean enableEdit) {
        isEditMode = enableEdit;

        // Set enabled state for input fields
        titleInput.setEnabled(isEditMode);
        descriptionInput.setEnabled(isEditMode);

        categoryInput.setEnabled(isEditMode);
        categoryInput.setFocusable(isEditMode);
        categoryInput.setFocusableInTouchMode(isEditMode);

        // Control the end icon of the TextInputLayout for the category dropdown
        if (isEditMode) {
            categoryInputLayout.setEndIconMode(TextInputLayout.END_ICON_DROPDOWN_MENU);
        } else {
            categoryInputLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);
            categoryInput.dismissDropDown();
            categoryInput.clearFocus();
        }

        // Manage visibility of buttons
        submitButton.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        deleteButton.setVisibility(isEditMode ? View.VISIBLE : View.GONE);

        // Update image picker/clear buttons visibility based on edit mode and imageUri state
        updateImageButtonsVisibility();

        // Change the edit/cancel button icon and description
        if (isEditMode) {
            editOrCancelButton.setImageResource(R.drawable.cross); // Assuming 'cross' is your cancel icon
            editOrCancelButton.setContentDescription("Cancel Edit");
            postTitleTextView.setText("Edit Post");
        } else {
            editOrCancelButton.setImageResource(R.drawable.edit); // Assuming 'edit' is your edit icon
            editOrCancelButton.setContentDescription("Edit Post");
            postTitleTextView.setText("View/Edit Post");
            // If exiting edit mode, reset to original data if not submitted.
            // This is important to revert changes if "Cancel" is pressed
            if (postId != null) {
                loadPostData(postId); // Reload original data to revert unsaved changes
            }
        }
    }

    private void loadPostData(String postId) {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("posts").document(postId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        titleInput.setText(documentSnapshot.getString("title"));
                        categoryInput.setText(documentSnapshot.getString("category"), false); // Set text without filtering/showing dropdown
                        descriptionInput.setText(documentSnapshot.getString("description"));
                        originalImageUrl = documentSnapshot.getString("imageUrl"); // Store original URL

                        if (originalImageUrl != null && !originalImageUrl.isEmpty()) {
                            imageUri = Uri.parse(originalImageUrl); // Set current imageUri to original for logic
                            Glide.with(this)
                                    .load(originalImageUrl)
                                    .placeholder(R.drawable.rounded_transparent_white_background) // Changed to match your new drawable
                                    .error(R.drawable.rounded_transparent_white_background) // Changed to match your new drawable
                                    .into(postImagePreview);
                        } else {
                            imageUri = null; // No image for this post
                            postImagePreview.setImageResource(R.drawable.rounded_transparent_white_background);
                        }
                        updateImageButtonsVisibility(); // Update visibility based on loaded image
                    } else {
                        Toast.makeText(this, "Post not found.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading post: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
                    Toast.makeText(this, "Error loading post: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void updatePost() {
        String title = titleInput.getText().toString().trim();
        String category = categoryInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();

        if (title.isEmpty()) {
            titleInput.setError("Title required");
            Toast.makeText(this, "Title is required.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (category.isEmpty()) {
            categoryInputLayout.setError("Category required"); // Set error on TextInputLayout
            Toast.makeText(this, "Category is required.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (description.isEmpty()) {
            descriptionInput.setError("Description required");
            Toast.makeText(this, "Description is required.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        submitButton.setEnabled(false);
        deleteButton.setEnabled(false);

        // Check if a new image has been selected or if the image has been cleared
        // Condition: imageUri is NOT null (there's an image to potentially upload)
        // AND (originalImageUrl was null/empty OR the new imageUri is different from the original URL)
        if (imageUri != null && (originalImageUrl == null || !imageUri.toString().equals(originalImageUrl))) {
            // Case 1: New image selected (or first image being added)
            uploadNewImageThenUpdatePost(title, category, description);
        } else if (imageUri == null && originalImageUrl != null && !originalImageUrl.isEmpty()) {
            // Case 2: Image was explicitly cleared (imageUri is null AND there WAS an original image)
            deleteOldImageThenSavePostToFirestore(title, category, description, null); // Pass null for newImageUrl
        } else {
            // Case 3: No change to image (imageUri is same as original, or both are null/empty)
            // Just update text data.
            savePostToFirestore(title, category, description, imageUri != null ? imageUri.toString() : null);
        }
    }

    private void uploadNewImageThenUpdatePost(String title, String category, String description) {
        // If there was an old image, attempt to delete it from storage first
        if (originalImageUrl != null && !originalImageUrl.isEmpty()) {
            deleteOldImageThenPerformNewUpload(title, category, description);
        } else {
            // No old image, directly perform new upload
            performImageUploadAndFirestoreUpdate(title, category, description);
        }
    }

    private void deleteOldImageThenPerformNewUpload(String title, String category, String description) {
        try {
            StorageReference oldImageRef = storage.getReferenceFromUrl(originalImageUrl);
            oldImageRef.delete().addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Old image deleted successfully before new upload.");
                performImageUploadAndFirestoreUpdate(title, category, description);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to delete old image before new upload: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
                // Important: Continue with new upload even if old one fails to delete
                Toast.makeText(this, "Failed to delete old image, but uploading new one.", Toast.LENGTH_SHORT).show();
                performImageUploadAndFirestoreUpdate(title, category, description);
            });
        } catch (IllegalArgumentException e) {
            // originalImageUrl might not be a valid Firebase Storage URL (e.g., it's a local URI or malformed).
            // Log a warning and proceed with new upload.
            Log.w(TAG, "Original image URL is not a valid Firebase Storage URL. Proceeding with new upload. Error: " + e.getMessage());
            performImageUploadAndFirestoreUpdate(title, category, description);
        }
    }

    private void deleteOldImageThenSavePostToFirestore(String title, String category, String description, @Nullable String newImageUrl) {
        if (originalImageUrl != null && !originalImageUrl.isEmpty()) {
            try {
                StorageReference oldImageRef = storage.getReferenceFromUrl(originalImageUrl);
                oldImageRef.delete().addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Old image deleted successfully after clear action.");
                    savePostToFirestore(title, category, description, newImageUrl);
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete old image after clear action: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
                    // Continue to update Firestore even if deletion fails
                    Toast.makeText(this, "Failed to delete old image, but post data will be updated.", Toast.LENGTH_SHORT).show();
                    savePostToFirestore(title, category, description, newImageUrl);
                });
            } catch (IllegalArgumentException e) {
                // originalImageUrl is not a valid Firebase Storage URL. Proceed with Firestore update.
                Log.w(TAG, "Original image URL is not a valid Firebase Storage URL. Proceeding with Firestore update after clear action. Error: " + e.getMessage());
                savePostToFirestore(title, category, description, newImageUrl);
            }
        } else {
            // No original image to delete, just save post to Firestore
            savePostToFirestore(title, category, description, newImageUrl);
        }
    }

    private void performImageUploadAndFirestoreUpdate(String title, String category, String description) {
        if (imageUri == null) {
            Log.e(TAG, "performImageUploadAndFirestoreUpdate called with null imageUri. This should not happen.");
            handleUpdateFailure(new Exception("Image URI is null during upload attempt."));
            return;
        }

        StorageReference storageRef = storage.getReference()
                .child("post_images/" + UUID.randomUUID().toString()); // Use unique ID for new images

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        storageRef.getDownloadUrl().addOnSuccessListener(uri ->
                                savePostToFirestore(title, category, description, uri.toString())
                        ).addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to get download URL: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
                            handleUpdateFailure(new Exception("Failed to get image download URL."));
                        })
                )
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Image upload failed: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
                    handleUpdateFailure(new Exception("Image upload failed."));
                });
    }

    private void savePostToFirestore(String title, String category, String description, @Nullable String newImageUrl) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            handleUpdateFailure(new Exception("User not logged in"));
            // Optionally, redirect to login if user session is invalid
            // startActivity(new Intent(this, SignIn.class));
            // finish();
            return;
        }

        DocumentReference postRef = db.collection("posts").document(postId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("category", category);
        updates.put("description", description);
        updates.put("imageUrl", newImageUrl); // This will be null if image was cleared
        updates.put("edited", true); // Set to true on edit
        updates.put("editDate", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

        postRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    submitButton.setEnabled(true);
                    deleteButton.setEnabled(true);
                    Toast.makeText(this, "Post updated successfully!", Toast.LENGTH_SHORT).show();
                    // Navigate to MyPosts after successful update
                    navigateToMyPosts();
                })
                .addOnFailureListener(this::handleUpdateFailure);
    }

    private void handleUpdateFailure(Exception e) {
        progressBar.setVisibility(View.GONE);
        submitButton.setEnabled(true);
        deleteButton.setEnabled(true);
        Toast.makeText(this, "Error updating post: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
        Log.e(TAG, "Error updating post: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
    }

    private void confirmDeletePost() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Post")
                .setMessage("Are you sure you want to delete this post? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deletePost())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deletePost() {
        if (postId == null || postId.isEmpty()) {
            Toast.makeText(this, "Cannot delete: Post ID is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        submitButton.setEnabled(false);
        deleteButton.setEnabled(false);

        // First, delete the image from Firebase Storage if it exists
        if (originalImageUrl != null && !originalImageUrl.isEmpty()) {
            try {
                StorageReference imageRef = storage.getReferenceFromUrl(originalImageUrl);
                imageRef.delete().addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Image deleted from storage successfully.");
                    deletePostDocumentFromFirestore(); // Proceed to delete Firestore document
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete image from storage: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
                    // Even if image deletion fails, try to delete the Firestore document to avoid orphaned data
                    Toast.makeText(this, "Failed to delete image, but deleting post data...", Toast.LENGTH_SHORT).show();
                    deletePostDocumentFromFirestore();
                });
            } catch (IllegalArgumentException e) {
                // originalImageUrl is not a valid Firebase Storage URL (e.g., local URI, malformed).
                // Log a warning and proceed with Firestore document deletion.
                Log.w(TAG, "Original image URL is not a valid Firebase Storage URL. Proceeding with Firestore document deletion. Error: " + e.getMessage());
                deletePostDocumentFromFirestore();
            }
        } else {
            // No image to delete, proceed directly to Firestore document deletion
            deletePostDocumentFromFirestore();
        }
    }

    private void deletePostDocumentFromFirestore() {
        db.collection("posts").document(postId).delete()
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Post deleted successfully!", Toast.LENGTH_SHORT).show();
                    navigateToMyPosts(); // Go back to MyPosts after deletion
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    submitButton.setEnabled(true);
                    deleteButton.setEnabled(true);
                    Toast.makeText(this, "Error deleting post: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error deleting post document: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
                });
    }
}