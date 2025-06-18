package com.uoc.fot.ict.edunews;

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
import android.widget.TextView;
import android.widget.Toast;

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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * EditPost Activity allows users to view and edit their existing news posts.
 * It integrates with Firebase Firestore for post data, Firebase Storage for images,
 * and Firebase Authentication for user context. It provides functionalities
 * for editing text fields, changing post images, and deleting posts.
 */
public class EditPost extends AppCompatActivity {

    private static final String TAG = "EditPost";

    // UI Elements
    private ImageButton backButton, pickImageButton, clearImageButton, editOrCancelButton;
    private ShapeableImageView profileIcon;
    private TextInputEditText titleInput, descriptionInput;
    private AutoCompleteTextView categoryInput;
    private ImageView postImagePreview;
    private Button submitButton, deleteButton;
    private ProgressBar progressBar;
    private TextView postTitleTextView;
    private TextInputLayout categoryInputLayout;

    // Firebase Instances
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    // Post Data
    private String postId;
    private Uri imageUri; // Currently displayed image URI (local or remote)
    private String originalImageUrl; // Stores the original image URL from Firestore
    private String originalTitle;
    private String originalCategory;
    private String originalDescription;
    private boolean isEditMode = false; // Tracks current UI mode (view or edit)

    // Activity Result Launcher for picking images (modern approach)
    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);

        initializeFirebase();
        initializeViews();
        setupCategoryDropdown();
        setupImagePickerLauncher();
        setupOnBackPressedCallback();
        setupListeners();

        // Get post ID from the intent that launched this activity
        Intent intent = getIntent();
        postId = intent.getStringExtra("postId");

        if (postId != null && !postId.isEmpty()) {
            loadPostData(postId); // Load post details from Firestore
        } else {
            Toast.makeText(this, "Post ID is missing.", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if no valid post ID
        }

        // Set initial state to view mode
        toggleEditMode(false);
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

    /**
     * Sets up the ActivityResultLauncher for selecting images from the gallery.
     * This handles the result of the image picking intent.
     */
    private void setupImagePickerLauncher() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData(); // Get the URI of the selected image
                        if (imageUri != null) {
                            // Display the newly selected image using Glide
                            Glide.with(this)
                                    .load(imageUri)
                                    .placeholder(R.drawable.rounded_transparent_white_background)
                                    .error(R.drawable.rounded_transparent_white_background)
                                    .into(postImagePreview);
                            updateImageButtonsVisibility(); // Adjust image buttons visibility
                        }
                    } else {
                        Toast.makeText(this, "Image selection cancelled.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /**
     * Registers the OnBackPressedCallback for custom back button behavior.
     * If in edit mode, prompts user to discard changes. Otherwise, navigates to MyPosts.
     */
    private void setupOnBackPressedCallback() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                Log.d(TAG, "Back button pressed, handling via dispatcher.");
                if (isEditMode) {
                    // Show a dialog to confirm discarding unsaved changes
                    new AlertDialog.Builder(EditPost.this)
                            .setTitle("Discard Changes?")
                            .setMessage("You have unsaved changes. Do you want to discard them and exit edit mode?")
                            .setPositiveButton("Discard", (dialog, which) -> {
                                // Revert to view mode and reload original data
                                isEditMode = false;
                                loadPostData(postId);
                                toggleEditMode(false);
                                Toast.makeText(EditPost.this, "Changes discarded.", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", null) // Do nothing on dialog cancel
                            .show();
                } else {
                    // If not in edit mode, simply navigate back to MyPosts
                    navigateToMyPosts();
                }
            }
        });
    }

    /**
     * Sets up click listeners for various UI elements.
     */
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

        // Ensure category dropdown shows when clicked/focused, only if in edit mode
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

    /**
     * Navigates the user to their profile information screen.
     * Depends on `mAuth` to get the current user's ID.
     */
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
     * Navigates the user back to the MyPosts activity.
     * Finishes the current activity to remove it from the back stack.
     */
    private void navigateToMyPosts() {
        startActivity(new Intent(this, MyPosts.class));
        finish();
    }

    /**
     * Sets up the dropdown list for post categories using a string array from resources.
     * This makes the categories easily modifiable in `arrays.xml`.
     */
    private void setupCategoryDropdown() {
        // Load categories from string-array resource
        String[] categoriesArray = getResources().getStringArray(R.array.post_categories);
        List<String> categories = Arrays.asList(categoriesArray);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.dropdown_item, // Custom layout for dropdown items
                categories
        );
        categoryInput.setAdapter(adapter);
        categoryInput.setThreshold(1); // Show dropdown after 1 character typed (or immediately on click)
    }

    /**
     * Opens the image picker intent using `pickImageLauncher`.
     * Allows the user to select an image from their device.
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*"); // Filter for image files
        pickImageLauncher.launch(Intent.createChooser(intent, "Select Image"));
    }

    /**
     * Clears the currently selected/displayed image.
     * Resets `imageUri` to null and sets a default placeholder on the ImageView.
     */
    private void clearSelectedImage() {
        imageUri = null; // Clear the URI
        postImagePreview.setImageResource(R.drawable.rounded_transparent_white_background);
        updateImageButtonsVisibility(); // Adjust button visibility
    }

    /**
     * Updates the visibility of the image picking and clearing buttons based on
     * the current edit mode and whether an image is currently selected (`imageUri`).
     */
    private void updateImageButtonsVisibility() {
        // In edit mode: pick button is visible if no image, clear button if image is present.
        // In view mode: both are hidden.
        pickImageButton.setVisibility(isEditMode && imageUri == null ? View.VISIBLE : View.GONE);
        clearImageButton.setVisibility(isEditMode && imageUri != null ? View.VISIBLE : View.GONE);

        // postImagePreview is always visible to show content (image or placeholder).
        postImagePreview.setVisibility(View.VISIBLE);
    }

    /**
     * Toggles the UI elements between view mode (fields disabled, action buttons hidden)
     * and edit mode (fields enabled, submit/delete buttons visible).
     * @param enableEdit true to enable edit mode, false to enable view mode.
     */
    private void toggleEditMode(boolean enableEdit) {
        isEditMode = enableEdit;

        // Set enabled state for input fields based on edit mode
        titleInput.setEnabled(isEditMode);
        descriptionInput.setEnabled(isEditMode);
        categoryInput.setEnabled(isEditMode);
        categoryInput.setFocusable(isEditMode);
        categoryInput.setFocusableInTouchMode(isEditMode);

        // Control the end icon for the category dropdown in TextInputLayout
        if (isEditMode) {
            categoryInputLayout.setEndIconMode(TextInputLayout.END_ICON_DROPDOWN_MENU);
        } else {
            categoryInputLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);
            categoryInput.dismissDropDown(); // Hide dropdown if visible
            categoryInput.clearFocus(); // Remove focus
        }

        // Manage visibility of action buttons (Submit, Delete)
        submitButton.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        deleteButton.setVisibility(isEditMode ? View.VISIBLE : View.GONE);

        updateImageButtonsVisibility(); // Update image-related buttons based on new mode

        // Change the text and icon of the top-right edit/cancel button
        if (isEditMode) {
            editOrCancelButton.setImageResource(R.drawable.cross); // Set to a "cancel" icon
            editOrCancelButton.setContentDescription("Cancel Edit");
            postTitleTextView.setText("Edit Post");
        } else {
            editOrCancelButton.setImageResource(R.drawable.edit); // Set to an "edit" icon
            editOrCancelButton.setContentDescription("Edit Post");
            postTitleTextView.setText("View/Edit Post");
            // If exiting edit mode, revert input fields to original data.
            // This is crucial if the "Cancel" button (which uses toggleEditMode(false)) is pressed.
            if (postId != null) {
                loadPostData(postId);
            }
        }
    }

    /**
     * Loads the post data from Firestore using the `postId`.
     * Populates UI fields and stores original values for change detection.
     * @param postId The ID of the post to load.
     * Dependencies: `db`, `titleInput`, `categoryInput`, `descriptionInput`, `postImagePreview`.
     * Affects: `originalTitle`, `originalCategory`, `originalDescription`, `originalImageUrl`, `imageUri`.
     */
    private void loadPostData(String postId) {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("posts").document(postId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        // Store original values for comparison later
                        originalTitle = documentSnapshot.getString("title");
                        originalCategory = documentSnapshot.getString("category");
                        originalDescription = documentSnapshot.getString("description");
                        originalImageUrl = documentSnapshot.getString("imageUrl");

                        // Populate UI with fetched data
                        titleInput.setText(originalTitle);
                        categoryInput.setText(originalCategory, false); // `false` prevents dropdown from showing
                        descriptionInput.setText(originalDescription);

                        // Load image
                        if (originalImageUrl != null && !originalImageUrl.isEmpty()) {
                            imageUri = Uri.parse(originalImageUrl); // Set current imageUri to original URL
                            Glide.with(this)
                                    .load(originalImageUrl)
                                    .placeholder(R.drawable.rounded_transparent_white_background)
                                    .error(R.drawable.rounded_transparent_white_background)
                                    .into(postImagePreview);
                        } else {
                            imageUri = null; // No image exists for this post
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
                    Log.e(TAG, "Error loading post: " + (e != null ? e.getMessage() : "Unknown error"));
                    Toast.makeText(this, "Error loading post: " + (e != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    /**
     * Attempts to update the post. It first validates inputs and then checks
     * if any changes have been made to the text fields or the image.
     * If no changes, it shows a Toast message and exits.
     * Otherwise, it proceeds with image upload/deletion and Firestore update.
     * Dependencies: `titleInput`, `categoryInput`, `descriptionInput`, `imageUri`, `originalImageUrl`,
     * `originalTitle`, `originalCategory`, `originalDescription`.
     */
    private void updatePost() {
        // Clear previous errors from input layouts
        titleInput.setError(null);
        categoryInputLayout.setError(null);
        descriptionInput.setError(null);

        // Get current values from input fields
        String newTitle = titleInput.getText().toString().trim();
        String newCategory = categoryInput.getText().toString().trim();
        String newDescription = descriptionInput.getText().toString().trim();

        // Input validation checks
        if (newTitle.isEmpty()) {
            titleInput.setError("Title required");
            Toast.makeText(this, "Title is required.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newCategory.isEmpty()) {
            categoryInputLayout.setError("Category required");
            Toast.makeText(this, "Category is required.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newDescription.isEmpty()) {
            descriptionInput.setError("Description required");
            Toast.makeText(this, "Description is required.", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- NEW LOGIC: Check for changes before proceeding ---
        boolean textDataChanged = !newTitle.equals(originalTitle) ||
                !newCategory.equals(originalCategory) ||
                !newDescription.equals(originalDescription);

        // Logic for image changes:
        // 1. New image selected (imageUri is not null AND different from originalImageUrl)
        // 2. Original image was cleared (imageUri is null AND originalImageUrl was not null)
        // 3. No image originally, but a new image is added (imageUri is not null AND originalImageUrl is null/empty)
        boolean imageChanged = (imageUri != null && (originalImageUrl == null || !imageUri.toString().equals(originalImageUrl))) ||
                (imageUri == null && originalImageUrl != null && !originalImageUrl.isEmpty());

        if (!textDataChanged && !imageChanged) {
            // No changes detected, show Toast and revert to view mode
            Toast.makeText(this, "No changes to save.", Toast.LENGTH_SHORT).show();
            toggleEditMode(false); // Go back to view mode
            return; // Exit the function, no Firestore update needed
        }
        // --- END NEW LOGIC ---

        // Show progress and disable buttons during update
        progressBar.setVisibility(View.VISIBLE);
        submitButton.setEnabled(false);
        deleteButton.setEnabled(false);

        // Handle image update scenarios
        if (imageChanged) {
            // If a new image is selected, or an image was cleared
            if (imageUri != null && (originalImageUrl == null || !imageUri.toString().equals(originalImageUrl))) {
                // Case 1: New image selected (or first image being added)
                uploadNewImageThenUpdatePost(newTitle, newCategory, newDescription);
            } else { // imageUri == null && originalImageUrl != null && !originalImageUrl.isEmpty()
                // Case 2: Image was explicitly cleared
                deleteOldImageThenSavePostToFirestore(newTitle, newCategory, newDescription, null); // Pass null for newImageUrl
            }
        } else {
            // Case 3: No change to image, just update text data.
            savePostToFirestore(newTitle, newCategory, newDescription, imageUri != null ? imageUri.toString() : null);
        }
    }

    /**
     * Handles the scenario where a new image is selected for an existing post.
     * If there was an old image, it attempts to delete it before uploading the new one.
     * @param title The updated post title.
     * @param category The updated post category.
     * @param description The updated post description.
     * Dependencies: `originalImageUrl`, `storage`.
     */
    private void uploadNewImageThenUpdatePost(String title, String category, String description) {
        if (originalImageUrl != null && !originalImageUrl.isEmpty() && originalImageUrl.contains("firebasestorage.googleapis.com")) {
            try {
                StorageReference oldImageRef = storage.getReferenceFromUrl(originalImageUrl);
                oldImageRef.delete().addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Old image deleted successfully before new upload.");
                    performImageUploadAndFirestoreUpdate(title, category, description);
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete old image before new upload (continuing with new upload): " + (e != null ? e.getMessage() : "Unknown error"));
                    Toast.makeText(this, "Failed to delete old image, but uploading new one.", Toast.LENGTH_SHORT).show();
                    performImageUploadAndFirestoreUpdate(title, category, description);
                });
            } catch (IllegalArgumentException e) {
                // If originalImageUrl is not a valid Firebase Storage URL, log and proceed
                Log.w(TAG, "Original image URL is not a valid Firebase Storage URL. Proceeding with new upload. Error: " + e.getMessage());
                performImageUploadAndFirestoreUpdate(title, category, description);
            }
        } else {
            // No old image or invalid old image URL, directly perform new upload
            performImageUploadAndFirestoreUpdate(title, category, description);
        }
    }

    /**
     * Handles the scenario where an existing post's image is cleared.
     * Deletes the old image from storage and then updates Firestore with a null image URL.
     * @param title The updated post title.
     * @param category The updated post category.
     * @param description The updated post description.
     * @param newImageUrl Should be `null` in this case to clear the image.
     * Dependencies: `originalImageUrl`, `storage`.
     */
    private void deleteOldImageThenSavePostToFirestore(String title, String category, String description, @Nullable String newImageUrl) {
        if (originalImageUrl != null && !originalImageUrl.isEmpty() && originalImageUrl.contains("firebasestorage.googleapis.com")) {
            try {
                StorageReference oldImageRef = storage.getReferenceFromUrl(originalImageUrl);
                oldImageRef.delete().addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Old image deleted successfully after clear action.");
                    savePostToFirestore(title, category, description, newImageUrl); // Update Firestore with null URL
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete old image after clear action (continuing with Firestore update): " + (e != null ? e.getMessage() : "Unknown error"));
                    Toast.makeText(this, "Failed to delete old image, but post data will be updated.", Toast.LENGTH_SHORT).show();
                    savePostToFirestore(title, category, description, newImageUrl);
                });
            } catch (IllegalArgumentException e) {
                // Original image URL is not a valid Firebase Storage URL, proceed with Firestore update
                Log.w(TAG, "Original image URL is not a valid Firebase Storage URL. Proceeding with Firestore update after clear action. Error: " + e.getMessage());
                savePostToFirestore(title, category, description, newImageUrl);
            }
        } else {
            // No original image to delete, just save post to Firestore with null URL
            savePostToFirestore(title, category, description, newImageUrl);
        }
    }

    /**
     * Performs the actual image upload to Firebase Storage and then calls
     * `savePostToFirestore` with the new image's download URL.
     * @param title The updated post title.
     * @param category The updated post category.
     * @param description The updated post description.
     * Dependencies: `imageUri`, `storage`, `mAuth`, `postId`.
     */
    private void performImageUploadAndFirestoreUpdate(String title, String category, String description) {
        if (imageUri == null) {
            Log.e(TAG, "performImageUploadAndFirestoreUpdate called with null imageUri. This indicates a logic error.");
            handleUpdateFailure(new Exception("Image URI is null during upload attempt."));
            return;
        }

        // Generate a unique path for the new image in Firebase Storage
        StorageReference storageRef = storage.getReference()
                .child("post_images/" + UUID.randomUUID().toString());

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        // Get the download URL after successful upload
                        storageRef.getDownloadUrl().addOnSuccessListener(uri ->
                                savePostToFirestore(title, category, description, uri.toString())
                        ).addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to get download URL: " + (e != null ? e.getMessage() : "Unknown error"));
                            handleUpdateFailure(new Exception("Failed to get image download URL."));
                        })
                )
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Image upload failed: " + (e != null ? e.getMessage() : "Unknown error"));
                    handleUpdateFailure(new Exception("Image upload failed."));
                });
    }

    /**
     * Saves the post data (including updated fields and image URL) to Firebase Firestore.
     * This is the final step for updating the post document.
     * @param title The new title.
     * @param category The new category.
     * @param description The new description.
     * @param newImageUrl The new image URL (can be null if image was cleared).
     * Dependencies: `db`, `postId`, `mAuth`.
     */
    private void savePostToFirestore(String title, String category, String description, @Nullable String newImageUrl) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            handleUpdateFailure(new Exception("User not logged in."));
            // If user session is invalid, redirect to login
            Intent intent = new Intent(this, SignIn.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        DocumentReference postRef = db.collection("posts").document(postId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("category", category);
        updates.put("description", description);
        updates.put("imageUrl", newImageUrl); // Will be null if image was cleared
        updates.put("edited", true); // Mark post as edited
        updates.put("editDate", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())); // Update edit date

        postRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    submitButton.setEnabled(true);
                    deleteButton.setEnabled(true);
                    Toast.makeText(this, "Post updated successfully!", Toast.LENGTH_SHORT).show();
                    // Update local original values to reflect the saved state
                    originalTitle = title;
                    originalCategory = category;
                    originalDescription = description;
                    originalImageUrl = newImageUrl;
                    imageUri = newImageUrl != null ? Uri.parse(newImageUrl) : null;
                    toggleEditMode(false); // Switch back to view mode
                })
                .addOnFailureListener(this::handleUpdateFailure);
    }

    /**
     * Handles failures during post update operations (image upload or Firestore save).
     * Re-enables UI elements and displays a Toast message.
     * @param e The exception that occurred (can be null).
     */
    private void handleUpdateFailure(Exception e) {
        progressBar.setVisibility(View.GONE);
        submitButton.setEnabled(true);
        deleteButton.setEnabled(true);
        String errorMessage = "Error updating post.";
        if (e != null && e.getMessage() != null) {
            errorMessage += " " + e.getMessage();
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Error updating post: " + errorMessage, e);
    }

    /**
     * Displays an AlertDialog to confirm post deletion before proceeding.
     */
    private void confirmDeletePost() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Post")
                .setMessage("Are you sure you want to delete this post? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deletePost())
                .setNegativeButton("Cancel", null) // Do nothing on cancel
                .show();
    }

    /**
     * Deletes the post, including its image from Firebase Storage (if present)
     * and its document from Firestore.
     * Dependencies: `postId`, `originalImageUrl`, `storage`, `db`.
     */
    private void deletePost() {
        if (postId == null || postId.isEmpty()) {
            Toast.makeText(this, "Cannot delete: Post ID is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        submitButton.setEnabled(false);
        deleteButton.setEnabled(false);

        // First, delete the image from Firebase Storage if it exists and is a Firebase URL
        if (originalImageUrl != null && !originalImageUrl.isEmpty() && originalImageUrl.contains("firebasestorage.googleapis.com")) {
            try {
                StorageReference imageRef = storage.getReferenceFromUrl(originalImageUrl);
                imageRef.delete().addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Image deleted from storage successfully.");
                    deletePostDocumentFromFirestore(); // Proceed to delete Firestore document
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete image from storage (continuing with document deletion): " + (e != null ? e.getMessage() : "Unknown error"));
                    Toast.makeText(this, "Failed to delete image, but deleting post data...", Toast.LENGTH_SHORT).show();
                    deletePostDocumentFromFirestore();
                });
            } catch (IllegalArgumentException e) {
                // If originalImageUrl is not a valid Firebase Storage URL, log and proceed
                Log.w(TAG, "Original image URL is not a valid Firebase Storage URL. Proceeding with Firestore document deletion. Error: " + e.getMessage());
                deletePostDocumentFromFirestore();
            }
        } else {
            // No image to delete or not a Firebase Storage URL, proceed directly to Firestore document deletion
            deletePostDocumentFromFirestore();
        }
    }

    /**
     * Deletes the post document from Firebase Firestore after image deletion (if any).
     * Dependencies: `db`, `postId`.
     */
    private void deletePostDocumentFromFirestore() {
        db.collection("posts").document(postId).delete()
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Post deleted successfully!", Toast.LENGTH_SHORT).show();
                    navigateToMyPosts(); // Go back to MyPosts after successful deletion
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    submitButton.setEnabled(true);
                    deleteButton.setEnabled(true);
                    String errorMessage = "Error deleting post document.";
                    if (e != null && e.getMessage() != null) {
                        errorMessage += " " + e.getMessage();
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error deleting post document: " + errorMessage, e);
                });
    }
}