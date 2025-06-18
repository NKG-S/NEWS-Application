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
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * SignUp Activity handles user registration functionality.
 * It validates user input, registers new users with Firebase Authentication,
 * sends an email verification, and saves user profile data to Firestore.
 */
public class SignUp extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";

    // Firebase instances
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // UI elements
    private TextInputEditText usernameInput, addressInput, mobileNumberInput, emailInput, passwordInput, confirmPasswordInput;
    private TextInputLayout usernameInputLayout, addressInputLayout, mobileNumberInputLayout, emailInputLayout, passwordInputLayout, confirmPasswordInputLayout;
    private Button registerButton;
    private TextView signInText;
    private ProgressBar progressBar;

    private static final String DEFAULT_COUNTRY = "Sri Lanka"; // Default country for new users

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI elements by finding their IDs from the layout
        usernameInput = findViewById(R.id.UsernameInput);
        addressInput = findViewById(R.id.AddressInput);
        mobileNumberInput = findViewById(R.id.MobileNumberInput);
        emailInput = findViewById(R.id.EmailInput);
        passwordInput = findViewById(R.id.PasswordInput);
        confirmPasswordInput = findViewById(R.id.ConfirmPasswordInput);

        usernameInputLayout = findViewById(R.id.usernameInputLayout);
        addressInputLayout = findViewById(R.id.AddressInputLayout);
        mobileNumberInputLayout = findViewById(R.id.MobileNumberInputLayout);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout);

        registerButton = findViewById(R.id.RegisterButton);
        signInText = findViewById(R.id.SignInTXT);
        progressBar = findViewById(R.id.progressBar);

        // Set click listener for the register button to attempt registration
        registerButton.setOnClickListener(v -> attemptRegistration());

        // Set click listener for the "Sign In" text to navigate to SignIn activity
        signInText.setOnClickListener(v -> {
            Intent intent = new Intent(SignUp.this, SignIn.class);
            // Flags to clear the activity stack, preventing back navigation to SignUp
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Finish SignUp activity
        });
    }

    /**
     * Attempts to register a new user by validating input fields and
     * then calling the Firebase registration method.
     */
    private void attemptRegistration() {
        // Reset errors for all input layouts before validation
        usernameInputLayout.setError(null);
        addressInputLayout.setError(null);
        mobileNumberInputLayout.setError(null);
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);
        confirmPasswordInputLayout.setError(null);

        // Retrieve and trim input values
        String username = Objects.requireNonNull(usernameInput.getText()).toString().trim();
        String address = Objects.requireNonNull(addressInput.getText()).toString().trim();
        String mobileNumber = Objects.requireNonNull(mobileNumberInput.getText()).toString().trim();
        String email = Objects.requireNonNull(emailInput.getText()).toString().trim();
        String password = Objects.requireNonNull(passwordInput.getText()).toString().trim();
        String confirmPassword = Objects.requireNonNull(confirmPasswordInput.getText()).toString().trim();

        boolean cancel = false; // Flag to indicate if any validation failed

        // Validate Username
        if (TextUtils.isEmpty(username)) {
            usernameInputLayout.setError("Username is required.");
            Toast.makeText(this, "Username is required.", Toast.LENGTH_SHORT).show();
            cancel = true;
        }

        // Validate Address
        if (TextUtils.isEmpty(address)) {
            addressInputLayout.setError("Address is required.");
            Toast.makeText(this, "Address is required.", Toast.LENGTH_SHORT).show();
            cancel = true;
        }

        // Validate Mobile Number
        if (TextUtils.isEmpty(mobileNumber)) {
            mobileNumberInputLayout.setError("Mobile Number is required.");
            Toast.makeText(this, "Mobile Number is required.", Toast.LENGTH_SHORT).show();
            cancel = true;
        } else {
            // Validate mobile number format based on prefixes
            if (mobileNumber.startsWith("+94")) {
                // Regex for +94 followed by exactly 9 digits (total 12 characters)
                if (mobileNumber.length() != 12 || !Pattern.matches("\\+94\\d{9}", mobileNumber)) {
                    mobileNumberInputLayout.setError("Mobile number starting with '+94' must be 12 characters long (e.g., +94712345678).");
                    Toast.makeText(this, "Invalid mobile number format with Country code. Expected: +94xxxxxxxxx", Toast.LENGTH_LONG).show();
                    cancel = true;
                }
            } else if (mobileNumber.startsWith("07")) {
                // Regex for 07 followed by exactly 8 digits (total 10 characters)
                if (mobileNumber.length() != 10 || !Pattern.matches("07\\d{8}", mobileNumber)) {
                    mobileNumberInputLayout.setError("Mobile number starting with '07' must be 10 characters long.");
                    Toast.makeText(this, "Invalid mobile number format for 07. Expected: 07xxxxxxxxx", Toast.LENGTH_LONG).show();
                    cancel = true;
                }
            } else {
                mobileNumberInputLayout.setError("Mobile number must start with '+94' or '07'.");
                Toast.makeText(this, "Mobile number must start with '+94' or '07'.", Toast.LENGTH_LONG).show();
                cancel = true;
            }
        }

        // Validate Email
        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError("Email is required.");
            Toast.makeText(this, "Email is required.", Toast.LENGTH_SHORT).show();
            cancel = true;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError("Please enter a valid email address.");
            Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_LONG).show();
            cancel = true;
        }

        // Validate Password
        if (TextUtils.isEmpty(password)) {
            passwordInputLayout.setError("Password is required.");
            Toast.makeText(this, "Password is required.", Toast.LENGTH_SHORT).show();
            cancel = true;
        } else if (!isPasswordValid(password)) {
            cancel = true; // isPasswordValid will set its own specific error
        }

        // Validate Confirm Password
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordInputLayout.setError("Please confirm your password.");
            Toast.makeText(this, "Please confirm your password.", Toast.LENGTH_SHORT).show();
            cancel = true;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordInputLayout.setError("Passwords don't match. Please try again.");
            Toast.makeText(this, "Passwords don't match. Please try again.", Toast.LENGTH_LONG).show();
            cancel = true;
        }

        // If any validation failed, return
        if (cancel) {
            return;
        } else {
            // If all validations pass, show progress bar and attempt registration
            progressBar.setVisibility(View.VISIBLE);
            // Directly attempt to create user, then handle collision exception
            registerUserInFirebase(email, password, username, address, mobileNumber);
        }
    }

    /**
     * Validates the password strength based on length, uppercase, lowercase,
     * digit, and special character requirements.
     * @param password The password string to validate.
     * @return True if the password meets all criteria, false otherwise.
     */
    private boolean isPasswordValid(String password) {
        passwordInputLayout.setError(null); // Clear previous error

        if (password.length() < 8) {
            passwordInputLayout.setError("Password must be at least 8 characters long.");
            Toast.makeText(this, "Password must be at least 8 characters long.", Toast.LENGTH_LONG).show();
            return false;
        }
        if (!password.matches(".*[A-Z].*")) {
            passwordInputLayout.setError("Password needs at least one uppercase letter.");
            Toast.makeText(this, "Password needs at least one uppercase letter.", Toast.LENGTH_LONG).show();
            return false;
        }
        if (!password.matches(".*[a-z].*")) {
            passwordInputLayout.setError("Password needs at least one lowercase letter.");
            Toast.makeText(this, "Password needs at least one lowercase letter.", Toast.LENGTH_LONG).show();
            return false;
        }
        if (!password.matches(".*\\d.*")) {
            passwordInputLayout.setError("Password needs at least one digit.");
            Toast.makeText(this, "Password needs at least one digit.", Toast.LENGTH_LONG).show();
            return false;
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            passwordInputLayout.setError("Password needs at least one special character.");
            Toast.makeText(this, "Password needs at least one special character.", Toast.LENGTH_LONG).show();
            return false;
        }
        return true; // Password is valid
    }

    /**
     * Registers the user with Firebase Authentication using the provided email and password.
     * Handles successful registration, email verification, and error cases including
     * `FirebaseAuthUserCollisionException` for already registered emails.
     * @param email User's email.
     * @param password User's password.
     * @param username User's chosen username.
     * @param address User's address.
     * @param mobileNumber User's mobile number.
     * Dependencies: `mAuth`, `db`, `progressBar`.
     */
    private void registerUserInFirebase(String email, String password, String username, String address, String mobileNumber) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE); // Hide progress bar after task completes

                        if (task.isSuccessful()) {
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();

                            if (user != null) {
                                // Send email verification to the newly registered user
                                user.sendEmailVerification()
                                        .addOnCompleteListener(task1 -> {
                                            if (task1.isSuccessful()) {
                                                Log.d(TAG, "Email verification sent to: " + user.getEmail());
                                                Toast.makeText(SignUp.this, "Registration successful. Verification email sent to " + user.getEmail() + ". Please verify your email before signing in.", Toast.LENGTH_LONG).show();
                                                // Save user data to Firestore only after email verification is initiated
                                                saveUserDataToFirestore(user.getUid(), username, address, mobileNumber, email, DEFAULT_COUNTRY);
                                                // Navigate to SignIn activity, user needs to verify email first
                                                Intent intent = new Intent(SignUp.this, SignIn.class);
                                                // Clear activity stack to prevent navigating back to SignUp
                                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                startActivity(intent);
                                                finish(); // Finish SignUp activity
                                            } else {
                                                Log.e(TAG, "Failed to send verification email.", task1.getException());
                                                Toast.makeText(SignUp.this, "Registration successful, but failed to send verification email. Please try signing in later or contact support.", Toast.LENGTH_LONG).show();
                                                // Even if email sending fails, we might still want to save user data if account was created
                                                saveUserDataToFirestore(user.getUid(), username, address, mobileNumber, email, DEFAULT_COUNTRY);
                                                // Still navigate to SignIn, as the account exists
                                                Intent intent = new Intent(SignUp.this, SignIn.class);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                startActivity(intent);
                                                finish(); // Finish SignUp activity
                                            }
                                        });

                            } else {
                                // This case should ideally not happen if task is successful but user is null
                                Log.e(TAG, "User is null after successful createUserWithEmail.");
                                Toast.makeText(SignUp.this, "Registration failed: User data not found.", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            // Registration failed
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            String errorMessage = "Registration failed.";
                            // Check for specific Firebase exceptions to provide user-friendly messages
                            if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                errorMessage = "This email address is already registered. Please sign in.";
                                emailInputLayout.setError(errorMessage); // Set error on email field
                            } else if (task.getException() != null) {
                                errorMessage += " " + task.getException().getMessage();
                            }
                            Toast.makeText(SignUp.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /**
     * Saves additional user profile data (username, address, mobile, country, author status)
     * to Firebase Firestore under the user's UID.
     * @param userId The UID of the newly registered user.
     * @param username The username provided by the user.
     * @param address The address provided by the user.
     * @param mobileNumber The mobile number provided by the user.
     * @param email The email address of the user.
     * @param country The default country (Sri Lanka).
     * Dependencies: `db`.
     */
    private void saveUserDataToFirestore(String userId, String username, String address, String mobileNumber, String email, String country) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("username", username);
        userMap.put("email", email);
        userMap.put("address", address);
        userMap.put("mobileNumber", mobileNumber);
        userMap.put("createdAt", FieldValue.serverTimestamp()); // Firestore timestamp for creation date
        userMap.put("country", country);
        userMap.put("author", false); // Default new users to not be authors

        // Set the document with the user's UID as the document ID
        db.collection("users").document(userId)
                .set(userMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User data saved to Firestore successfully!"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving user data to Firestore", e));
    }
}