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
    private TextView forgotPasswordButton, signUpTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Initialize the input fields and buttons
        emailInput = findViewById(R.id.EmailInput);
        passwordInput = findViewById(R.id.PasswordInput);
        loginButton = findViewById(R.id.Loginbutton);
        forgotPasswordButton = findViewById(R.id.ForgotPasswordButton);
        signUpTextView = findViewById(R.id.SignUpTXT);

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
                // Navigate to Fogot Password activity
                Intent intent = new Intent(SignIn.this, FogotPassword.class);
                startActivity(intent);
            }
        });


        signUpTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Create an account !", Toast.LENGTH_LONG).show();
                // Create an Intent to navigate to SignUpActivity
                Intent intent = new Intent(SignIn.this, SignUp.class);
                startActivity(intent); // Start the new activity
            }
        });


    }
}