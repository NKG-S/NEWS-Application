package com.uoc.fot.ict.edunews;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils; // Import TextUtils
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar; // Import ProgressBar
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText; // Use TextInputEditText
import com.google.android.material.textfield.TextInputLayout; // Use TextInputLayout
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects; // Import Objects

public class FogotPassword extends AppCompatActivity {

    private static final String TAG = "ForgotPasswordActivity"; // Tag for logging

    // Firebase instance
    private FirebaseAuth mAuth;

    // UI elements
    private TextInputEditText emailInput;
    private TextInputLayout emailInputLayout; // For displaying errors
    private Button submitButton;
    private TextView loginText;
    private ProgressBar progressBar; // Declare ProgressBar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // EdgeToEdge.enable(this); // Keep this if you want edge-to-edge display
        setContentView(R.layout.activity_fogot_password);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        emailInput = findViewById(R.id.EmailInput);
        emailInputLayout = findViewById(R.id.emailInputLayout); // Initialize TextInputLayout
        submitButton = findViewById(R.id.SubmitButton);
        loginText = findViewById(R.id.LoginTXT);
        progressBar = findViewById(R.id.progressBar); // Initialize the progress bar

        // Set click listener for submit button
        submitButton.setOnClickListener(v -> attemptResetPassword());

        // Set click listener for back to login text
        loginText.setOnClickListener(v -> {
            Intent intent = new Intent(FogotPassword.this, SignIn.class); // Navigate to SignIn, not MainActivity
            startActivity(intent);
            finish(); // Close ForgotPassword activity
        });

        // Apply window insets to adjust for system bars (like status bar/navigation bar)
        // The ScrollView handles keyboard resizing, so this is mainly for initial layout.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.UserInfo), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void attemptResetPassword() {
        // Reset errors
        emailInputLayout.setError(null);

        String email = Objects.requireNonNull(emailInput.getText()).toString().trim();

        // Validate email
        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError("Email address is required.");
            Toast.makeText(this, "Please enter your email address.", Toast.LENGTH_SHORT).show();
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError("Please enter a valid email address.");
            Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show();
        } else {
            // Show progress bar
            progressBar.setVisibility(View.VISIBLE);
            sendPasswordResetEmail(email);
        }
    }

    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressBar.setVisibility(View.GONE); // Hide progress bar

                        if (task.isSuccessful()) {
                            Log.d(TAG, "Password reset email sent to " + email);
                            Toast.makeText(FogotPassword.this,
                                    "Password reset link sent to your email. Please check your inbox.",
                                    Toast.LENGTH_LONG).show();

                            // Optionally, navigate back to the login screen after successful email sent
                            Intent intent = new Intent(FogotPassword.this, SignIn.class);
                            startActivity(intent);
                            finish(); // Close ForgotPassword activity
                        } else {
                            // If sending failed, display a message to the user.
                            Log.w(TAG, "sendPasswordResetEmail:failure", task.getException());
                            String errorMessage = "Failed to send reset email.";
                            if (task.getException() != null) {
                                errorMessage += " " + task.getException().getMessage();
                            }
                            Toast.makeText(FogotPassword.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}