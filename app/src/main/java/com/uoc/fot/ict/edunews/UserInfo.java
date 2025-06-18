package com.uoc.fot.ict.edunews;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.ImageDecoder; // Required for decoding image on newer Android versions

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback; // For modern back press handling

import com.bumptech.glide.Glide; // Image loading library
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks; // For Tasks.whenAllComplete
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException; // Potentially useful for reauthentication
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot; // For iterating query results
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream; // For compressing bitmap to byte array
import java.io.IOException; // For handling IO exceptions
import java.io.InputStream; // For reading image data
import java.util.ArrayList; // For list of tasks
import java.util.HashMap; // For Firestore data maps
import java.util.List; // For list of tasks
import java.util.Map; // For Firestore data maps
import java.util.Objects; // For Objects.requireNonNull
import java.util.concurrent.ExecutorService; // For background threading
import java.util.concurrent.Executors; // For creating ExecutorService
import java.util.regex.Pattern; // For mobile number validation

import de.hdodenhof.circleimageview.CircleImageView; // Custom Circular ImageView

/**
 * UserInfo Activity displays and allows editing of user profile information.
 * It integrates with Firebase Authentication, Firestore for user data,
 * and Firebase Storage for profile pictures. It also manages author-specific
 * functionalities like post creation and viewing.
 */
public class UserInfo extends AppCompatActivity {

    private static final String TAG = "UserInfoActivity";

    // UI Elements
    private TextInputEditText usernameInput, addressInput, mobileInput, emailInput;
    private TextInputLayout usernameInputLayout, addressInputLayout, mobileInputLayout;
    private Button editSaveButton, mainActionButton, deleteAccountButton;
    private ImageButton editProfilePictureButton;
    private CircleImageView profilePicture;
    private ProgressBar progressBar;
    private LinearLayout authorButtonsLayout;

    // Firebase Instances
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private FirebaseUser currentUser;

    // Local User Data (original values for comparison)
    private String originalUsername;
    private String originalAddress;
    private String originalMobile;
    private String originalProfilePictureUrl; // Stores the URL initially loaded from Firestore
    private boolean isAuthor = false; // User's author status

    // Current data, potentially modified by user in edit mode
    private String currentUsername;
    private String currentAddress;
    private String currentMobile;
    private String currentEmail; // Email is from FirebaseAuth, not editable via input
    private String currentProfilePictureUrl; // Tracks the URL that is currently displayed/saved

    private Uri selectedImageUri; // URI of a newly selected image (not yet uploaded)

    private boolean isEditMode = false; // State flag for UI mode

    // Activity Result Launcher for picking images (modern replacement for startActivityForResult)
    private ActivityResultLauncher<Intent> pickImageLauncher;

    // For background processing to prevent UI freezes
    private ExecutorService executorService;
    private Handler handler; // For posting UI updates back to the main thread

