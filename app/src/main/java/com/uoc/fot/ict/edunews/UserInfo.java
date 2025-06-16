package com.uoc.fot.ict.edunews;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory; // NEW IMPORT: For BitmapFactory
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.graphics.ImageDecoder;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;


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
import java.io.InputStream; // NEW IMPORT: For InputStream
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
    private TextInputLayout usernameInputLayout, addressInputLayout, mobileInputLayout;
    private Button editSaveButton;
    private Button mainActionButton;
    private ImageButton editProfilePictureButton;
    private CircleImageView profilePicture;
    private ProgressBar progressBar;
    private LinearLayout authorButtonsLayout;

    // Firebase Instances
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private FirebaseUser currentUser;

    // Local User Data
    private String currentUsername;
    private String currentAddress;
    private String currentMobile;
    private String currentEmail;
    private String currentProfilePictureUrl;
    private boolean isAuthor = false;
    private Uri selectedImageUri;

    private boolean isEditMode = false;

    // Activity Result Launcher for picking images
    private ActivityResultLauncher<Intent> pickImageLauncher;

    // For background processing
    private ExecutorService executorService;
    private Handler handler;

    // OnBackPressedCallback for modern back press handling
    private OnBackPressedCallback onBackPressedCallback;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseStorage storage = FirebaseStorage.getInstance();
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

        editSaveButton = findViewById(R.id.editSaveButton);
        Button dev_Info = findViewById(R.id.devInfo);
        mainActionButton = findViewById(R.id.mainActionButton);
        Button writeNewPostButton = findViewById(R.id.WriteNewPost);
        Button myPostsButton = findViewById(R.id.MyPosts);
        authorButtonsLayout = findViewById(R.id.authorButtonsLayout);

        ImageButton backButton = findViewById(R.id.backButton);
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
                            // Use Glide to immediately display the selected image
                            Glide.with(this).load(selectedImageUri).into(profilePicture);
                            Toast.makeText(this, "Image selected, click 'Save' to upload.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Image selection cancelled.", Toast.LENGTH_SHORT).show();
                        selectedImageUri = null; // Clear selected URI if cancelled
                    }
                }
        );

        // Initialize and enable OnBackPressedCallback
        onBackPressedCallback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                Log.d(TAG, "Back button pressed, handling via dispatcher.");
                if (isEditMode) {
                    Toast.makeText(UserInfo.this, "Discarding changes and exiting edit mode.", Toast.LENGTH_SHORT).show();
                    isEditMode = false;
                    displayUserData(false);
                    selectedImageUri = null;
                } else {
                    Intent intent = new Intent(UserInfo.this, home.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);

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
            } else {
                signOutUser();
            }
        });

        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        editProfilePictureButton.setOnClickListener(v -> {
            if (isEditMode) {
                openImageChooser();
            }
        });

        writeNewPostButton.setOnClickListener(v -> {
            Toast.makeText(UserInfo.this, "Opening new post activity...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(UserInfo.this, CreatePost.class);
            startActivity(intent);
        });

        myPostsButton.setOnClickListener(v -> {
            Toast.makeText(UserInfo.this, "Opening your posts...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(UserInfo.this, MyPosts.class);
            startActivity(intent);
        });

        dev_Info.setOnClickListener(v -> {
            Toast.makeText(UserInfo.this, "Opening Developer Informations...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(UserInfo.this, DevInfo.class);
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
                            currentMobile = document.getString("mobileNumber");
                            currentEmail = currentUser.getEmail();
                            currentProfilePictureUrl = document.getString("profilePictureUrl");
                            Boolean authorStatus = document.getBoolean("author");
                            isAuthor = (authorStatus != null && authorStatus);

                            Log.d(TAG, "User data fetched: " + currentUsername + ", isAuthor: " + isAuthor);
                            displayUserData(false);
                        } else {
                            Log.d(TAG, "User data document does not exist, creating default.");
                            currentEmail = currentUser.getEmail();
                            currentUsername = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "";
                            currentAddress = "";
                            currentMobile = "";
                            currentProfilePictureUrl = "";
                            isAuthor = false;

                            Map<String, Object> defaultUserData = new HashMap<>();
                            defaultUserData.put("username", currentUsername);
                            defaultUserData.put("email", currentEmail);
                            defaultUserData.put("address", currentAddress);
                            defaultUserData.put("mobileNumber", currentMobile);
                            defaultUserData.put("profilePictureUrl", currentProfilePictureUrl);
                            defaultUserData.put("author", isAuthor);

                            db.collection("users").document(userId).set(defaultUserData)
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Default user data created in Firestore."))
                                    .addOnFailureListener(e -> Log.e(TAG, "Error creating default user data", e));

                            displayUserData(false);
                        }
                    } else {
                        Log.e(TAG, "Failed to load user data from Firestore: ", task.getException());
                        Toast.makeText(UserInfo.this, "Failed to load user data: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                        currentEmail = currentUser.getEmail();
                        displayUserData(false);
                    }
                }
            });
        } else {
            Log.d(TAG, "No current user, redirecting to SignIn.");
            Toast.makeText(this, "User not logged in. Please sign in.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(UserInfo.this, SignIn.class);
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

            // Hide author-specific buttons when in edit mode
            authorButtonsLayout.setVisibility(View.GONE);

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

            // Show author-specific buttons based on isAuthor flag when in view mode
            updateAuthorButtonVisibility();
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
        selectedImageUri = null; // Clear selected image URI when toggling mode
    }

    /**
     * Updates the visibility of the "Write A New Post" and "My Posts" buttons
     * based on the isAuthor flag and current edit mode.
     */
    private void updateAuthorButtonVisibility() {
        if (!isEditMode && isAuthor) {
            authorButtonsLayout.setVisibility(View.VISIBLE);
        } else {
            authorButtonsLayout.setVisibility(View.GONE);
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

        executorService.execute(() -> {
            try {
                // Handle profile picture upload first if a new one is selected
                if (selectedImageUri != null) {
                    Bitmap bitmap;
                    // Check Android version to use appropriate method for getting bitmap
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), selectedImageUri));
                    } else {
                        // For older Android versions, use a more robust way than deprecated getBitmap
                        // This involves opening an InputStream and decoding it
                        InputStream imageStream = getContentResolver().openInputStream(selectedImageUri);
                        bitmap = BitmapFactory.decodeStream(imageStream);
                        if (imageStream != null) {
                            imageStream.close(); // Close the stream
                        }
                    }

                    if (bitmap == null) {
                        handler.post(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(UserInfo.this, "Failed to decode image.", Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Bitmap is null after decoding image URI.");
                        });
                        return; // Exit if bitmap is null
                    }

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos);
                    byte[] data = baos.toByteArray();

                    StorageReference profilePicsRef = storageRef.child("profile_pictures/" + currentUser.getUid() + ".jpg");

                    profilePicsRef.putBytes(data)
                            .addOnSuccessListener(taskSnapshot -> profilePicsRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                currentProfilePictureUrl = uri.toString();
                                updateFirestoreUserData(newUsername, newAddress, newMobile, currentProfilePictureUrl);
                            }).addOnFailureListener(e -> {
                                handler.post(() -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(UserInfo.this, "Failed to get new profile picture URL: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                                    Log.e(TAG, "Error getting download URL", e);
                                });
                            }))
                            .addOnFailureListener(e -> {
                                handler.post(() -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(UserInfo.this, "Failed to upload profile picture: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                                    Log.e(TAG, "Error uploading profile picture", e);
                                });
                            });
                } else {
                    // If no new image selected, just update Firestore data
                    updateFirestoreUserData(newUsername, newAddress, newMobile, currentProfilePictureUrl);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error processing image for upload", e);
                handler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(UserInfo.this, "Error processing image: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void updateFirestoreUserData(String newUsername, String newAddress, String newMobile, String profilePictureUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("username", newUsername);
        updates.put("address", newAddress);
        updates.put("mobileNumber", newMobile); // Ensure this matches Firestore field name
        if (profilePictureUrl != null) {
            updates.put("profilePictureUrl", profilePictureUrl);
        }

        db.collection("users").document(currentUser.getUid()).update(updates)
                .addOnSuccessListener(aVoid -> {
                    currentUsername = newUsername;
                    currentAddress = newAddress;
                    currentMobile = newMobile;
                    currentProfilePictureUrl = profilePictureUrl;
                    handler.post(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(UserInfo.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                        isEditMode = false;
                        displayUserData(false); // Switch back to view mode
                        selectedImageUri = null; // Clear selected image
                    });
                })
                .addOnFailureListener(e -> {
                    handler.post(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(UserInfo.this, "Error updating profile: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error updating document", e);
                    });
                });
    }


    /**
     * Opens image chooser for selecting profile picture.
     */
    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    /**
     * Signs out the current user and navigates to the sign-in screen.
     */
    private void signOutUser() {
        mAuth.signOut();
        Toast.makeText(this, "Signed out successfully.", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(UserInfo.this, SignIn.class); // Assuming SignIn is your login activity
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Clear back stack
        startActivity(intent);
        finish(); // Finish current activity
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdownNow(); // Shut down the executor service
        }
    }
}