package com.uoc.fot.ict.edunews;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class SignUp extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private TextInputEditText usernameInput, addressInput, mobileNumberInput, emailInput, passwordInput, confirmPasswordInput;
    private TextInputLayout usernameInputLayout, addressInputLayout, mobileNumberInputLayout, emailInputLayout, passwordInputLayout, confirmPasswordInputLayout;
    private Button registerButton;
    private TextView signInText;
    private ProgressBar progressBar;

    private static final String DEFAULT_COUNTRY = "Sri Lanka";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        usernameInput = findViewById(R.id.UsernameInput);
        addressInput = findViewById(R.id.AddressInput);
        mobileNumberInput = findViewById(R.id.MobileNumberInput);
        emailInput = findViewById(R.id.EmailInput);
        passwordInput = findViewById(R.id.PasswordInput);
        confirmPasswordInput = findViewById(R.id.ConfirmPasswordInput);

        usernameInputLayout = findViewById(R.id.usernameInputLayout);
        addressInputLayout = findViewById(R.id.AddressInputLayout);
        mobileNumberInputLayout = findViewById(R.id.MobileNumberInputLayout);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout);

        registerButton = findViewById(R.id.RegisterButton);
        signInText = findViewById(R.id.SignInTXT);
        progressBar = findViewById(R.id.progressBar);

        registerButton.setOnClickListener(v -> attemptRegistration());

        signInText.setOnClickListener(v -> {
            Intent intent = new Intent(SignUp.this, SignIn.class);
            startActivity(intent);
            finish();
        });
    }

    private void attemptRegistration() {
        // Reset errors for all fields
        usernameInputLayout.setError(null);
        addressInputLayout.setError(null);
        mobileNumberInputLayout.setError(null);
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);
        confirmPasswordInputLayout.setError(null);

        String username = Objects.requireNonNull(usernameInput.getText()).toString().trim();
        String address = Objects.requireNonNull(addressInput.getText()).toString().trim();
        String mobileNumber = Objects.requireNonNull(mobileNumberInput.getText()).toString().trim();
        String email = Objects.requireNonNull(emailInput.getText()).toString().trim();
        String password = Objects.requireNonNull(passwordInput.getText()).toString().trim();
        String confirmPassword = Objects.requireNonNull(confirmPasswordInput.getText()).toString().trim();

        boolean cancel = false;

        if (TextUtils.isEmpty(username)) {
            usernameInputLayout.setError("Username is required.");
            Toast.makeText(this, "Username is required.", Toast.LENGTH_SHORT).show();
            cancel = true;
        }

        if (TextUtils.isEmpty(address)) {
            addressInputLayout.setError("Address is required.");
            Toast.makeText(this, "Address is required.", Toast.LENGTH_SHORT).show();
            cancel = true;
        }

        if (TextUtils.isEmpty(mobileNumber)) {
            mobileNumberInputLayout.setError("Mobile Number is required.");
            Toast.makeText(this, "Mobile Number is required.", Toast.LENGTH_SHORT).show();
            cancel = true;
        } else {
            if (mobileNumber.startsWith("+94")) {
                if (mobileNumber.length() != 12) {
                    mobileNumberInputLayout.setError("Mobile number starting with '+94' must be 12 characters long (e.g., +94712345678).");
                    Toast.makeText(this, "Mobile number starting with '+94' must be 12 characters long.", Toast.LENGTH_LONG).show();
                    cancel = true;
                } else if (!Pattern.matches("\\+\\d{12}", mobileNumber)) {
                    mobileNumberInputLayout.setError("Please enter a valid 12-character mobile number starting with Country code.");
                    Toast.makeText(this, "Invalid mobile number format with Country code", Toast.LENGTH_LONG).show();
                    cancel = true;
                }
            } else if (mobileNumber.startsWith("07")) {
                if (mobileNumber.length() != 10) {
                    mobileNumberInputLayout.setError("Mobile number starting with '07' must be 10 characters long.");
                    Toast.makeText(this, "Mobile number starting with '07' must be 10 characters long.", Toast.LENGTH_LONG).show();
                    cancel = true;
                } else if (!Pattern.matches("07\\d{8}", mobileNumber)) {
                    mobileNumberInputLayout.setError("Please enter a valid 10-digit mobile number starting with '07'.");
                    Toast.makeText(this, "Invalid mobile number format for 07.", Toast.LENGTH_LONG).show();
                    cancel = true;
                }
            } else {
                mobileNumberInputLayout.setError("Mobile number must start with '+94' or '07'.");
                Toast.makeText(this, "Mobile number must start with '+94' or '07'.", Toast.LENGTH_LONG).show();
                cancel = true;
            }
        }

        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError("Email is required.");
            Toast.makeText(this, "Email is required.", Toast.LENGTH_SHORT).show();
            cancel = true;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError("Please enter a valid email address.");
            Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_LONG).show();
            cancel = true;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInputLayout.setError("Password is required.");
            Toast.makeText(this, "Password is required.", Toast.LENGTH_SHORT).show();
            cancel = true;
        } else if (!isPasswordValid(password)) {
            cancel = true;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordInputLayout.setError("Please confirm your password.");
            Toast.makeText(this, "Please confirm your password.", Toast.LENGTH_SHORT).show();
            cancel = true;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordInputLayout.setError("Passwords don't match. Please try again.");
            Toast.makeText(this, "Passwords don't match. Please try again.", Toast.LENGTH_LONG).show();
            cancel = true;
        }

        if (cancel) {
            return;
        } else {
            progressBar.setVisibility(View.VISIBLE);
            checkEmailExistenceAndRegister(email, password, username, address, mobileNumber);
        }
    }

    private boolean isPasswordValid(String password) {
        passwordInputLayout.setError(null);

        if (password.length() < 8) {
            passwordInputLayout.setError("Password must be at least 8 characters long.");
            Toast.makeText(this, "Password must be at least 8 characters long.", Toast.LENGTH_LONG).show();
            return false;
        }
        if (!password.matches(".*[A-Z].*")) {
            passwordInputLayout.setError("Password needs at least one uppercase letter.");
            Toast.makeText(this, "Password needs at least one uppercase letter.", Toast.LENGTH_LONG).show();
            return false;
        }
        if (!password.matches(".*[a-z].*")) {
            passwordInputLayout.setError("Password needs at least one lowercase letter.");
            Toast.makeText(this, "Password needs at least one lowercase letter.", Toast.LENGTH_LONG).show();
            return false;
        }
        if (!password.matches(".*\\d.*")) {
            passwordInputLayout.setError("Password needs at least one digit.");
            Toast.makeText(this, "Password needs at least one digit.", Toast.LENGTH_LONG).show();
            return false;
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            passwordInputLayout.setError("Password needs at least one special character.");
            Toast.makeText(this, "Password needs at least one special character.", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void checkEmailExistenceAndRegister(String email, String password, String username, String address, String mobileNumber) {
        mAuth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        boolean isNewUser = task.getResult().getSignInMethods().isEmpty();
                        if (isNewUser) {
                            registerUserInFirebase(email, password, username, address, mobileNumber);
                        } else {
                            progressBar.setVisibility(View.GONE);
                            emailInputLayout.setError("This email address is already registered.");
                            Toast.makeText(SignUp.this, "This email address is already registered. Please sign in.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Error checking email existence: " + task.getException());
                        Toast.makeText(SignUp.this, "Error checking email: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void registerUserInFirebase(String email, String password, String username, String address, String mobileNumber) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE);

                        if (task.isSuccessful()) {
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();

                            if (user != null) {
                                // Send email verification
                                user.sendEmailVerification()
                                        .addOnCompleteListener(task1 -> {
                                            if (task1.isSuccessful()) {
                                                Log.d(TAG, "Email verification sent.");
                                                Toast.makeText(SignUp.this, "Registration successful. Verification email sent to " + user.getEmail() + ". Please verify your email before signing in.", Toast.LENGTH_LONG).show();
                                                // Only save user data to Firestore after email verification is sent
                                                saveUserDataToFirestore(user.getUid(), username, address, mobileNumber, email, DEFAULT_COUNTRY);
                                                // Navigate to SignIn activity, user needs to verify email first
                                                Intent intent = new Intent(SignUp.this, SignIn.class);
                                                startActivity(intent);
                                                finish(); // Prevent going back to SignUp
                                            } else {
                                                Log.e(TAG, "Failed to send verification email.", task1.getException());
                                                Toast.makeText(SignUp.this, "Registration successful, but failed to send verification email. Please try signing in later or contact support.", Toast.LENGTH_LONG).show();
                                                // Even if email sending fails, we might still want to save user data if account was created
                                                saveUserDataToFirestore(user.getUid(), username, address, mobileNumber, email, DEFAULT_COUNTRY);
                                                // Still navigate to SignIn, as the account exists
                                                Intent intent = new Intent(SignUp.this, SignIn.class);
                                                startActivity(intent);
                                                finish();
                                            }
                                        });

                            }
                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            String errorMessage = "Registration failed.";
                            if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                errorMessage = "This email address is already registered. Please sign in.";
                            } else if (task.getException() != null) {
                                errorMessage += " " + task.getException().getMessage();
                            }
                            Toast.makeText(SignUp.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void saveUserDataToFirestore(String userId, String username, String address, String mobileNumber, String email, String country) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("username", username);
        userMap.put("email", email);
        userMap.put("address", address);
        userMap.put("mobileNumber", mobileNumber);
        userMap.put("createdAt", FieldValue.serverTimestamp());
        userMap.put("country", country);
        userMap.put("author", false); // Default value

        db.collection("users").document(userId)
                .set(userMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User data saved to Firestore successfully!"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving user data to Firestore", e));
    }
}