package com.uoc.fot.ict.edunews;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.uoc.fot.ict.edunews.MainActivity;
import com.uoc.fot.ict.edunews.R;

public class SignUp extends AppCompatActivity {

    private EditText usernameInput, emailInput, passwordInput, confirmPasswordInput;
    private Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        emailInput = findViewById(R.id.EmailInput);
        passwordInput = findViewById(R.id.PasswordInput);
        confirmPasswordInput = findViewById(R.id.ConfirmPasswordInput);
        usernameInput = findViewById(R.id.UsernameInput);
        registerButton = findViewById(R.id.RegisterButton);
        registerButton.setOnClickListener(v -> attemptRegistration());
    }

    private void attemptRegistration() {
        // Reset errors
        usernameInput.setError(null);
        emailInput.setError(null);
        passwordInput.setError(null);
        confirmPasswordInput.setError(null);

        String username = usernameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        boolean cancel = false; // Flag to indicate if any validation failed

        // Validate username
        if (TextUtils.isEmpty(username)) {
            usernameInput.setError("Username is required");
            cancel = true;
        }

        // Validate email
        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            cancel = true;
        } else if (!isEmailValid(email)) {
            emailInput.setError("Please enter a valid email address.");
            cancel = true;
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required.");
            cancel = true;
        } else if (!isPasswordValid(password)) {
            // isPasswordValid will set the error on passwordInput directly if invalid
            cancel = true;
        }

        // Validate confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordInput.setError("Please confirm your password.");
            cancel = true;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords don't match. Please try again.");
            cancel = true;
        }

        if (cancel) {
            // If any validation failed, show a general toast message as well.
            Toast.makeText(this, "Please fix the errors to proceed.", Toast.LENGTH_SHORT).show();
        } else {
            // All validations passed, proceed with registration
            registerUser(username, email, password);
        }
    }

    private boolean isEmailValid(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isPasswordValid(String password) {
        // We'll set the error directly on the passwordInput field
        // and return false if any condition isn't met.
        // This makes the error messages directly visible on the input field.

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

        return true; // All password rules are met
    }

    private void registerUser(String username, String email, String password) {
        // Here you would typically make an API call to register the user
        // For now, we'll just show a success message
        Toast.makeText(this, "Registration successful! Welcome, " + username + "!", Toast.LENGTH_LONG).show();

        // Optionally navigate to another activity after successful registration
        // Intent intent = new Intent(SignUp.this, HomeActivity.class);
        // startActivity(intent);
        // finish();
    }
}