    // OnBackPressedCallback for modern back press handling
    private OnBackPressedCallback onBackPressedCallback;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        initializeFirebase();
        initializeThreading();
        initializeViews();
        setupImagePickerLauncher();
        setupOnBackPressedCallback();
        fetchUserData(); // Initiates data loading and UI population
        setupListeners();
    }

    /**
     * Initializes Firebase authentication, Firestore, and Storage instances.
     */
    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        currentUser = mAuth.getCurrentUser();
    }

    /**
     * Initializes ExecutorService for background tasks and Handler for UI updates.
     */
    private void initializeThreading() {
        executorService = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());
    }

    /**
     * Initializes all UI elements by finding them from the layout file.
     */
    private void initializeViews() {
        usernameInput = findViewById(R.id.UsernameInput);
        addressInput = findViewById(R.id.AddressInput);
        mobileInput = findViewById(R.id.MobileInput);
        emailInput = findViewById(R.id.EmailInput);

        usernameInputLayout = findViewById(R.id.usernameInputLayout);
        addressInputLayout = findViewById(R.id.addressInputLayout);
        mobileInputLayout = findViewById(R.id.mobileInputLayout);

        editSaveButton = findViewById(R.id.editSaveButton);
        Button dev_Info = findViewById(R.id.devInfo); // Developer info button
        mainActionButton = findViewById(R.id.mainActionButton); // Sign Out / Cancel button
        deleteAccountButton = findViewById(R.id.deleteAccountButton);

        Button writeNewPostButton = findViewById(R.id.WriteNewPost); // Author's "Write New Post" button
        Button myPostsButton = findViewById(R.id.MyPosts); // Author's "My Posts" button
        authorButtonsLayout = findViewById(R.id.authorButtonsLayout); // Layout containing author buttons

        ImageButton backButton = findViewById(R.id.backButton); // Back navigation button
        profilePicture = findViewById(R.id.profilePicture); // User's profile picture ImageView
        editProfilePictureButton = findViewById(R.id.editProfilePictureButton); // Button to change profile picture
        progressBar = findViewById(R.id.progressBar); // Progress indicator
    }

    /**
     * Sets up the ActivityResultLauncher for selecting images from the gallery.
     */
    private void setupImagePickerLauncher() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData(); // Get the URI of the selected image
                        if (selectedImageUri != null) {
                            // Display the newly selected image immediately
                            Glide.with(this).load(selectedImageUri).into(profilePicture);
                            Toast.makeText(this, "Image selected, click 'Save' to upload.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Image selection cancelled.", Toast.LENGTH_SHORT).show();
                        selectedImageUri = null; // Clear URI if selection was cancelled
                    }
                }
        );
    }

    /**
     * Configures the onBackPressedCallback to handle back button presses.
     * In edit mode, it discards changes and reverts to view mode.
     * In view mode, it navigates to the home screen.
     */
    private void setupOnBackPressedCallback() {
        onBackPressedCallback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                Log.d(TAG, "Back button pressed, handling via dispatcher.");
                if (isEditMode) {
                    // If in edit mode, discard changes and revert to view mode
                    Toast.makeText(UserInfo.this, "Discarding changes and exiting edit mode.", Toast.LENGTH_SHORT).show();
                    isEditMode = false;
                    displayUserData(false); // Revert UI to show original data
                    selectedImageUri = null; // Clear any pending image selection
                } else {
                    // If in view mode, navigate to home and clear back stack
                    Intent intent = new Intent(UserInfo.this, home.class);
                    // Flags to clear existing activities and bring home to top
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish(); // Finish current UserInfo activity to prevent returning to it
                }
            }
        };
        // Add the callback to the dispatcher. 'this' refers to the lifecycle owner.
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    /**
     * Sets up click listeners for various UI elements.
     */
    private void setupListeners() {
        // Edit/Save button click listener
        editSaveButton.setOnClickListener(v -> {
            if (isEditMode) {
                saveUserData(); // If in edit mode, attempt to save
            } else {
                toggleEditMode(); // If in view mode, switch to edit mode
            }
        });

        // Main action button (Sign Out / Cancel) click listener
        mainActionButton.setOnClickListener(v -> {
            if (isEditMode) {
                // If in edit mode, cancel changes and revert
                Toast.makeText(this, "Changes discarded.", Toast.LENGTH_SHORT).show();
                isEditMode = false;
                displayUserData(false); // Display original data
                selectedImageUri = null; // Clear pending image selection
            } else {
                signOutUser(); // If in view mode, sign out
            }
        });

        // Back button in the top bar click listener
        findViewById(R.id.backButton).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Edit profile picture button click listener
        editProfilePictureButton.setOnClickListener(v -> {
            if (isEditMode) {
                openImageChooser(); // Only allow image selection in edit mode
            }
        });

        // Delete account button click listener
        deleteAccountButton.setOnClickListener(v -> {
            if (isEditMode) { // Typically delete account is only available in edit mode or a specific settings screen
                confirmDeleteAccount();
            }
        });

        // Author's "Write New Post" button listener
        findViewById(R.id.WriteNewPost).setOnClickListener(v -> {
            Toast.makeText(UserInfo.this, "Opening new post activity...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(UserInfo.this, CreatePost.class);
            startActivity(intent);
        });

        // Author's "My Posts" button listener
        findViewById(R.id.MyPosts).setOnClickListener(v -> {
            Toast.makeText(UserInfo.this, "Opening your posts...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(UserInfo.this, MyPosts.class);
            startActivity(intent);
        });

        // Developer info button listener
        findViewById(R.id.devInfo).setOnClickListener(v -> {
            Toast.makeText(UserInfo.this, "Opening Developer Informations...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(UserInfo.this, DevInfo.class);
            startActivity(intent);
        });
    }

    /**
     * Fetches user data from Firebase Firestore and updates UI.
     * If user data doesn't exist, it creates a default document.
     * If no current user, it redirects to the sign-in screen.
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
                            // Store original data to detect changes later
                            originalUsername = document.getString("username");
                            originalAddress = document.getString("address");
                            originalMobile = document.getString("mobileNumber");
                            originalProfilePictureUrl = document.getString("profilePictureUrl");

                            // Set current data for display
                            currentUsername = originalUsername;
                            currentAddress = originalAddress;
                            currentMobile = originalMobile;
                            currentEmail = currentUser.getEmail(); // Email from FirebaseAuth
                            currentProfilePictureUrl = originalProfilePictureUrl; // Currently displayed URL
                            Boolean authorStatus = document.getBoolean("author");
                            isAuthor = (authorStatus != null && authorStatus);

                            Log.d(TAG, "User data fetched: " + currentUsername + ", isAuthor: " + isAuthor);
                            displayUserData(false); // Display in view mode
                        } else {
                            // If user document doesn't exist, create a default one
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
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Default user data created in Firestore.");
                                        // Update original values after creating default
                                        originalUsername = currentUsername;
                                        originalAddress = currentAddress;
                                        originalMobile = currentMobile;
                                        originalProfilePictureUrl = currentProfilePictureUrl;
                                        displayUserData(false); // Display in view mode
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error creating default user data", e);
                                        Toast.makeText(UserInfo.this, "Error creating user data: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                                        displayUserData(false); // Still attempt to display what we have
                                    });
                        }
                    } else {
                        // Handle failure to fetch user data
                        Log.e(TAG, "Failed to load user data from Firestore: ", task.getException());
                        Toast.makeText(UserInfo.this, "Failed to load user data: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                        currentEmail = currentUser.getEmail(); // Still display email if available from Auth
                        displayUserData(false);
                    }
                }
            });
        } else {
            // If no user is logged in, redirect to sign-in
            Log.d(TAG, "No current user, redirecting to SignIn.");
            Toast.makeText(this, "User not logged in. Please sign in.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(UserInfo.this, SignIn.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Finish current activity to prevent returning
        }
    }


    /**
     * Updates the UI with user data and sets editability of input fields.
     * Also manages button visibility and text based on mode.
     * @param editable True to enable edit mode, false for view mode.
     */
    private void displayUserData(boolean editable) {
        usernameInput.setText(currentUsername);
        addressInput.setText(currentAddress);
        mobileInput.setText(currentMobile);
        emailInput.setText(currentEmail); // Email is always displayed but not editable

        // Load profile picture using Glide
        if (currentProfilePictureUrl != null && !currentProfilePictureUrl.isEmpty()) {
            Glide.with(this)
                    .load(currentProfilePictureUrl)
                    .placeholder(R.drawable.user) // Placeholder while loading
                    .error(R.drawable.user) // Fallback on error
                    .into(profilePicture);
        } else {
            profilePicture.setImageResource(R.drawable.user); // Default icon if no URL
        }

        // Set editability for relevant fields
        setFieldEditable(usernameInput, editable);
        setFieldEditable(addressInput, editable);
        setFieldEditable(mobileInput, editable);

        // Email is always non-editable
        emailInput.setEnabled(false);
        emailInput.setFocusable(false);
        emailInput.setFocusableInTouchMode(false);
        emailInput.setLongClickable(false);
        emailInput.setCursorVisible(false);

        // UI adjustments based on mode
        if (editable) {
            editSaveButton.setText("Save");
            editSaveButton.setVisibility(View.VISIBLE);
            editSaveButton.setTextColor(getResources().getColor(R.color.white, getTheme()));
            editSaveButton.setBackgroundTintList(getResources().getColorStateList(R.color.ButtonColour, getTheme()));

            mainActionButton.setText("Cancel");
            mainActionButton.setBackgroundTintList(getResources().getColorStateList(R.color.RedColour, getTheme()));

            deleteAccountButton.setVisibility(View.VISIBLE);
            deleteAccountButton.setTextColor(getResources().getColor(R.color.white, getTheme()));
            deleteAccountButton.setBackgroundTintList(getResources().getColorStateList(R.color.RedColour, getTheme()));

            editProfilePictureButton.setVisibility(View.VISIBLE); // Allow changing profile picture in edit mode

            // Set text color for editable fields to black
            usernameInput.setTextColor(getResources().getColor(R.color.black, getTheme()));
            addressInput.setTextColor(getResources().getColor(R.color.black, getTheme()));
            mobileInput.setTextColor(getResources().getColor(R.color.black, getTheme()));
            emailInput.setTextColor(getResources().getColor(R.color.black, getTheme())); // Email color also black

            authorButtonsLayout.setVisibility(View.GONE); // Hide author buttons in edit mode

        } else { // View mode
            editSaveButton.setText("Edit");
            editSaveButton.setVisibility(View.VISIBLE);
            editSaveButton.setTextColor(getResources().getColor(R.color.white, getTheme()));
            editSaveButton.setBackgroundTintList(getResources().getColorStateList(R.color.ButtonColour, getTheme()));

            mainActionButton.setText("Sign Out");
            mainActionButton.setBackgroundTintList(getResources().getColorStateList(R.color.ButtonColour, getTheme()));

            deleteAccountButton.setVisibility(View.GONE); // Hide delete account in view mode

            editProfilePictureButton.setVisibility(View.GONE); // Hide edit profile picture button

            // Set text color for fields to a lighter shade (greyish black) for non-editable appearance
            usernameInput.setTextColor(Color.parseColor("#80000000")); // Hex code for 50% black
            addressInput.setTextColor(Color.parseColor("#80000000"));
            mobileInput.setTextColor(Color.parseColor("#80000000"));
            emailInput.setTextColor(Color.parseColor("#80000000"));

            updateAuthorButtonVisibility(); // Show/hide author buttons based on status
        }
    }

    /**
     * Helper method to set editable state of a TextInputEditText field, including focusability and cursor.
     * @param field The TextInputEditText to modify.
     * @param editable True to enable editing, false to disable.
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
     * Resets any pending image selection when entering edit mode.
     */
    private void toggleEditMode() {
        isEditMode = !isEditMode;
        displayUserData(isEditMode);
        selectedImageUri = null; // Clear selected image URI when toggling mode
    }

    /**
     * Updates the visibility of the "Write A New Post" and "My Posts" buttons
     * based on the user's author status and the current UI mode (only visible in view mode if author).
     */
    private void updateAuthorButtonVisibility() {
        if (!isEditMode && isAuthor) {
            authorButtonsLayout.setVisibility(View.VISIBLE);
        } else {
            authorButtonsLayout.setVisibility(View.GONE);
        }
    }

    /**
     * Saves user data after validation. This function first checks if any changes
     * have been made. If no changes, it shows a Toast. Otherwise, it proceeds to
     * upload image (if new) and update Firestore data.
     */
    private void saveUserData() {
        // Clear previous error messages from input layouts
        usernameInputLayout.setError(null);
        addressInputLayout.setError(null);
        mobileInputLayout.setError(null);

        // Get current values from input fields
        String newUsername = Objects.requireNonNull(usernameInput.getText()).toString().trim();
        String newAddress = Objects.requireNonNull(addressInput.getText()).toString().trim();
        String newMobile = Objects.requireNonNull(mobileInput.getText()).toString().trim();

        boolean cancel = false; // Flag to indicate if validation failed

        // Input validation checks
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
        } else if (!Pattern.matches("\\d{10}", newMobile)) { // Regex for 10-digit number
            mobileInputLayout.setError("Please enter a valid 10-digit mobile number.");
            cancel = true;
        }

        if (cancel) {
            Toast.makeText(this, "Please correct the errors to save.", Toast.LENGTH_SHORT).show();
            return; // Exit if validation failed
        }

        // --- NEW LOGIC: Check for changes before proceeding ---
        boolean profileDataChanged = !newUsername.equals(originalUsername) ||
                !newAddress.equals(originalAddress) ||
                !newMobile.equals(originalMobile);

        boolean profilePictureChanged = (selectedImageUri != null) || // New image selected
                (!TextUtils.isEmpty(originalProfilePictureUrl) && TextUtils.isEmpty(currentProfilePictureUrl)); // Existing image removed (not implemented here but good to check)

        if (!profileDataChanged && !profilePictureChanged) {
            // If no changes detected, show Toast and revert to view mode
            Toast.makeText(this, "No changes to save.", Toast.LENGTH_SHORT).show();
            isEditMode = false;
            displayUserData(false); // Go back to uneditable mode
            selectedImageUri = null; // Clear pending image selection
            return; // Exit the function
        }
        // --- END NEW LOGIC ---

        // Show progress and disable button while saving
        progressBar.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Saving profile...", Toast.LENGTH_SHORT).show();

        // Execute image upload and Firestore update on a background thread
        executorService.execute(() -> {
            try {
                if (selectedImageUri != null) { // If a new image is selected, upload it first
                    Bitmap bitmap;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), selectedImageUri));
                    } else {
                        InputStream imageStream = getContentResolver().openInputStream(selectedImageUri);
                        bitmap = BitmapFactory.decodeStream(imageStream);
                        if (imageStream != null) {
                            imageStream.close(); // Close the stream to prevent resource leaks
                        }
                    }

                    if (bitmap == null) {
                        handler.post(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(UserInfo.this, "Failed to decode image.", Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Bitmap is null after decoding image URI.");
                        });
                        return;
                    }

                    // Compress the bitmap to JPEG format
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos); // 75% quality
                    byte[] data = baos.toByteArray();

                    // Define Storage reference path for profile picture (unique to user)
                    StorageReference profilePicsRef = storageRef.child("profile_pictures/" + currentUser.getUid() + ".jpg");

                    // Upload image bytes to Firebase Storage
                    profilePicsRef.putBytes(data)
                            .addOnSuccessListener(taskSnapshot ->
                                    // Get download URL after successful upload
                                    profilePicsRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                        currentProfilePictureUrl = uri.toString(); // Update current URL
                                        // Proceed to update Firestore with new URL
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
                    // No new image selected, just update Firestore with existing or unchanged URL
                    updateFirestoreUserData(newUsername, newAddress, newMobile, currentProfilePictureUrl);
                }
            } catch (IOException e) {
                // Handle IO exceptions during image processing
                Log.e(TAG, "Error processing image for upload", e);
                handler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(UserInfo.this, "Error processing image: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Updates the user's data in Firebase Firestore. This is called after
     * successful image upload (if any) or directly if no image changes.
     *
     * @param newUsername The new username to save.
     * @param newAddress The new address to save.
     * @param newMobile The new mobile number to save.
     * @param profilePictureUrl The URL of the profile picture (newly uploaded or existing).
     */
    private void updateFirestoreUserData(String newUsername, String newAddress, String newMobile, String profilePictureUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("username", newUsername);
        updates.put("address", newAddress);
        updates.put("mobileNumber", newMobile);
        if (profilePictureUrl != null) { // Only update if a URL is provided
            updates.put("profilePictureUrl", profilePictureUrl);
        }

        db.collection("users").document(currentUser.getUid()).update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Update local 'original' data and 'current' data upon successful Firestore update
                    originalUsername = newUsername;
                    originalAddress = newAddress;
                    originalMobile = newMobile;
                    originalProfilePictureUrl = profilePictureUrl;
                    currentUsername = newUsername;
                    currentAddress = newAddress;
                    currentMobile = newMobile;
                    currentProfilePictureUrl = profilePictureUrl;

                    handler.post(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(UserInfo.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                        isEditMode = false; // Exit edit mode
                        displayUserData(false); // Update UI to view mode
                        selectedImageUri = null; // Clear pending image selection
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
     * Opens image chooser for selecting profile picture using the ActivityResultLauncher.
     */
    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    /**
     * Signs out the current user from Firebase and navigates to the sign-in screen.
     */
    private void signOutUser() {
        mAuth.signOut();
        Toast.makeText(this, "Signed out successfully.", Toast.LENGTH_SHORT).show();
        // Use FLAG_ACTIVITY_NEW_TASK and FLAG_ACTIVITY_CLEAR_TASK to clear all other activities
        // and ensure the user cannot navigate back to authenticated screens.
        signOutAndNavigateToSignIn();
    }

    /**
     * Helper method to sign out and navigate to sign-in activity, clearing activity stack.
     */
    private void signOutAndNavigateToSignIn() {
        Intent intent = new Intent(UserInfo.this, SignIn.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Finish current activity
    }

    /**
     * Displays an AlertDialog to confirm account deletion and offers an option
     * to delete associated posts if the user is an author.
     */
    private void confirmDeleteAccount() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Account");
        builder.setMessage("Are you sure you want to delete your account?\nThis action is irreversible.");

        // Inflate custom dialog layout for checkbox
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_delete_account, null);
        CheckBox deletePostsCheckbox = dialogView.findViewById(R.id.deletePostsCheckbox);
        LinearLayout deletePostsOptionLayout = dialogView.findViewById(R.id.deletePostsOptionLayout);

        // Show delete posts option only if the user is an author
        if (isAuthor) {
            deletePostsOptionLayout.setVisibility(View.VISIBLE);
        } else {
            deletePostsOptionLayout.setVisibility(View.GONE);
        }

        builder.setView(dialogView); // Set the custom view to the dialog

        builder.setPositiveButton("Delete", (dialog, which) -> {
            boolean deletePosts = isAuthor && deletePostsCheckbox.isChecked();
            executeAccountDeletion(deletePosts); // Proceed with deletion
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss()); // Dismiss dialog on cancel
        builder.show();
    }

    /**
     * Orchestrates the multi-step account deletion process.
     * Steps include: deleting profile picture, user's posts & images (if applicable),
     * user document from Firestore, and finally the Firebase Authentication account.
     * @param deletePosts True if author's posts should also be deleted.
     */
    private void executeAccountDeletion(boolean deletePosts) {
        if (currentUser == null) {
            Toast.makeText(this, "No user logged in to delete.", Toast.LENGTH_SHORT).show();
            signOutAndNavigateToSignIn(); // Ensure proper navigation
            return;
        }

        // Show progress and disable buttons to prevent re-clicks
        progressBar.setVisibility(View.VISIBLE);
        editSaveButton.setEnabled(false);
        mainActionButton.setEnabled(false);
        if (deleteAccountButton != null) deleteAccountButton.setEnabled(false);

        Toast.makeText(this, "Deleting account...", Toast.LENGTH_LONG).show();

        // Start deletion process on a background thread
        executorService.execute(() -> {
            // Chain deletion tasks sequentially using callbacks
            deleteProfilePicture(() -> {
                deleteUserPostsAndImages(deletePosts, () -> {
                    deleteUserDocument(() -> {
                        deleteFirebaseAuthAccount();
                    });
                });
            });
        });
    }

    /**
     * Handles failures during any step of the deletion process.
     * Re-enables buttons and shows a Toast message.
     * @param message User-friendly error message.
     * @param e The exception that occurred (can be null).
     */
    private void handleDeletionFailure(String message, Exception e) {
        handler.post(() -> {
            progressBar.setVisibility(View.GONE);
            editSaveButton.setEnabled(true);
            mainActionButton.setEnabled(true);
            if (deleteAccountButton != null) deleteAccountButton.setEnabled(true);
            Toast.makeText(UserInfo.this, message + (e != null ? ": " + e.getMessage() : ""), Toast.LENGTH_LONG).show();
            if (e != null) Log.e(TAG, message, e);
        });
    }

    /**
     * Step 1 in deletion process: Deletes the user's profile picture from Firebase Storage.
     * Continues to the next step via `onComplete` runnable regardless of success/failure.
     * @param onComplete Callback to run after profile picture deletion attempt.
     */
    private void deleteProfilePicture(Runnable onComplete) {
        // Check if there's a profile picture URL and it's a Firebase Storage URL
        if (currentProfilePictureUrl != null && !currentProfilePictureUrl.isEmpty() && currentProfilePictureUrl.contains("firebasestorage.googleapis.com")) {
            try {
                StorageReference profilePicRef = FirebaseStorage.getInstance().getReferenceFromUrl(currentProfilePictureUrl);
                profilePicRef.delete()
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Profile picture deleted successfully.");
                            onComplete.run(); // Proceed
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to delete profile picture (might not exist or permissions issue): " + e.getMessage());
                            onComplete.run(); // Proceed even if deletion fails (e.g., file not found)
                        });
            } catch (IllegalArgumentException e) {
                // If URL is not a valid Firebase Storage URL (e.g., empty or malformed)
                Log.w(TAG, "Profile picture URL is not a valid Firebase Storage URL. Skipping deletion. Error: " + e.getMessage());
                onComplete.run(); // Proceed
            }
        } else {
            // No profile picture or not a Firebase Storage URL, proceed immediately
            Log.d(TAG, "No profile picture to delete or URL not from Firebase Storage.");
            onComplete.run();
        }
    }

    /**
     * Step 2 in deletion process: Deletes user's posts from Firestore and their associated images from Storage.
     * This is conditional based on `isAuthor` and `deletePosts` flag.
     * Uses `Tasks.whenAllComplete` to wait for all deletions.
     * @param deletePosts True if posts should be deleted.
     * @param onComplete Callback to run after all post/image deletion attempts.
     */
    private void deleteUserPostsAndImages(boolean deletePosts, Runnable onComplete) {
        if (isAuthor && deletePosts && currentUser != null) {
            db.collection("posts")
                    .whereEqualTo("userId", currentUser.getUid()) // Query for posts by current user
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            List<Task<Void>> deletionTasks = new ArrayList<>(); // List to hold all deletion tasks
                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                String imageUrl = doc.getString("imageUrl");
                                // Delete associated image from Storage
                                if (imageUrl != null && !imageUrl.isEmpty() && imageUrl.contains("firebasestorage.googleapis.com")) {
                                    try {
                                        StorageReference postImageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
                                        // Add image deletion task to list. Add `addOnFailureListener` to log errors but allow `whenAllComplete` to proceed.
                                        deletionTasks.add(postImageRef.delete().addOnFailureListener(e -> Log.e(TAG, "Failed to delete post image for post " + doc.getId() + ": " + e.getMessage())));
                                    } catch (IllegalArgumentException e) {
                                        Log.w(TAG, "Post image URL for post " + doc.getId() + " not valid, skipping image deletion: " + e.getMessage());
                                    }
                                }
                                // Add post document deletion task to list
                                deletionTasks.add(doc.getReference().delete().addOnFailureListener(e -> Log.e(TAG, "Failed to delete post document " + doc.getId() + ": " + e.getMessage())));
                            }

                            // Execute all deletion tasks concurrently and wait for them to complete
                            if (!deletionTasks.isEmpty()) {
                                Tasks.whenAllComplete(deletionTasks)
                                        .addOnCompleteListener(allTasks -> {
                                            if (allTasks.isSuccessful()) {
                                                Log.d(TAG, "All user posts and images deleted successfully.");
                                            } else {
                                                Log.e(TAG, "One or more post/image deletions failed. Errors may be logged above.");
                                            }
                                            onComplete.run(); // Proceed regardless of individual task success
                                        });
                            } else {
                                Log.d(TAG, "No posts found for user to delete.");
                                onComplete.run(); // No posts, proceed
                            }
                        } else {
                            // Failed to query posts
                            Log.e(TAG, "Failed to query user posts for deletion: " + task.getException());
                            onComplete.run(); // Proceed even if query fails
                        }
                    });
        } else {
            // Not an author or posts not opted for deletion, proceed immediately
            Log.d(TAG, "Skipping post deletion (not author or deletePosts not checked).");
            onComplete.run();
        }
    }

    /**
     * Step 3 in deletion process: Deletes the user's document from Firestore.
     * @param onComplete Callback to run after user document deletion attempt.
     */
    private void deleteUserDocument(Runnable onComplete) {
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid()).delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "User document deleted from Firestore.");
                        onComplete.run(); // Proceed
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to delete user document from Firestore: " + e.getMessage());
                        handleDeletionFailure("Failed to delete user data.", e); // Handle and stop
                    });
        } else {
            Log.e(TAG, "Current user is null, cannot delete user document.");
            handleDeletionFailure("Authentication error during deletion.", null); // Handle and stop
        }
    }

    /**
     * Final step in deletion process: Deletes the user's account from Firebase Authentication.
     * This step requires the user to have recently logged in.
     */
    private void deleteFirebaseAuthAccount() {
        if (currentUser != null) {
            currentUser.delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Firebase Authentication account deleted.");
                        Toast.makeText(UserInfo.this, "Account deleted successfully!", Toast.LENGTH_LONG).show();
                        signOutAndNavigateToSignIn(); // Navigate away after successful deletion
                    })
                    .addOnFailureListener(e -> {
                        if (e instanceof FirebaseAuthRecentLoginRequiredException) {
                            // User needs to re-authenticate (e.g., if session expired)
                            Log.e(TAG, "Recent login required for account deletion.", e);
                            handler.post(() -> showReauthenticateDialog());
                        } else {
                            // Other authentication errors
                            Log.e(TAG, "Failed to delete Firebase Auth account: " + e.getMessage());
                            handleDeletionFailure("Failed to delete account (authentication error).", e);
                        }
                    });
        } else {
            Log.e(TAG, "Current user is null, cannot delete Firebase Auth account.");
            handleDeletionFailure("Authentication error during deletion.", null);
        }
    }

    /**
     * Displays a dialog prompting the user to re-authenticate to delete their account.
     */
    private void showReauthenticateDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Re-authenticate Required")
                .setMessage("For security reasons, please re-authenticate to delete your account.")
                .setPositiveButton("Re-authenticate", (dialog, which) -> {
                    // Navigate to a re-authentication screen or directly prompt credentials
                    // For simplicity, we'll send them to SignIn and expect them to re-login.
                    // A more robust solution would be to prompt for password/credentials directly here.
                    Toast.makeText(UserInfo.this, "Please sign in again to confirm deletion.", Toast.LENGTH_LONG).show();
                    signOutAndNavigateToSignIn();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                    handleDeletionFailure("Account deletion cancelled.", null); // Re-enable buttons
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shut down the executor service to prevent memory leaks
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }
}