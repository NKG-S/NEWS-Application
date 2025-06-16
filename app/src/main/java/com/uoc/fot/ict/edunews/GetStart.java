package com.uoc.fot.ict.edunews;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class GetStart extends AppCompatActivity {

    private static final String PREFS_NAME = "MyAppPrefs"; // Must match SplashScreen
    private static final String KEY_FIRST_LAUNCH = "is_first_launch"; // Must match SplashScreen

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_get_start);

        Button getStartButton = findViewById(R.id.getstart);

        getStartButton.setOnClickListener(view -> {
            Toast.makeText(this, "Get Started", Toast.LENGTH_SHORT).show();

            // Set the flag to false, indicating app has been launched once
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(KEY_FIRST_LAUNCH, false);
            editor.apply(); // Use apply() for async save

            Intent intent = new Intent(GetStart.this, SignIn.class);
            startActivity(intent);
            finish(); // Finish the GetStart activity so user can't go back to it
        });

    }
}