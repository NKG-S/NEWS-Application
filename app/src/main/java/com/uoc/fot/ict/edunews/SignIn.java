package com.uoc.fot.ict.edunews;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SignIn extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private Button loginButton;
    private TextView forgotPasswordButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Initialize the input fields and buttons
        emailInput = findViewById(R.id.EmailInput);
        passwordInput = findViewById(R.id.PasswordInput);
        loginButton = findViewById(R.id.Loginbutton);
        forgotPasswordButton = findViewById(R.id.ForgotPasswordButton);

        // Set onClickListener for the login button
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the text from email and password fields
                String email = emailInput.getText().toString().trim();
                String password = passwordInput.getText().toString().trim();

                // Check if the email field is filled
                if (email.isEmpty()) {
                    Toast.makeText(SignIn.this, "Please enter your email", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check if the password field is filled
                if (password.isEmpty()) {
                    Toast.makeText(SignIn.this, "Please enter your password", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Validate password strength
                if (!isPasswordValid(password)) {
                    return;
                }

                // If all validations pass, navigate to MainActivity
                Intent intent = new Intent(SignIn.this, MainActivity.class);
                startActivity(intent);
                finish(); // Close the current SignIn activity
            }
        });

        // Set onClickListener for the forgot password button
        forgotPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to FogotPassword activity
                Intent intent = new Intent(SignIn.this, FogotPassword.class);
                startActivity(intent);
            }
        });
    }

    /**
     * Validates the password against complexity requirements
     * @param password The password to validate
     * @return true if password meets all requirements, false otherwise
     */
    private boolean isPasswordValid(String password) {
        // Check for minimum length
        if (password.length() < 8) {
            Toast.makeText(SignIn.this,
                    "Password must be at least 8 characters long",
                    Toast.LENGTH_LONG).show();
            return false;
        }

        // Check for at least one uppercase letter
        if (!password.matches(".*[A-Z].*")) {
            Toast.makeText(SignIn.this,
                    "Password must contain at least one uppercase letter",
                    Toast.LENGTH_LONG).show();
            return false;
        }

        // Check for at least one lowercase letter
        if (!password.matches(".*[a-z].*")) {
            Toast.makeText(SignIn.this,
                    "Password must contain at least one lowercase letter",
                    Toast.LENGTH_LONG).show();
            return false;
        }

        // Check for at least one digit
        if (!password.matches(".*\\d.*")) {
            Toast.makeText(SignIn.this,
                    "Password must contain at least one digit",
                    Toast.LENGTH_LONG).show();
            return false;
        }

        // Check for at least one special character
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            Toast.makeText(SignIn.this,
                    "Password must contain at least one special character",
                    Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }
}