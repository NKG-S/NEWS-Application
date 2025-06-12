package com.uoc.fot.ict.edunews;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils; // Import TextUtils
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
import com.google.android.material.textfield.TextInputEditText; // Use TextInputEditText
import com.google.android.material.textfield.TextInputLayout; // Use TextInputLayout
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects; // Import Objects

public class SignIn extends AppCompatActivity {

    private static final String TAG = "SignInActivity"; // Tag for logging

    // Firebase instance
    private FirebaseAuth mAuth;

    // UI elements
    private TextInputEditText emailInput, passwordInput;
    private TextInputLayout emailInputLayout, passwordInputLayout; // For displaying errors
    private Button loginButton;
    private TextView forgotPasswordButton, signUpText; // Renamed to match XML IDs
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
        forgotPasswordButton = findViewById(R.id.ForgotPasswordButton); // Matches XML ID
        signUpText = findViewById(R.id.SignUpTXT); // Matches XML ID
        progressBar = findViewById(R.id.progressBar); // Initialize the progress bar

        // Set onClickListener for the login button
        loginButton.setOnClickListener(v -> attemptLogin());

        // Set onClickListener for Forgot Password text
        forgotPasswordButton.setOnClickListener(v -> {
            // TODO: Navigate to Forgot Password activity
            Toast.makeText(SignIn.this, "Forgot Password clicked!", Toast.LENGTH_SHORT).show();
            // Example: Intent intent = new Intent(SignIn.this, ForgotPasswordActivity.class);
            // startActivity(intent);
        });

        // Set onClickListener for Sign Up text
        signUpText.setOnClickListener(v -> {
            Intent intent = new Intent(SignIn.this, MainActivity.class); // Assuming SignUp.class is your registration activity
            startActivity(intent);
            // No finish() here, so user can go back to SignIn from SignUp if they change their mind
        });
    }

    // This method is called when the activity starts (or resumes)
    // It's a good place to check if a user is already signed in
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already signed in, navigate to MainActivity
            Log.d(TAG, "User already signed in: " + currentUser.getEmail());
            navigateToMainActivity();
        }
    }

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

    private void loginUserWithFirebase(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE); // Hide progress bar

                        if (task.isSuccessful()) {
                            // Sign in success
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(SignIn.this, "Login successful! Welcome.", Toast.LENGTH_SHORT).show();
                            navigateToMainActivity();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            String errorMessage = "Authentication failed.";
                            if (task.getException() != null) {
                                errorMessage += " " + task.getException().getMessage();
                            }
                            Toast.makeText(SignIn.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(SignIn.this, MainActivity.class);
        startActivity(intent);
        finish(); // Finish the SignIn activity so user can't go back to it after successful login
    }
}