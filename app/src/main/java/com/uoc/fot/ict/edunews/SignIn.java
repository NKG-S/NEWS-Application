package com.uoc.fot.ict.edunews;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SignIn extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Initialize the input fields and button
        emailInput = findViewById(R.id.EmailInput);
        passwordInput = findViewById(R.id.PasswordInput);
        loginButton = findViewById(R.id.Loginbutton);

        // Set onClickListener for the login button
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the text from email and password fields
                String email = emailInput.getText().toString().trim();
                String password = passwordInput.getText().toString().trim();
                // Check if the email and password fields are filled
                if (email.isEmpty()) {
                    Toast.makeText(SignIn.this, "Please enter your email", Toast.LENGTH_SHORT).show();
                    return; // Exit the method if the email is empty
                }
                if (password.isEmpty()) {
                    Toast.makeText(SignIn.this, "Please enter your password", Toast.LENGTH_SHORT).show();
                    return; // Exit the method if the password is empty
                }
                // If both fields are filled, navigate to MainActivity
                Intent intent = new Intent(SignIn.this, MainActivity.class);
                startActivity(intent);
                finish(); // Close the current SignIn activity
            }
        });
    }
}
