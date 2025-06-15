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
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
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
    private TextInputEditText titleInput, descriptionInput;
    private AutoCompleteTextView categoryInput;
    private ImageView postImagePreview;
    private Button submitButton, deleteButton;
    private ProgressBar progressBar;
    private TextView postTitleTextView;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    // Post Data
    private Uri imageUri;
    private String postId;
    private String originalImageUrl;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);

        initializeFirebase();
        initializeViews();
        setupCategoryDropdown();
        setupListeners();

        // Get post ID from intent
        Intent intent = getIntent();
        postId = intent.getStringExtra("postId");

        if (postId != null && !postId.isEmpty()) {
            loadPostData(postId);
        } else {
            Toast.makeText(this, "Post ID is missing.", Toast.LENGTH_SHORT).show();
            finish();
        }

        toggleEditMode(false);
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        postTitleTextView = findViewById(R.id.postTitle);
        editOrCancelButton = findViewById(R.id.editOrCancelButton);
        titleInput = findViewById(R.id.TitleInput);
        categoryInput = findViewById(R.id.CategoryInput);
        descriptionInput = findViewById(R.id.DescriptionInput);
        postImagePreview = findViewById(R.id.postImagePreview);
        pickImageButton = findViewById(R.id.pickImageButton);
        submitButton = findViewById(R.id.submitButton);
        progressBar = findViewById(R.id.progressBar);
        clearImageButton = findViewById(R.id.clearImageButton);
        deleteButton = findViewById(R.id.deleteButton);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> navigateToBack());
        pickImageButton.setOnClickListener(v -> openImagePicker());
        clearImageButton.setOnClickListener(v -> clearSelectedImage());
        submitButton.setOnClickListener(v -> updatePost());
        deleteButton.setOnClickListener(v -> confirmDeletePost());
        editOrCancelButton.setOnClickListener(v -> toggleEditMode(!isEditMode));
    }

    private void navigateToBack() {
        startActivity(new Intent(this, MyPosts.class));
        finish();
    }

    private void setupCategoryDropdown() {
        List<String> categories = Arrays.asList(
                "Business", "Crime", "Editorials", "Political", "Sports",
                "Social", "International", "Technology", "Health", "Education",
                "Environment", "Art & Culture", "Science", "Lifestyle", "Travel"
        );

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.dropdown_item,
                categories
        );
        categoryInput.setAdapter(adapter);
        categoryInput.setThreshold(1);
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
        postImagePreview.setImageResource(R.drawable.add_icon);
        updateImageButtonsVisibility();
    }

    private void updateImageButtonsVisibility() {
        pickImageButton.setVisibility(isEditMode && imageUri == null ? View.VISIBLE : View.GONE);
        clearImageButton.setVisibility(isEditMode && imageUri != null ? View.VISIBLE : View.GONE);
        postImagePreview.setVisibility(View.VISIBLE);
    }

    private void toggleEditMode(boolean enableEdit) {
        isEditMode = enableEdit;

        // Set enabled state for input fields
        titleInput.setEnabled(isEditMode);
        descriptionInput.setEnabled(isEditMode);

        // Configure category input based on edit mode
        categoryInput.setEnabled(isEditMode);
        categoryInput.setFocusable(isEditMode);
        categoryInput.setFocusableInTouchMode(isEditMode);
        categoryInput.setClickable(isEditMode);
        categoryInput.setCursorVisible(isEditMode);

        if (!isEditMode) {
            categoryInput.dismissDropDown();
        }

        // Manage visibility of buttons
        submitButton.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        deleteButton.setVisibility(isEditMode ? View.VISIBLE : View.GONE);

        // Update image picker/clear buttons
        updateImageButtonsVisibility();

        // Change the edit/cancel button icon and description
        if (isEditMode) {
            editOrCancelButton.setImageResource(R.drawable.cross);
            editOrCancelButton.setContentDescription("Cancel Edit");
            postTitleTextView.setText("Edit Post");
        } else {
            editOrCancelButton.setImageResource(R.drawable.edit);
            editOrCancelButton.setContentDescription("Edit Post");
            postTitleTextView.setText("View/Edit Post");
            if (postId != null) {
                loadPostData(postId);
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
                        categoryInput.setText(documentSnapshot.getString("category"), false);
                        descriptionInput.setText(documentSnapshot.getString("description"));
                        originalImageUrl = documentSnapshot.getString("imageUrl");

                        if (originalImageUrl != null && !originalImageUrl.isEmpty()) {
                            imageUri = Uri.parse(originalImageUrl);
                            Glide.with(this)
                                    .load(originalImageUrl)
                                    .placeholder(R.drawable.add_icon)
                                    .error(R.drawable.add_icon)
                                    .into(postImagePreview);
                        } else {
                            imageUri = null;
                            postImagePreview.setImageResource(R.drawable.add_icon);
                        }
                        updateImageButtonsVisibility();
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
        deleteButton.setEnabled(false);

        if (imageUri != null && (originalImageUrl == null || !imageUri.toString().equals(originalImageUrl))) {
            uploadNewImageThenUpdatePost(title, category, description);
        } else if (imageUri == null && originalImageUrl != null && !originalImageUrl.isEmpty()) {
            deleteOldImageThenSavePostToFirestore(title, category, description, null);
        } else {
            savePostToFirestore(title, category, description, imageUri != null ? imageUri.toString() : null);
        }
    }

    private void uploadNewImageThenUpdatePost(String title, String category, String description) {
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
                performImageUploadAndFirestoreUpdate(title, category, description);
            });
        } catch (IllegalArgumentException e) {
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
                    Toast.makeText(this, "Failed to delete old image, but post data will be updated.", Toast.LENGTH_SHORT).show();
                    savePostToFirestore(title, category, description, newImageUrl);
                });
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Original image URL is not a valid Firebase Storage URL. Proceeding with Firestore update after clear action.");
                savePostToFirestore(title, category, description, newImageUrl);
            }
        } else {
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
        updates.put("imageUrl", newImageUrl);
        updates.put("edited", true);
        updates.put("editDate", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

        postRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    submitButton.setEnabled(true);
                    deleteButton.setEnabled(true);
                    Toast.makeText(this, "Post updated successfully!", Toast.LENGTH_SHORT).show();
                    originalImageUrl = newImageUrl;
                    imageUri = newImageUrl != null ? Uri.parse(newImageUrl) : null;
                    toggleEditMode(false);
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

        if (originalImageUrl != null && !originalImageUrl.isEmpty()) {
            try {
                StorageReference imageRef = storage.getReferenceFromUrl(originalImageUrl);
                imageRef.delete().addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Image deleted from storage successfully.");
                    deletePostDocumentFromFirestore();
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete image from storage: " + e.getMessage());
                    Toast.makeText(this, "Failed to delete image, but deleting post data...", Toast.LENGTH_SHORT).show();
                    deletePostDocumentFromFirestore();
                });
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Original image URL is not a valid Firebase Storage URL. Proceeding with Firestore document deletion.");
                deletePostDocumentFromFirestore();
            }
        } else {
            deletePostDocumentFromFirestore();
        }
    }

    private void deletePostDocumentFromFirestore() {
        db.collection("posts").document(postId).delete()
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Post deleted successfully!", Toast.LENGTH_SHORT).show();
                    navigateToBack();
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