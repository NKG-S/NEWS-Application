package com.uoc.fot.ict.edunews;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class FogotPassword extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_fogot_password);

        // Initialize views
        Button submitButton = findViewById(R.id.SubmitButton);
        TextView loginText = findViewById(R.id.LoginTXT);

        // Set click listener for submit button
        submitButton.setOnClickListener(v -> {
            Toast.makeText(this, "Password reset link sent to your email", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(FogotPassword.this, MainActivity.class);
            startActivity(intent);
            // Here you would typically implement the password reset logic
        });

        // Set click listener for back to login text
        loginText.setOnClickListener(v -> {
            Intent intent = new Intent(FogotPassword.this, SignIn.class);
            startActivity(intent);
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}