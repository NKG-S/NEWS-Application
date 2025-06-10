package com.uoc.fot.ict.edunews;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

public class UserInfo extends AppCompatActivity {

    // UI Elements
    private TextInputEditText usernameInput, passwordInput, addressInput, mobileInput, emailInput;
    private Button editSaveButton, mainActionButton; // editSaveButton is top right, mainActionButton is bottom
    private ImageButton backButton;

    // Dummy User Data (replace with actual database data)
    private String currentUsername = "Nethmin";
    private String currentPassword = "dummyPassword123!"; // Store hashed password in real app!
    private String currentAddress = "294/2, Mapalagama Road, Mattaka";
    private String currentMobile = "+94 721663030";
    private String currentEmail = "nethminkavindu@gmail.com";

    private boolean isEditMode = false; // Initial state is view mode

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info); // Make sure this matches your layout file name

        // Initialize UI elements
        usernameInput = findViewById(R.id.UsernameInput);
        passwordInput = findViewById(R.id.PasswordInput);
        addressInput = findViewById(R.id.AddressInput);
        mobileInput = findViewById(R.id.MobileInput);
        emailInput = findViewById(R.id.EmailInput);

        editSaveButton = findViewById(R.id.editSaveButton);
        mainActionButton = findViewById(R.id.mainActionButton);
        backButton = findViewById(R.id.backButton);

        // Set initial data and state
        displayUserData(false); // Display in view mode initially

        // Set Listeners
        editSaveButton.setOnClickListener(v -> toggleEditMode());
        mainActionButton.setOnClickListener(v -> {
            if (isEditMode) {
                saveUserData();
            } else {
                signOutUser();
            }
        });
        backButton.setOnClickListener(v -> onBackPressed());

        // Handle profile picture edit (optional, could open gallery/camera)
        findViewById(R.id.editProfilePictureButton).setOnClickListener(v -> {
            Toast.makeText(this, "Edit Profile Picture clicked!", Toast.LENGTH_SHORT).show();
            // Implement logic to change profile picture here
        });
    }

    /**
     * Updates the UI with user data and sets editability.
     * @param editable True to make fields editable, false for read-only.
     */
    private void displayUserData(boolean editable) {
        // Update EditTexts with current data
        usernameInput.setText(currentUsername);
        passwordInput.setText(currentPassword); // Be careful with displaying actual passwords
        addressInput.setText(currentAddress);
        mobileInput.setText(currentMobile);
        emailInput.setText(currentEmail);

        // Set editability for all fields
        setFieldEditable(usernameInput, editable);
        setFieldEditable(passwordInput, editable);
        setFieldEditable(addressInput, editable);
        setFieldEditable(mobileInput, editable);
        setFieldEditable(emailInput, editable);

        // Adjust password input type based on editability for clear text visibility
        // The TextInputLayout's passwordToggleEnabled handles showing/hiding dots,
        // but if you want the actual text to be visible when editable,
        // you might change inputType or transformation method.
        // For consistent look with the eye icon, we just rely on that.
        if (!editable) {
            passwordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());
        } else {
            // In edit mode, the eye icon will control visibility, so no need to force hide.
            // passwordInput.setTransformationMethod(null); // Optional: to show plain text in edit mode by default
        }

        // Update button text and color based on mode
        if (editable) {
            editSaveButton.setText("Save");
            editSaveButton.setTextColor(getResources().getColor(R.color.ButtonColour, getTheme()));
            mainActionButton.setText("Save"); // Change bottom button to Save
            mainActionButton.setBackgroundTintList(getResources().getColorStateList(R.color.ButtonColour, getTheme()));

            // Visually indicate editable fields
            usernameInput.setTextColor(getResources().getColor(R.color.black, getTheme()));
            emailInput.setTextColor(getResources().getColor(R.color.black, getTheme()));
            passwordInput.setTextColor(getResources().getColor(R.color.black, getTheme()));
            addressInput.setTextColor(getResources().getColor(R.color.black, getTheme()));
            mobileInput.setTextColor(getResources().getColor(R.color.black, getTheme()));

        } else {
            editSaveButton.setText("Edit");
            editSaveButton.setTextColor(getResources().getColor(R.color.ButtonColour, getTheme()));
            mainActionButton.setText("Sign Out"); // Change bottom button to Sign Out
            mainActionButton.setBackgroundTintList(getResources().getColorStateList(R.color.ButtonColour, getTheme()));


            // Visually indicate non-editable fields (low visible)
            // You might need to define a custom color for 'low visibility' in colors.xml
            // For now, using a slightly lighter black or gray.
            usernameInput.setTextColor(Color.parseColor("#80000000")); // 50% black alpha
            emailInput.setTextColor(Color.parseColor("#80000000"));
            passwordInput.setTextColor(Color.parseColor("#80000000"));
            addressInput.setTextColor(Color.parseColor("#80000000"));
            mobileInput.setTextColor(Color.parseColor("#80000000"));
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
    }

    /**
     * Saves user data after validation.
     */
    private void saveUserData() {
        // Reset errors before validation
        usernameInput.setError(null);
        passwordInput.setError(null);
        addressInput.setError(null);
        mobileInput.setError(null);
        emailInput.setError(null);

        String newUsername = usernameInput.getText().toString().trim();
        String newPassword = passwordInput.getText().toString().trim();
        String newAddress = addressInput.getText().toString().trim();
        String newMobile = mobileInput.getText().toString().trim();
        String newEmail = emailInput.getText().toString().trim();

        boolean cancel = false; // Flag for validation errors

        // Validate data (similar to SignUp for consistency)
        if (TextUtils.isEmpty(newUsername)) {
            usernameInput.setError("Username cannot be empty.");
            cancel = true;
        }
        if (TextUtils.isEmpty(newEmail)) {
            emailInput.setError("Email cannot be empty.");
            cancel = true;
        } else if (!isEmailValid(newEmail)) {
            emailInput.setError("Invalid email format.");
            cancel = true;
        }
        // Only validate password strength if it has changed AND it's not empty
        if (!newPassword.equals(currentPassword) && !TextUtils.isEmpty(newPassword)) {
            if (!isPasswordValid(newPassword)) {
                cancel = true; // isPasswordValid will set error on field
            }
        }
        // Address and Mobile can be empty or have simpler validation if needed
        // For simplicity, we won't add complex validation for these now.

        if (cancel) {
            Toast.makeText(this, "Please correct the errors to save.", Toast.LENGTH_SHORT).show();
            return; // Stay in edit mode if validation fails
        }

        // If validation passes, update dummy data
        currentUsername = newUsername;
        currentPassword = newPassword; // In a real app, hash and store securely!
        currentAddress = newAddress;
        currentMobile = newMobile;
        currentEmail = newEmail;

        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();

        // Switch back to view mode
        toggleEditMode();
    }

    /**
     * Helper method for email validation (reused from SignUp.java).
     */
    private boolean isEmailValid(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Helper method for password strength validation (reused from SignUp.java).
     * Sets error on passwordInput if rules are not met.
     */
    private boolean isPasswordValid(String password) {
        if (password.length() < 8) {
            passwordInput.setError("Password must be at least 8 characters long.");
            return false;
        }
        if (!password.matches(".*[A-Z].*")) {
            passwordInput.setError("Password needs at least one uppercase letter.");
            return false;
        }
        if (!password.matches(".*[a-z].*")) {
            passwordInput.setError("Password needs at least one lowercase letter.");
            return false;
        }
        if (!password.matches(".*\\d.*")) {
            passwordInput.setError("Password needs at least one digit.");
            return false;
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            passwordInput.setError("Password needs at least one special character.");
            return false;
        }
        return true;
    }

    /**
     * Handles user sign out.
     */
    private void signOutUser() {
        Toast.makeText(this, "Signing out...", Toast.LENGTH_SHORT).show();
        // Implement actual sign-out logic here (e.g., clear session, navigate to login)
        Intent intent = new Intent(UserInfo.this, MainActivity.class); // Assuming MainActivity is your login screen
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Finish current activity
    }

    @Override
    public void onBackPressed() {
        if (isEditMode) {
            // If in edit mode, confirm if user wants to discard changes
            Toast.makeText(this, "Changes discarded.", Toast.LENGTH_SHORT).show();
            isEditMode = false; // Revert to view mode
            displayUserData(false);
        } else {
            super.onBackPressed(); // Go back normally
        }
    }
}
