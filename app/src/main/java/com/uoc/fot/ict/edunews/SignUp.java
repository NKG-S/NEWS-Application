package com.uoc.fot.ict.edunews;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class SignUp extends AppCompatActivity {

    private static final String TAG = "SignUpActivity"; // Tag for logging

    // Firebase instances
    private FirebaseAuth mAuth;
    private FirebaseFirestore db; // Firebase Firestore instance

    // UI elements
    private TextInputEditText usernameInput, addressInput, mobileNumberInput, emailInput, passwordInput, confirmPasswordInput;
    private TextInputLayout usernameInputLayout, addressInputLayout, mobileNumberInputLayout, emailInputLayout, passwordInputLayout, confirmPasswordInputLayout;
    private Button registerButton;
    private TextView signInText;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // Initialize Cloud Firestore

        // Initialize TextInputEditText views
        usernameInput = findViewById(R.id.UsernameInput);
        addressInput = findViewById(R.id.AddressInput);
        mobileNumberInput = findViewById(R.id.MobileNumberInput);
        emailInput = findViewById(R.id.EmailInput);
        passwordInput = findViewById(R.id.PasswordInput);
        confirmPasswordInput = findViewById(R.id.ConfirmPasswordInput);

        // Initialize TextInputLayout views to set errors directly
        usernameInputLayout = findViewById(R.id.usernameInputLayout);
        addressInputLayout = findViewById(R.id.AddressInputLayout);
        mobileNumberInputLayout = findViewById(R.id.MobileNumberInputLayout);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout);

        registerButton = findViewById(R.id.RegisterButton);
        signInText = findViewById(R.id.SignInTXT);
        progressBar = findViewById(R.id.progressBar); // Initialize the progress bar

        // Set click listener for register button
        registerButton.setOnClickListener(v -> attemptRegistration());

        // Set click listener for sign in text
        signInText.setOnClickListener(v -> {
            Intent intent = new Intent(SignUp.this, SignIn.class);
            startActivity(intent);
            finish(); // Finish the SignUp activity so the user can't go back with back button
        });
    }

    private void attemptRegistration() {
        // Reset errors for all fields
        usernameInputLayout.setError(null);
        addressInputLayout.setError(null);
        mobileNumberInputLayout.setError(null);
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);
        confirmPasswordInputLayout.setError(null);

        String username = Objects.requireNonNull(usernameInput.getText()).toString().trim();
        String address = Objects.requireNonNull(addressInput.getText()).toString().trim();
        String mobileNumber = Objects.requireNonNull(mobileNumberInput.getText()).toString().trim();
        String email = Objects.requireNonNull(emailInput.getText()).toString().trim();
        String password = Objects.requireNonNull(passwordInput.getText()).toString().trim();
        String confirmPassword = Objects.requireNonNull(confirmPasswordInput.getText()).toString().trim();

        boolean cancel = false; // Flag to indicate if any validation failed

        // Validate username
        if (TextUtils.isEmpty(username)) {
            usernameInputLayout.setError("Username is required.");
            cancel = true;
        }

        // Validate address
        if (TextUtils.isEmpty(address)) {
            addressInputLayout.setError("Address is required.");
            cancel = true;
        }

        // Validate mobile number (10 digits)
        if (TextUtils.isEmpty(mobileNumber)) {
            mobileNumberInputLayout.setError("Mobile Number is required.");
            cancel = true;
        } else if (!Pattern.matches("\\d{10}", mobileNumber)) { // Simple check for 10 digits
            mobileNumberInputLayout.setError("Please enter a valid 10-digit mobile number.");
            cancel = true;
        }

        // Validate email
        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError("Email is required.");
            cancel = true;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError("Please enter a valid email address.");
            cancel = true;
        }

        // Validate password (complexity rules)
        if (TextUtils.isEmpty(password)) {
            passwordInputLayout.setError("Password is required.");
            cancel = true;
        } else if (!isPasswordValid(password)) {
            // isPasswordValid will set the error on passwordInputLayout directly if invalid
            cancel = true;
        }

        // Validate confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordInputLayout.setError("Please confirm your password.");
            cancel = true;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordInputLayout.setError("Passwords don't match. Please try again.");
            cancel = true;
        }

        if (cancel) {
            Toast.makeText(this, "Please fix the errors to proceed.", Toast.LENGTH_SHORT).show();
        } else {
            // All validations passed, proceed with Firebase registration
            progressBar.setVisibility(View.VISIBLE); // Show progress bar
            registerUserInFirebase(email, password, username, address, mobileNumber);
        }
    }

    private boolean isPasswordValid(String password) {
        // Set error directly on the TextInputLayout and return false if a condition isn't met.
        if (password.length() < 8) {
            passwordInputLayout.setError("Password must be at least 8 characters long.");
            return false;
        }
        if (!password.matches(".*[A-Z].*")) {
            passwordInputLayout.setError("Password needs at least one uppercase letter.");
            return false;
        }
        if (!password.matches(".*[a-z].*")) {
            passwordInputLayout.setError("Password needs at least one lowercase letter.");
            return false;
        }
        if (!password.matches(".*\\d.*")) {
            passwordInputLayout.setError("Password needs at least one digit.");
            return false;
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            passwordInputLayout.setError("Password needs at least one special character.");
            return false;
        }
        return true; // All password rules are met
    }

    private void registerUserInFirebase(String email, String password, String username, String address, String mobileNumber) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE); // Hide progress bar

                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();

                            if (user != null) {
                                // Save additional user details to Cloud Firestore
                                saveUserDataToFirestore(user.getUid(), username, address, mobileNumber, email);

                                Toast.makeText(SignUp.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                                // Navigate to the main activity or a welcome screen
                                Intent intent = new Intent(SignUp.this, home.class);
                                startActivity(intent);
                                finish(); // Prevent going back to SignUp
                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            String errorMessage = "Registration failed.";
                            if (task.getException() != null) {
                                errorMessage += " " + task.getException().getMessage();
                            }
                            Toast.makeText(SignUp.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    // Save user data to Cloud Firestore
    private void saveUserDataToFirestore(String userId, String username, String address, String mobileNumber, String email) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("username", username);
        userMap.put("email", email);
        userMap.put("address", address);
        userMap.put("mobileNumber", mobileNumber);
        userMap.put("createdAt", FieldValue.serverTimestamp()); // Firestore server timestamp
        userMap.put("author", false); // Set the default value of "author" to false

        db.collection("users").document(userId)
                .set(userMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User data saved to Firestore successfully!"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving user data to Firestore", e));
    }
}
