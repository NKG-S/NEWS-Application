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

import java.util.Objects;

public class SignIn extends AppCompatActivity {

    private static final String TAG = "SignInActivity"; // Consistent tag for logging

    // Firebase instance
    private FirebaseAuth mAuth;

    // UI elements
    private TextInputEditText emailInput, passwordInput;
    private TextInputLayout emailInputLayout, passwordInputLayout; // For displaying errors
    private Button loginButton;
    private TextView forgotPasswordButton, signUpText;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize TextInputEditText views
        emailInput = findViewById(R.id.EmailInput);
        passwordInput = findViewById(R.id.PasswordInput);

        // Initialize TextInputLayout views for error handling
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);

        loginButton = findViewById(R.id.Loginbutton);
        forgotPasswordButton = findViewById(R.id.ForgotPasswordButton);
        signUpText = findViewById(R.id.SignUpTXT);
        progressBar = findViewById(R.id.progressBar);

        // Set onClickListener for the login button
        loginButton.setOnClickListener(v -> attemptLogin());

        // Set onClickListener for Forgot Password text
        forgotPasswordButton.setOnClickListener(v -> {
            Intent intent = new Intent(SignIn.this, FogotPassword.class); // Corrected to FogotPassword
            startActivity(intent);
        });

        // Set onClickListener for Sign Up text
        signUpText.setOnClickListener(v -> {
            Intent intent = new Intent(SignIn.this, SignUp.class);
            startActivity(intent);
            // No finish() here, so user can go back to SignIn from SignUp if they change their mind
        });
    }

    /**
     * This method is called when the activity starts (or resumes).
     * It's crucial for checking if a user is already signed in and their email is verified.
     */
    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User already signed in. Checking email verification status...");
            // Reload user to get fresh verification status in case they verified outside the app
            currentUser.reload().addOnCompleteListener(reloadTask -> {
                if (reloadTask.isSuccessful() && currentUser.isEmailVerified()) {
                    Log.d(TAG, "User is authenticated and email verified. Redirecting to home.");
                    navigateToHome(); // Navigate to home if verified
                } else if (reloadTask.isSuccessful() && !currentUser.isEmailVerified()) {
                    // User is logged in but not verified. Keep them on SignIn page.
                    Log.d(TAG, "User logged in but email not verified. Keeping on SignIn screen.");
                    Toast.makeText(SignIn.this, "Please verify your email to continue. Check your inbox for a verification link.", Toast.LENGTH_LONG).show();
                    // Optionally sign them out to force re-login and re-check, or prevent further action
                    mAuth.signOut(); // Sign out unverified user from previous session
                } else {
                    Log.w(TAG, "Failed to reload user status, keeping on SignIn.", reloadTask.getException());
                    // Fallback: If reload fails, assume not verified or issue, let them try to sign in
                }
            });
        } else {
            Log.d(TAG, "No user signed in.");
        }
    }

    /**
     * Attempts to log in the user after validating email and password fields.
     */
    private void attemptLogin() {
        // Reset errors
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);

        String email = Objects.requireNonNull(emailInput.getText()).toString().trim();
        String password = Objects.requireNonNull(passwordInput.getText()).toString().trim();

        boolean cancel = false; // Flag to indicate if any validation failed

        // Validate email
        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError("Email is required.");
            cancel = true;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError("Please enter a valid email address.");
            cancel = true;
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            passwordInputLayout.setError("Password is required.");
            cancel = true;
        }

        if (cancel) {
            Toast.makeText(this, "Please fix the errors to proceed.", Toast.LENGTH_SHORT).show();
        } else {
            // Show progress bar and attempt Firebase login
            progressBar.setVisibility(View.VISIBLE);
            loginUserWithFirebase(email, password);
        }
    }

    /**
     * Authenticates the user with Firebase using provided email and password.
     * Includes email verification check.
     */
    private void loginUserWithFirebase(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE); // Hide progress bar

                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();

                            if (user != null) {
                                // Reload user to get the most up-to-date verification status
                                user.reload().addOnCompleteListener(reloadTask -> {
                                    if (reloadTask.isSuccessful() && user.isEmailVerified()) {
                                        Toast.makeText(SignIn.this, "Login successful! Welcome.", Toast.LENGTH_SHORT).show();
                                        navigateToHome(); // Only navigate to home if email is verified
                                    } else if (reloadTask.isSuccessful() && !user.isEmailVerified()) {
                                        // User logged in, but email is not verified
                                        Toast.makeText(SignIn.this, "Please verify your email address to continue. A verification link has been sent to " + user.getEmail() + ".", Toast.LENGTH_LONG).show();
                                        // Sign out the user to prevent them from accessing unverified content
                                        mAuth.signOut();
                                        // Optionally, resend verification email
                                        user.sendEmailVerification()
                                                .addOnCompleteListener(resendTask -> {
                                                    if (resendTask.isSuccessful()) {
                                                        Log.d(TAG, "Verification email resent successfully.");
                                                        Toast.makeText(SignIn.this, "Verification email resent.", Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        Log.e(TAG, "Failed to resend verification email.", resendTask.getException());
                                                    }
                                                });
                                    } else {
                                        // Failed to reload user, treat as unverified for safety
                                        Toast.makeText(SignIn.this, "Authentication successful, but unable to verify email status. Please try again or check your internet connection.", Toast.LENGTH_LONG).show();
                                        mAuth.signOut(); // Sign out for safety
                                    }
                                });
                            } else {
                                // This case should ideally not happen if task is successful but user is null
                                Toast.makeText(SignIn.this, "Authentication failed. User data unavailable.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            String errorMessage = "Authentication failed. ";
                            if (task.getException() != null) {
                                errorMessage += task.getException().getMessage();
                                // Provide more user-friendly messages for common errors
                                if (task.getException().getMessage().contains("badly formatted")) {
                                    emailInputLayout.setError("Invalid email format.");
                                    errorMessage = "Invalid email format.";
                                } else if (task.getException().getMessage().contains("password")) {
                                    passwordInputLayout.setError("Incorrect password.");
                                    errorMessage = "Incorrect password or email.";
                                } else if (task.getException().getMessage().contains("no user record")) {
                                    emailInputLayout.setError("No account found with this email.");
                                    errorMessage = "No account found with this email.";
                                }
                            }
                            Toast.makeText(SignIn.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /**
     * Navigates to the home activity and clears the activity stack.
     */
    private void navigateToHome() {
        Intent intent = new Intent(SignIn.this, home.class);
        // FLAG_ACTIVITY_NEW_TASK: Starts activity in a new task.
        // FLAG_ACTIVITY_CLEAR_TASK: Clears any existing task that would hold the new activity.
        // Together, these ensure 'home' is the only activity on the stack, preventing back navigation to SignIn/SignUp.
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Finish the SignIn activity so user can't go back to it after successful login
    }
}