package com.uoc.fot.ict.edunews;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class SplashScreen extends AppCompatActivity {

    private static final String PREFS_NAME = "MyAppPrefs"; // Name for SharedPreferences file
    private static final String KEY_FIRST_LAUNCH = "is_first_launch"; // Key for the flag

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_screen);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                // By default, isFirstLaunch will be true if not found (first time app is opened)
                boolean isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true);

                Intent nextIntent;
                if (isFirstLaunch) {
                    // If it's the first launch, go to the GetStart onboarding screen
                    nextIntent = new Intent(SplashScreen.this, GetStart.class);
                } else {
                    // If not the first launch, go directly to the SignIn screen
                    nextIntent = new Intent(SplashScreen.this, SignIn.class);
                }
                startActivity(nextIntent);
                finish(); // Finish the SplashScreen activity
            }
        }, 1500); // 1.5 second delay

        // Ensure the correct ID is used here. It was 'UserInfo' in your snippet.
        // Assuming 'main' is the root layout ID for activity_splash_screen.
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
    }
}