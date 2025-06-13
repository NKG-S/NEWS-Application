package com.uoc.fot.ict.edunews; // IMPORTANT: Ensure this matches your actual package name

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

import de.hdodenhof.circleimageview.CircleImageView;

public class CreatePost extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    // UI Elements
    private ImageButton backButton;
    private CircleImageView profileIcon;
    private TextInputEditText titleInput;
    private AutoCompleteTextView categoryInput;
    private TextInputEditText descriptionInput;
    private ImageView postImagePreview;
    private ImageButton pickImageButton;
    private Button submitButton;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private Uri imageUri; // To store the selected image URI

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post); // Ensure this matches your XML file name

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Initialize UI elements
        backButton = findViewById(R.id.backButton);
        profileIcon = findViewById(R.id.profileIcon);
        titleInput = findViewById(R.id.TitleInput);
        categoryInput = findViewById(R.id.CategoryInput);
        descriptionInput = findViewById(R.id.DescriptionInput);
        postImagePreview = findViewById(R.id.postImagePreview);
        pickImageButton = findViewById(R.id.pickImageButton);
        submitButton = findViewById(R.id.submitButton);
        progressBar = findViewById(R.id.progressBar);

        // Set up Listeners
        backButton.setOnClickListener(v -> navigateToMainActivity());
        pickImageButton.setOnClickListener(v -> openImagePicker());
        submitButton.setOnClickListener(v -> createNewPost());

        // CORRECTED: Listener for profileIcon to navigate to user profile
        profileIcon.setOnClickListener(v -> navigateToUserProfile());

        // Load user profile picture
        loadUserProfilePicture();

        // Set up Category Dropdown
        setupCategoryDropdown();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(CreatePost.this, MainActivity.class);
        startActivity(intent);
        finish(); // Finish the current activity to prevent going back to it
    }

    private void navigateToUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // THIS LINE IS CRUCIAL: Use YOUR UserProfileActivity class, not Firebase's UserInfo interface
            Intent intent = new Intent(CreatePost.this, UserInfo.class);
            // Optionally pass user ID to UserProfileActivity if it needs to fetch user specific data
            intent.putExtra("userId", currentUser.getUid());
            startActivity(intent);
        } else {
            Toast.makeText(this, "You need to be logged in to view your profile.", Toast.LENGTH_SHORT).show();
            // Optionally, prompt user to log in or navigate to login activity
        }
    }

    private void loadUserProfilePicture() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Uri photoUrl = currentUser.getPhotoUrl();

            if (photoUrl != null) {
                // Use Glide to load the profile picture from the URL
                Glide.with(this)
                        .load(photoUrl)
                        .placeholder(R.drawable.user) // Placeholder while loading
                        .error(R.drawable.user) // Image to show if loading fails
                        .into(profileIcon);
            } else {
                // If no photo URL is set in Firebase Auth, use the default drawable
                profileIcon.setImageResource(R.drawable.user);
            }
        } else {
            // No user logged in, display default user icon
            profileIcon.setImageResource(R.drawable.user);
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
                android.R.layout.simple_dropdown_item_1line, // A simple layout for dropdown items
                categories
        );
        categoryInput.setAdapter(adapter);
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*"); // Specifies that we want to pick an image
        intent.setAction(Intent.ACTION_GET_CONTENT); // Action to pick content
        startActivityForResult(Intent.createChooser(intent, "Select Post Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                // Display the selected image in the ImageView
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                postImagePreview.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void createNewPost() {
        String title = titleInput.getText().toString().trim();
        String category = categoryInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();

        // Input validation
        if (title.isEmpty()) {
            titleInput.setError("Title is required");
            titleInput.requestFocus();
            return;
        }
        if (category.isEmpty()) {
            categoryInput.setError("Category is required");
            categoryInput.requestFocus();
            return;
        }
        if (description.isEmpty()) {
            descriptionInput.setError("Description is required");
            descriptionInput.requestFocus();
            return;
        }
        if (imageUri == null) {
            Toast.makeText(this, "Please select an image for your post.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress bar and disable submit button
        progressBar.setVisibility(View.VISIBLE);
        submitButton.setEnabled(false);

        uploadImageToFirebase(title, category, description);
    }

    private void uploadImageToFirebase(String title, String category, String description) {
        // Create a unique name for the image in Firebase Storage
        StorageReference storageRef = storage.getReference().child("post_images/" + UUID.randomUUID().toString());

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Image uploaded successfully, now get its download URL
                    storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        savePostToFirestore(title, category, description, downloadUri.toString());
                    }).addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        submitButton.setEnabled(true);
                        Toast.makeText(CreatePost.this, "Failed to get image download URL: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    submitButton.setEnabled(true);
                    Toast.makeText(CreatePost.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void savePostToFirestore(String title, String category, String description, String imageUrl) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            progressBar.setVisibility(View.GONE);
            submitButton.setEnabled(true);
            Toast.makeText(this, "User not logged in. Cannot create post.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get author name: Prioritize Firebase Auth display name, otherwise default
        String authorName = currentUser.getDisplayName();
        if (authorName == null || authorName.isEmpty()) {
            authorName = "Anonymous"; // Default if display name is not set
            // You might want to fetch this from a 'users' collection in Firestore if available
        }

        // Get current date and time for post date
        String postDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        // Create a map to store post data
        Map<String, Object> post = new HashMap<>();
        post.put("title", title);
        post.put("category", category);
        post.put("description", description);
        post.put("imageUrl", imageUrl);
        post.put("author", authorName);
        post.put("postDate", postDate);
        post.put("userId", currentUser.getUid()); // Store the UID of the author

        // Add the post to the "posts" collection in Firestore
        db.collection("posts")
                .add(post)
                .addOnSuccessListener(documentReference -> {
                    progressBar.setVisibility(View.GONE);
                    submitButton.setEnabled(true);
                    Toast.makeText(CreatePost.this, "Post created successfully!", Toast.LENGTH_SHORT).show();
                    clearFields(); // Clear input fields after successful post
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    submitButton.setEnabled(true);
                    Toast.makeText(CreatePost.this, "Error creating post: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void clearFields() {
        titleInput.setText("");
        categoryInput.setText("");
        descriptionInput.setText("");
        postImagePreview.setImageResource(R.drawable.image_placeholder_background); // Reset image preview to placeholder
        imageUri = null; // Clear the stored image URI
        // Remove error messages
        titleInput.setError(null);
        categoryInput.setError(null);
        descriptionInput.setError(null);
    }
}