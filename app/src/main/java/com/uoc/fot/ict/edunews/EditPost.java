package com.uoc.fot.ict.edunews;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout; // Import TextInputLayout
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

    private static final int PICK_IMAGE_REQUEST = 1;
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
    private TextInputLayout categoryInputLayout; // NEW: Declare TextInputLayout for category

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    // Post Data
    private Uri imageUri; // Currently selected image URI (could be new or existing)
    private String postId;
    private String originalImageUrl; // To store the URL of the image fetched from Firestore
    private boolean isEditMode = false; // To track current mode

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);

        initializeFirebase();
        initializeViews();
        setupCategoryDropdown(); // Setup dropdown *before* listeners so adapter is ready
        setupListeners(); // Now setup listeners

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
        // Assuming profileIcon exists in your XML for completeness
        profileIcon = findViewById(R.id.profileIcon);
        postTitleTextView = findViewById(R.id.postTitle);
        editOrCancelButton = findViewById(R.id.editOrCancelButton);

        titleInput = findViewById(R.id.TitleInput);
        descriptionInput = findViewById(R.id.DescriptionInput);

        // NEW: Initialize the TextInputLayout for the category dropdown
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
        backButton.setOnClickListener(v -> navigateToBack());
        // Handle profile icon click if it exists in your XML
        if (profileIcon != null) {
            profileIcon.setOnClickListener(v -> navigateToUserProfile());
        }
        pickImageButton.setOnClickListener(v -> openImagePicker());
        clearImageButton.setOnClickListener(v -> clearSelectedImage());
        submitButton.setOnClickListener(v -> updatePost());
        deleteButton.setOnClickListener(v -> confirmDeletePost());
        editOrCancelButton.setOnClickListener(v -> toggleEditMode(!isEditMode));

        // IMPORTANT: The categoryInput listeners should ONLY show dropdown in edit mode.
        // These listeners are key for when the user *explicitly* interacts with the field in edit mode.
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

    private void navigateToBack() {
        startActivity(new Intent(this, MyPosts.class));
        finish();
    }

    private void navigateToUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Replace UserInfo.class with your actual user profile activity
            Intent intent = new Intent(this, UserInfo.class); // Assuming UserInfo.class is your user profile activity
            intent.putExtra("userId", currentUser.getUid());
            startActivity(intent);
        } else {
            Toast.makeText(this, "Please log in to view profile.", Toast.LENGTH_SHORT).show();
        }
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
        // Important: Set a threshold for when the dropdown automatically appears.
        // A value of 1 means it will show immediately when typed into or clicked.
        categoryInput.setThreshold(1);

        // This listener ensures that if a selection is made, it updates the text and keeps the dropdown dismissed.
        categoryInput.setOnItemClickListener((parent, view, position, id) -> {
            // Optional: You can do something here if an item is clicked,
            // like setting a flag that a category was selected.
            // For now, just setting the text is handled automatically by AutoCompleteTextView
            // and the dropdown will dismiss itself.
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                postImagePreview.setImageBitmap(bitmap);
                updateImageButtonsVisibility();
            } catch (IOException e) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void clearSelectedImage() {
        imageUri = null;
        postImagePreview.setImageResource(R.drawable.add_icon); // Set to default add icon
        updateImageButtonsVisibility();
    }

    private void updateImageButtonsVisibility() {
        // In edit mode, pickImageButton is always visible when there's no imageUri set
        pickImageButton.setVisibility(isEditMode && imageUri == null ? View.VISIBLE : View.GONE);
        // clearImageButton is visible only if an image is currently displayed (imageUri is not null) AND we are in edit mode
        clearImageButton.setVisibility(isEditMode && imageUri != null ? View.VISIBLE : View.GONE);
        // postImagePreview should always be visible to show either the image or the add_icon
        postImagePreview.setVisibility(View.VISIBLE);
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

        // Crucial for AutoCompleteTextView behavior
        categoryInput.setEnabled(isEditMode);
        // Toggle focusability directly to prevent unwanted interaction in view mode
        categoryInput.setFocusable(isEditMode);
        categoryInput.setFocusableInTouchMode(isEditMode);

        // NEW: Control the end icon of the TextInputLayout for the category dropdown
        if (isEditMode) {
            // When in edit mode, show the dropdown icon
            categoryInputLayout.setEndIconMode(TextInputLayout.END_ICON_DROPDOWN_MENU);
            // We no longer automatically show the dropdown here; it relies on user click/focus.
        } else {
            // When in view mode, hide the dropdown icon
            categoryInputLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);
            categoryInput.dismissDropDown(); // Hide dropdown if exiting edit mode
            categoryInput.clearFocus(); // Ensure it loses focus properly when switching to view mode
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
            // If exiting edit mode, reset to original data if not submitted
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
                                    .placeholder(R.drawable.add_icon) // Placeholder if loading fails
                                    .error(R.drawable.add_icon)
                                    .into(postImagePreview);
                        } else {
                            imageUri = null; // No image for this post
                            postImagePreview.setImageResource(R.drawable.add_icon);
                        }
                        updateImageButtonsVisibility(); // Update visibility based on loaded image
                    } else {
                        Toast.makeText(this, "Post not found.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading post: " + e.getMessage());
                    Toast.makeText(this, "Error loading post: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void updatePost() {
        String title = titleInput.getText().toString().trim();
        String category = categoryInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();

        if (title.isEmpty()) {
            titleInput.setError("Title required");
            return;
        }
        if (category.isEmpty()) {
            categoryInput.setError("Category required");
            return;
        }
        if (description.isEmpty()) {
            descriptionInput.setError("Description required");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        submitButton.setEnabled(false);
        deleteButton.setEnabled(false); // Disable delete during update

        // Check if a new image has been selected or if the image has been cleared
        if (imageUri != null && (originalImageUrl == null || !imageUri.toString().equals(originalImageUrl))) {
            // Case 1: New image selected (imageUri is not null AND it's different from original or original was null)
            uploadNewImageThenUpdatePost(title, category, description);
        } else if (imageUri == null && originalImageUrl != null && !originalImageUrl.isEmpty()) {
            // Case 2: Image was explicitly cleared (imageUri is null AND there was an original image)
            deleteOldImageThenSavePostToFirestore(title, category, description, null);
        } else {
            // Case 3: No change to image (imageUri is same as original, or both are null/empty)
            // Or if originalImageUrl was null/empty and imageUri is also null/empty, just update text data
            savePostToFirestore(title, category, description, imageUri != null ? imageUri.toString() : null);
        }
    }

    private void uploadNewImageThenUpdatePost(String title, String category, String description) {
        // If there was an old image, delete it from storage first to avoid orphaned files
        if (originalImageUrl != null && !originalImageUrl.isEmpty()) {
            deleteOldImageThenPerformNewUpload(title, category, description);
        } else {
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
                Log.e(TAG, "Failed to delete old image before new upload: " + e.getMessage());
                // Continue with new upload even if old one fails to delete
                performImageUploadAndFirestoreUpdate(title, category, description);
            });
        } catch (IllegalArgumentException e) {
            // originalImageUrl might not be a valid Firebase Storage URL. Proceed with new upload.
            Log.w(TAG, "Original image URL is not a valid Firebase Storage URL. Proceeding with new upload.");
            performImageUploadAndFirestoreUpdate(title, category, description);
        }
    }

    private void deleteOldImageThenSavePostToFirestore(String title, String category, String description, String newImageUrl) {
        if (originalImageUrl != null && !originalImageUrl.isEmpty()) {
            try {
                StorageReference oldImageRef = storage.getReferenceFromUrl(originalImageUrl);
                oldImageRef.delete().addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Old image deleted successfully after clear action.");
                    savePostToFirestore(title, category, description, newImageUrl);
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete old image after clear action: " + e.getMessage());
                    // Continue to update Firestore even if deletion fails
                    Toast.makeText(this, "Failed to delete old image, but post data will be updated.", Toast.LENGTH_SHORT).show();
                    savePostToFirestore(title, category, description, newImageUrl);
                });
            } catch (IllegalArgumentException e) {
                // originalImageUrl is not a valid Firebase Storage URL. Proceed with Firestore update.
                Log.w(TAG, "Original image URL is not a valid Firebase Storage URL. Proceeding with Firestore update after clear action.");
                savePostToFirestore(title, category, description, newImageUrl);
            }
        } else {
            savePostToFirestore(title, category, description, newImageUrl);
        }
    }

    private void performImageUploadAndFirestoreUpdate(String title, String category, String description) {
        // This method should only be called if imageUri is NOT null
        if (imageUri == null) {
            Log.e(TAG, "performImageUploadAndFirestoreUpdate called with null imageUri. This should not happen.");
            handleUpdateFailure(new Exception("Image URI is null during upload attempt."));
            return;
        }

        StorageReference storageRef = storage.getReference()
                .child("post_images/" + UUID.randomUUID().toString());

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        storageRef.getDownloadUrl().addOnSuccessListener(uri ->
                                savePostToFirestore(title, category, description, uri.toString())
                        ).addOnFailureListener(this::handleUpdateFailure)
                )
                .addOnFailureListener(this::handleUpdateFailure);
    }

    private void savePostToFirestore(String title, String category, String description, @Nullable String newImageUrl) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            handleUpdateFailure(new Exception("User not logged in"));
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
                    originalImageUrl = newImageUrl; // Update original image URL after successful update
                    imageUri = newImageUrl != null ? Uri.parse(newImageUrl) : null; // Also update current imageUri state
                    toggleEditMode(false); // Switch back to view mode
                })
                .addOnFailureListener(this::handleUpdateFailure);
    }

    private void handleUpdateFailure(Exception e) {
        progressBar.setVisibility(View.GONE);
        submitButton.setEnabled(true);
        deleteButton.setEnabled(true);
        Toast.makeText(this, "Error updating post: " + e.getMessage(), Toast.LENGTH_LONG).show();
        Log.e(TAG, "Error updating post: " + e.getMessage());
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
                    deletePostDocumentFromFirestore();
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete image from storage: " + e.getMessage());
                    // Even if image deletion fails, try to delete the Firestore document
                    Toast.makeText(this, "Failed to delete image, but deleting post data...", Toast.LENGTH_SHORT).show();
                    deletePostDocumentFromFirestore();
                });
            } catch (IllegalArgumentException e) {
                // originalImageUrl is not a valid Firebase Storage URL, proceed to delete document
                Log.w(TAG, "Original image URL is not a valid Firebase Storage URL. Proceeding with Firestore document deletion.");
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
                    navigateToBack(); // Go back to home after deletion
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    submitButton.setEnabled(true);
                    deleteButton.setEnabled(true);
                    Toast.makeText(this, "Error deleting post: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error deleting post document: " + e.getMessage());
                });
    }
}