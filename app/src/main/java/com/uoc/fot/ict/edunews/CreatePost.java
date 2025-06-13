package com.uoc.fot.ict.edunews;

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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
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

public class CreatePost extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageButton backButton, pickImageButton, clearImageButton;
    private ShapeableImageView profileIcon;
    private TextInputEditText titleInput, descriptionInput;
    private AutoCompleteTextView categoryInput;
    private ImageView postImagePreview;
    private Button submitButton;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        initializeFirebase();
        initializeViews();
        setupListeners();
        loadUserProfilePicture();
        setupCategoryDropdown();
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
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> navigateToMainActivity());
        pickImageButton.setOnClickListener(v -> openImagePicker());
        submitButton.setOnClickListener(v -> createNewPost());
        profileIcon.setOnClickListener(v -> navigateToUserProfile());
        clearImageButton.setOnClickListener(v -> clearSelectedImage());
    }

    private void navigateToMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void navigateToUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Intent intent = new Intent(this, UserInfo.class);
            intent.putExtra("userId", currentUser.getUid());
            startActivity(intent);
        } else {
            Toast.makeText(this, "Please login to view profile", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUserProfilePicture() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(currentUser.getPhotoUrl())
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .into(profileIcon);
        } else {
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
                R.layout.dropdown_item,
                categories
        );
        categoryInput.setAdapter(adapter);
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
        postImagePreview.setImageResource(R.drawable.image_placeholder_background);
        updateImageButtonsVisibility();
    }

    private void updateImageButtonsVisibility() {
        pickImageButton.setVisibility(imageUri == null ? View.VISIBLE : View.GONE);
        clearImageButton.setVisibility(imageUri != null ? View.VISIBLE : View.GONE);
    }

    private void createNewPost() {
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
        if (imageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        submitButton.setEnabled(false);

        uploadImageToFirebase(title, category, description);
    }

    private void uploadImageToFirebase(String title, String category, String description) {
        StorageReference storageRef = storage.getReference()
                .child("post_images/" + UUID.randomUUID().toString());

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        storageRef.getDownloadUrl().addOnSuccessListener(uri ->
                                savePostToFirestore(title, category, description, uri.toString())
                        ).addOnFailureListener(this::handleUploadFailure)
                )
                .addOnFailureListener(this::handleUploadFailure);
    }

    private void savePostToFirestore(String title, String category, String description, String imageUrl) {
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
        post.put("author", user.getDisplayName() != null ? user.getDisplayName() : "Anonymous");
        post.put("postDate", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        post.put("userId", user.getUid());

        db.collection("posts")
                .add(post)
                .addOnSuccessListener(ref -> {
                    progressBar.setVisibility(View.GONE);
                    submitButton.setEnabled(true);
                    Toast.makeText(this, "Post created!", Toast.LENGTH_SHORT).show();
                    clearFields();
                })
                .addOnFailureListener(this::handleUploadFailure);
    }

    private void handleUploadFailure(Exception e) {
        progressBar.setVisibility(View.GONE);
        submitButton.setEnabled(true);
        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
    }

    private void clearFields() {
        titleInput.setText("");
        categoryInput.setText("");
        descriptionInput.setText("");
        clearSelectedImage();
        titleInput.setError(null);
        categoryInput.setError(null);
        descriptionInput.setError(null);
    }
}