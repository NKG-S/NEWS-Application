package com.uoc.fot.ict.edunews;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashScreen extends AppCompatActivity {

    private static final String PREFS_NAME = "MyAppPrefs";
    private static final String KEY_FIRST_LAUNCH = "is_first_launch";
    private static final String TAG = "SplashScreen"; // Tag for logging

    private FirebaseAuth mAuth; // Firebase Auth instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_screen);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        new Handler().postDelayed(() -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();

            if (currentUser != null) {
                // User is logged in, now check if their account is still valid (not deleted by admin)
                currentUser.reload().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Intent nextIntent;
                        if (task.isSuccessful()) {
                            // User reloaded successfully, meaning their account still exists
                            Log.d(TAG, "Firebase user session is valid. Navigating to Home.");
                            nextIntent = new Intent(SplashScreen.this, home.class);
                        } else {
                            // User reload failed (e.g., account deleted or disabled)
                            Log.w(TAG, "Firebase user session invalid or account deleted. Redirecting to login/onboarding.", task.getException());
                            // Sign out the current user if reload fails to clear invalid state
                            mAuth.signOut();
                            handleFirstLaunchOrSignIn();
                            return; // Return here to prevent double navigation
                        }
                        startActivity(nextIntent);
                        finish(); // Finish the SplashScreen activity
                    }
                });
            } else {
                // No user is logged in, proceed with the normal first launch / sign in flow
                Log.d(TAG, "No Firebase user logged in. Proceeding to login/onboarding.");
                handleFirstLaunchOrSignIn();
            }
        }, 1000); // 1.5 second delay
    }

    /**
     * Handles navigation based on whether it's the first app launch or subsequent launches.
     * This method is called if no user is logged in, or if a logged-in user's account is invalid.
     */
    private void handleFirstLaunchOrSignIn() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true);

        Intent nextIntent;
        if (isFirstLaunch) {
            // It's the first launch ever, go to GetStart onboarding
            Log.d(TAG, "First launch detected. Navigating to GetStart.");
            nextIntent = new Intent(SplashScreen.this, GetStart.class);
            // Mark as not first launch for next time
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
        } else {
            // Not the first launch, go to SignIn screen
            Log.d(TAG, "Not first launch. Navigating to SignIn.");
            nextIntent = new Intent(SplashScreen.this, SignIn.class);
        }
        startActivity(nextIntent);
        finish(); // Finish the SplashScreen activity
    }
}