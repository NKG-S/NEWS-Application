package com.uoc.fot.ict.edunews;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import de.hdodenhof.circleimageview.CircleImageView;


public class UserInfo extends AppCompatActivity {

    private static final String TAG = "UserInfoActivity";

    // UI Elements
    private TextInputEditText usernameInput, addressInput, mobileInput, emailInput;
    private TextInputLayout usernameInputLayout, addressInputLayout, mobileInputLayout, emailInputLayout;
    private Button editSaveButton, mainActionButton, writeNewPostButton; // Added writeNewPostButton
    private ImageButton backButton, editProfilePictureButton;
    private CircleImageView profilePicture;
    private ProgressBar progressBar;

    // Firebase Instances
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private FirebaseUser currentUser;

    // Local User Data
    private String currentUsername;
    private String currentAddress;
    private String currentMobile;
    private String currentEmail;
    private String currentProfilePictureUrl;
    private boolean isAuthor = false; // Added isAuthor flag
    private Uri selectedImageUri; // For newly selected profile picture

    private boolean isEditMode = false; // Initial state is view mode

    // Activity Result Launcher for picking images
    private ActivityResultLauncher<Intent> pickImageLauncher;

    // For background processing
    private ExecutorService executorService;
    private Handler handler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info); // Assuming your layout file is user_info.xml

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        currentUser = mAuth.getCurrentUser();

        // Initialize ExecutorService and Handler for background tasks
        executorService = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());

        // Initialize UI elements
        usernameInput = findViewById(R.id.UsernameInput);
        addressInput = findViewById(R.id.AddressInput);
        mobileInput = findViewById(R.id.MobileInput);
        emailInput = findViewById(R.id.EmailInput);

        usernameInputLayout = findViewById(R.id.usernameInputLayout);
        addressInputLayout = findViewById(R.id.addressInputLayout);
        mobileInputLayout = findViewById(R.id.mobileInputLayout);
        emailInputLayout = findViewById(R.id.emailInputLayout);

        editSaveButton = findViewById(R.id.editSaveButton);
        mainActionButton = findViewById(R.id.mainActionButton);
        writeNewPostButton = findViewById(R.id.WriteNewPost); // Initialize the new button
        backButton = findViewById(R.id.backButton);
        profilePicture = findViewById(R.id.profilePicture);
        editProfilePictureButton = findViewById(R.id.editProfilePictureButton);
        progressBar = findViewById(R.id.progressBar);

        // Setup ActivityResultLauncher for image picking
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            Glide.with(this).load(selectedImageUri).into(profilePicture);
                            Toast.makeText(this, "Image selected, click 'Save' to upload.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Image selection cancelled.", Toast.LENGTH_SHORT).show();
                        selectedImageUri = null; // Clear selected image if cancelled
                    }
                }
        );

        // Fetch user data from Firebase
        fetchUserData();

        // Set Listeners
        editSaveButton.setOnClickListener(v -> {
            if (isEditMode) {
                saveUserData();
            } else {
                toggleEditMode();
            }
        });

        mainActionButton.setOnClickListener(v -> {
            if (isEditMode) {
                Toast.makeText(this, "Changes discarded.", Toast.LENGTH_SHORT).show();
                isEditMode = false;
                displayUserData(false);
                selectedImageUri = null;
                updateAuthorButtonVisibility(); // Re-evaluate visibility after cancelling edit
            } else {
                signOutUser();
            }
        });

        backButton.setOnClickListener(v -> onBackPressed());

        editProfilePictureButton.setOnClickListener(v -> {
            if (isEditMode) {
                openImageChooser();
            }
        });

        // Set listener for the new "Write A new post" button
        writeNewPostButton.setOnClickListener(v -> {
            Toast.makeText(UserInfo.this, "Opening new post activity...", Toast.LENGTH_SHORT).show();
             Intent intent = new Intent(UserInfo.this, CreatePost.class);
             startActivity(intent);
        });
    }

    /**
     * Fetches user data from Firebase Firestore and updates UI.
     */
    private void fetchUserData() {
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DocumentReference userRef = db.collection("users").document(userId);

            userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            currentUsername = document.getString("username");
                            currentAddress = document.getString("address");
                            currentMobile = document.getString("mobile");
                            currentEmail = currentUser.getEmail();
                            currentProfilePictureUrl = document.getString("profilePictureUrl");
                            // Retrieve author status
                            Boolean authorStatus = document.getBoolean("author");
                            isAuthor = (authorStatus != null && authorStatus); // Default to false if not found/null

                            Log.d(TAG, "User data fetched: " + currentUsername + ", isAuthor: " + isAuthor);
                            displayUserData(false);
                            updateAuthorButtonVisibility(); // Update button visibility after data is fetched
                        } else {
                            Log.d(TAG, "User data document does not exist, creating default.");
                            currentEmail = currentUser.getEmail();
                            currentUsername = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "";
                            currentAddress = "";
                            currentMobile = "";
                            currentProfilePictureUrl = "";
                            isAuthor = false; // Default to not author if document doesn't exist

                            Map<String, Object> defaultUserData = new HashMap<>();
                            defaultUserData.put("username", currentUsername);
                            defaultUserData.put("email", currentEmail);
                            defaultUserData.put("address", currentAddress);
                            defaultUserData.put("mobile", currentMobile);
                            defaultUserData.put("profilePictureUrl", currentProfilePictureUrl);
                            defaultUserData.put("author", isAuthor); // Add author field to default data

                            db.collection("users").document(userId).set(defaultUserData)
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Default user data created in Firestore."))
                                    .addOnFailureListener(e -> Log.e(TAG, "Error creating default user data", e));

                            displayUserData(false);
                            updateAuthorButtonVisibility(); // Update button visibility after default data set
                        }
                    } else {
                        Log.e(TAG, "Failed to load user data from Firestore: ", task.getException());
                        Toast.makeText(UserInfo.this, "Failed to load user data: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        currentEmail = currentUser.getEmail();
                        displayUserData(false);
                        updateAuthorButtonVisibility(); // Ensure button visibility is handled even on error
                    }
                }
            });
        } else {
            Log.d(TAG, "No current user, redirecting to SignIn.");
            Toast.makeText(this, "User not logged in. Please sign in.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(UserInfo.this, SignIn.class); // Assuming SignIn is your login activity
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }


    /**
     * Updates the UI with user data and sets editability.
     */
    private void displayUserData(boolean editable) {
        usernameInput.setText(currentUsername);
        addressInput.setText(currentAddress);
        mobileInput.setText(currentMobile);
        emailInput.setText(currentEmail);

        if (currentProfilePictureUrl != null && !currentProfilePictureUrl.isEmpty()) {
            Glide.with(this)
                    .load(currentProfilePictureUrl)
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .into(profilePicture);
        } else {
            profilePicture.setImageResource(R.drawable.user);
        }

        setFieldEditable(usernameInput, editable);
        setFieldEditable(addressInput, editable);
        setFieldEditable(mobileInput, editable);

        emailInput.setEnabled(false);
        emailInput.setFocusable(false);
        emailInput.setFocusableInTouchMode(false);
        emailInput.setLongClickable(false);
        emailInput.setCursorVisible(false);

        if (editable) {
            editSaveButton.setText("Save");
            editSaveButton.setVisibility(View.VISIBLE);
            editSaveButton.setTextColor(getResources().getColor(R.color.white, getTheme()));
            editSaveButton.setBackgroundTintList(getResources().getColorStateList(R.color.ButtonColour, getTheme()));

            mainActionButton.setText("Cancel");
            mainActionButton.setBackgroundTintList(getResources().getColorStateList(R.color.RedColour, getTheme()));

            editProfilePictureButton.setVisibility(View.VISIBLE);

            usernameInput.setTextColor(getResources().getColor(R.color.black, getTheme()));
            addressInput.setTextColor(getResources().getColor(R.color.black, getTheme()));
            mobileInput.setTextColor(getResources().getColor(R.color.black, getTheme()));
            emailInput.setTextColor(getResources().getColor(R.color.black, getTheme()));

            writeNewPostButton.setVisibility(View.GONE); // Hide "Write New Post" button when in edit mode

        } else { // View Mode
            editSaveButton.setText("Edit");
            editSaveButton.setVisibility(View.VISIBLE);
            editSaveButton.setTextColor(getResources().getColor(R.color.white, getTheme()));
            editSaveButton.setBackgroundTintList(getResources().getColorStateList(R.color.ButtonColour, getTheme()));

            mainActionButton.setText("Sign Out");
            mainActionButton.setBackgroundTintList(getResources().getColorStateList(R.color.ButtonColour, getTheme()));

            editProfilePictureButton.setVisibility(View.GONE);

            usernameInput.setTextColor(Color.parseColor("#80000000"));
            addressInput.setTextColor(Color.parseColor("#80000000"));
            mobileInput.setTextColor(Color.parseColor("#80000000"));
            emailInput.setTextColor(Color.parseColor("#80000000"));

            updateAuthorButtonVisibility(); // Show "Write New Post" button based on isAuthor flag when in view mode
        }
    }

    /**
     * Helper method to set editable state of a TextInputEditText.
     */
    private void setFieldEditable(TextInputEditText field, boolean editable) {
        field.setEnabled(editable);
        field.setFocusable(editable);
        field.setFocusableInTouchMode(editable);
        field.setLongClickable(editable);
        field.setCursorVisible(editable);
    }

    /**
     * Toggles the UI between view and edit mode.
     */
    private void toggleEditMode() {
        isEditMode = !isEditMode;
        displayUserData(isEditMode);
        selectedImageUri = null;
    }

    /**
     * Updates the visibility of the "Write A New Post" button based on the isAuthor flag.
     */
    private void updateAuthorButtonVisibility() {
        if (!isEditMode && isAuthor) {
            writeNewPostButton.setVisibility(View.VISIBLE);
        } else {
            writeNewPostButton.setVisibility(View.GONE);
        }
    }

    /**
     * Saves user data after validation and updates Firebase.
     */
    private void saveUserData() {
        usernameInputLayout.setError(null);
        addressInputLayout.setError(null);
        mobileInputLayout.setError(null);

        String newUsername = Objects.requireNonNull(usernameInput.getText()).toString().trim();
        String newAddress = Objects.requireNonNull(addressInput.getText()).toString().trim();
        String newMobile = Objects.requireNonNull(mobileInput.getText()).toString().trim();

        boolean cancel = false;

        if (TextUtils.isEmpty(newUsername)) {
            usernameInputLayout.setError("Username cannot be empty.");
            cancel = true;
        }
        if (TextUtils.isEmpty(newAddress)) {
            addressInputLayout.setError("Address cannot be empty.");
            cancel = true;
        }
        if (TextUtils.isEmpty(newMobile)) {
            mobileInputLayout.setError("Mobile number cannot be empty.");
            cancel = true;
        } else if (!Pattern.matches("\\d{10}", newMobile)) {
            mobileInputLayout.setError("Please enter a valid 10-digit mobile number.");
            cancel = true;
        }

        if (cancel) {
            Toast.makeText(this, "Please correct the errors to save.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Saving profile...", Toast.LENGTH_SHORT).show();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            DocumentReference userRef = db.collection("users").document(userId);

            Map<String, Object> userData = new HashMap<>();
            userData.put("username", newUsername);
            userData.put("address", newAddress);
            userData.put("mobile", newMobile);

            // Do not update 'author' status here as it's typically an admin-set permission.
            // If you want users to be able to request author status, that's a different flow.

            if (!newUsername.equals(currentUser.getDisplayName())) {
                currentUser.updateProfile(new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                .setDisplayName(newUsername)
                                .build())
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Auth Profile display name updated.");
                            } else {
                                Log.e(TAG, "Failed to update Auth Profile display name.", task.getException());
                            }
                        });
            }

            userRef.update(userData)
                    .addOnSuccessListener(aVoid -> {
                        currentUsername = newUsername;
                        currentAddress = newAddress;
                        currentMobile = newMobile;

                        if (selectedImageUri != null) {
                            uploadProfilePicture(userId);
                        } else {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(UserInfo.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                            toggleEditMode();
                            updateAuthorButtonVisibility(); // Re-evaluate visibility after saving
                        }
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(UserInfo.this, "Error updating profile data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }

    /**
     * Opens an image chooser intent to select a profile picture.
     */
    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    /**
     * Uploads the selected profile picture to Firebase Storage.
     * Uses manual Bitmap scaling and compression.
     */
    private void uploadProfilePicture(String userId) {
        if (selectedImageUri == null) {
            Toast.makeText(this, "No image selected for upload.", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            toggleEditMode();
            updateAuthorButtonVisibility(); // Re-evaluate visibility if upload is skipped
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();

        executorService.execute(() -> {
            try {
                // 1. Load the image as a Bitmap
                Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);

                // 2. Scale the Bitmap (e.g., to max 400x400, maintaining aspect ratio)
                int originalWidth = originalBitmap.getWidth();
                int originalHeight = originalBitmap.getHeight();
                int newWidth = 400;
                int newHeight = 400;

                if (originalWidth > newWidth || originalHeight > newHeight) {
                    float ratio = Math.min((float) newWidth / originalWidth, (float) newHeight / originalHeight);
                    newWidth = Math.round(ratio * originalWidth);
                    newHeight = Math.round(ratio * originalHeight);
                    originalBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);
                }

                // 3. Compress the Bitmap into a ByteArrayOutputStream
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                originalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos); // Compress with 80% quality
                byte[] data = baos.toByteArray(); // Get the compressed byte array

                // 4. Upload the compressed byte array to Firebase Storage
                StorageReference profileImageRef = storageRef.child("profile_pictures/" + userId + ".jpg");

                profileImageRef.putBytes(data) // Use putBytes instead of putFile
                        .addOnSuccessListener(taskSnapshot -> {
                            profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                currentProfilePictureUrl = uri.toString();
                                Map<String, Object> profileUpdate = new HashMap<>();
                                profileUpdate.put("profilePictureUrl", currentProfilePictureUrl);

                                db.collection("users").document(userId).update(profileUpdate)
                                        .addOnSuccessListener(aVoid -> handler.post(() -> {
                                            Toast.makeText(UserInfo.this, "Profile picture uploaded and URL updated!", Toast.LENGTH_SHORT).show();
                                            selectedImageUri = null; // Clear selected image
                                            progressBar.setVisibility(View.GONE);
                                            toggleEditMode(); // Switch back to view mode
                                            updateAuthorButtonVisibility(); // Re-evaluate visibility after upload
                                        }))
                                        .addOnFailureListener(e -> handler.post(() -> {
                                            Toast.makeText(UserInfo.this, "Failed to update profile picture URL in Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            progressBar.setVisibility(View.GONE);
                                            toggleEditMode();
                                            updateAuthorButtonVisibility(); // Re-evaluate visibility after error
                                        }));
                            });
                        })
                        .addOnFailureListener(e -> handler.post(() -> {
                            Toast.makeText(UserInfo.this, "Failed to upload profile picture: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            progressBar.setVisibility(View.GONE);
                            toggleEditMode();
                            updateAuthorButtonVisibility(); // Re-evaluate visibility after error
                        }));

            } catch (IOException e) {
                Log.e(TAG, "Error processing image: " + e.getMessage(), e);
                handler.post(() -> {
                    Toast.makeText(UserInfo.this, "Error processing image: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    progressBar.setVisibility(View.GONE);
                    toggleEditMode();
                    updateAuthorButtonVisibility(); // Re-evaluate visibility after error
                });
            }
        });
    }


    /**
     * Handles user sign out.
     */
    private void signOutUser() {
        if (currentUser != null) {
            mAuth.signOut();
            Toast.makeText(this, "Signed out successfully.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(UserInfo.this, SignIn.class); // Assuming SignIn is your login activity
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Clear back stack
            startActivity(intent);
            finish(); // Finish current activity
        } else {
            Toast.makeText(this, "No user to sign out.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (isEditMode) {
            Toast.makeText(this, "Changes discarded.", Toast.LENGTH_SHORT).show();
            isEditMode = false;
            displayUserData(false);
            selectedImageUri = null;
            updateAuthorButtonVisibility(); // Re-evaluate visibility after cancelling edit via back button
        } else {
            Intent intent = new Intent(UserInfo.this, home.class); // Assuming 'home.class' is your main activity
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }
}