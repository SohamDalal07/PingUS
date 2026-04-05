package com.example.backtoyou;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import android.content.Intent;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Login_page extends AppCompatActivity {

    private View mainRoot;
    private TextInputEditText emailSignIn, passwordSignIn;
    private TextInputLayout emailLayoutSignIn, passwordLayoutSignIn;
    private CheckBox keepSignedIn;
    private TextView forgotPasswordLink, linkCreateAccount;
    private MaterialButton btnSignIn, btnUniversitySso;
    private FrameLayout signInForm, createAccountForm;

    private TextInputEditText fullName, emailSignUp, studentId, role, department, phone;
    private TextInputEditText passwordSignUp, confirmPassword;
    private TextInputLayout fullNameLayout, emailLayoutSignUp, studentIdLayout, roleLayout;
    private TextInputLayout departmentLayout, phoneLayout, passwordLayoutSignUp, confirmPasswordLayout;
    private CheckBox termsCheckbox;
    private MaterialButton btnCreateAccount;
    private TextView linkSignIn;

    private View pageDotLeft, pageDotMid, pageDotRight;

    private View strengthBar1, strengthBar2, strengthBar3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setUpListeners();
        switchToSignIn();
    }

    private void initializeViews() {
        mainRoot = findViewById(R.id.main);
        signInForm = findViewById(R.id.sign_in_form);
        createAccountForm = findViewById(R.id.create_account_form);

        pageDotLeft = findViewById(R.id.page_dot_left);
        pageDotMid = findViewById(R.id.page_dot_mid);
        pageDotRight = findViewById(R.id.page_dot_right);

        emailSignIn = findViewById(R.id.email_signin);
        passwordSignIn = findViewById(R.id.password_signin);
        emailLayoutSignIn = findViewById(R.id.email_input_layout_signin);
        passwordLayoutSignIn = findViewById(R.id.password_input_layout_signin);
        keepSignedIn = findViewById(R.id.keep_signed_in);
        forgotPasswordLink = findViewById(R.id.forgot_password_link);
        btnSignIn = findViewById(R.id.btn_sign_in);
        btnUniversitySso = findViewById(R.id.btn_university_sso);
        linkCreateAccount = findViewById(R.id.link_create_account);

        fullName = findViewById(R.id.fullname);
        emailSignUp = findViewById(R.id.email_signup);
        studentId = findViewById(R.id.student_id);
        role = findViewById(R.id.role);
        department = findViewById(R.id.department);
        phone = findViewById(R.id.phone);
        passwordSignUp = findViewById(R.id.password_signup);
        confirmPassword = findViewById(R.id.confirm_password);
        fullNameLayout = findViewById(R.id.fullname_input_layout);
        emailLayoutSignUp = findViewById(R.id.email_input_layout_signup);
        studentIdLayout = findViewById(R.id.student_id_input_layout);
        roleLayout = findViewById(R.id.role_input_layout);
        departmentLayout = findViewById(R.id.department_input_layout);
        phoneLayout = findViewById(R.id.phone_input_layout);
        passwordLayoutSignUp = findViewById(R.id.password_input_layout_signup);
        confirmPasswordLayout = findViewById(R.id.confirm_password_input_layout);
        termsCheckbox = findViewById(R.id.terms_checkbox);
        btnCreateAccount = findViewById(R.id.btn_create_account);
        linkSignIn = findViewById(R.id.link_sign_in);

        strengthBar1 = findViewById(R.id.strength_bar_1);
        strengthBar2 = findViewById(R.id.strength_bar_2);
        strengthBar3 = findViewById(R.id.strength_bar_3);
    }

    private void setUpListeners() {
        pageDotLeft.setOnClickListener(v -> switchToSignIn());
        pageDotMid.setOnClickListener(v -> switchToSignIn());
        pageDotRight.setOnClickListener(v -> switchToCreateAccount());

        btnSignIn.setOnClickListener(v -> handleSignIn());
        btnUniversitySso.setOnClickListener(v -> handleSSO());
        forgotPasswordLink.setOnClickListener(v -> handleForgotPassword());
        linkCreateAccount.setOnClickListener(v -> switchToCreateAccount());

        btnCreateAccount.setOnClickListener(v -> handleCreateAccount());
        linkSignIn.setOnClickListener(v -> switchToSignIn());

        passwordSignUp.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePasswordStrength(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void applyStatusBarAppearance(boolean lightStatusBarIcons) {
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        c.setAppearanceLightStatusBars(lightStatusBarIcons);
    }

    private void updatePageDots(boolean signupMode) {
        int dimLogin = ContextCompat.getColor(this, R.color.auth_dot_inactive_login);
        int dimSignup = ContextCompat.getColor(this, R.color.auth_dot_inactive_signup);
        int selected = Color.WHITE;

        if (!signupMode) {
            pageDotLeft.setBackgroundTintList(android.content.res.ColorStateList.valueOf(dimLogin));
            pageDotMid.setBackgroundTintList(android.content.res.ColorStateList.valueOf(selected));
            pageDotRight.setBackgroundTintList(android.content.res.ColorStateList.valueOf(dimLogin));
        } else {
            pageDotLeft.setBackgroundTintList(android.content.res.ColorStateList.valueOf(dimSignup));
            pageDotMid.setBackgroundTintList(android.content.res.ColorStateList.valueOf(dimSignup));
            pageDotRight.setBackgroundTintList(android.content.res.ColorStateList.valueOf(selected));
        }
    }

    private void switchToSignIn() {
        signInForm.setVisibility(View.VISIBLE);
        createAccountForm.setVisibility(View.GONE);
        mainRoot.setBackgroundColor(ContextCompat.getColor(this, R.color.auth_login_screen_bg));
        updatePageDots(false);
        applyStatusBarAppearance(false);
    }

    private void switchToCreateAccount() {
        signInForm.setVisibility(View.GONE);
        createAccountForm.setVisibility(View.VISIBLE);
        mainRoot.setBackgroundColor(ContextCompat.getColor(this, R.color.auth_signup_screen_bg));
        updatePageDots(true);
        applyStatusBarAppearance(true);
    }

    private void handleSignIn() {
        String email = emailSignIn.getText().toString().trim();
        String password = passwordSignIn.getText().toString().trim();

        boolean isValid = true;

        if (email.isEmpty()) {
            emailLayoutSignIn.setError("Email is required");
            isValid = false;
        } else if (!email.endsWith("@university.in") && !email.contains("@")) {
            emailLayoutSignIn.setError("Must be a valid university email");
            isValid = false;
        } else {
            emailLayoutSignIn.setError(null);
        }

        if (password.isEmpty()) {
            passwordLayoutSignIn.setError("Password is required");
            isValid = false;
        } else if (password.length() < 8) {
            passwordLayoutSignIn.setError("Password must be at least 8 characters");
            isValid = false;
        } else {
            passwordLayoutSignIn.setError(null);
        }

        if (isValid) {
            btnSignIn.setEnabled(false);
            btnSignIn.setText(R.string.sign_in_progress);

            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        btnSignIn.setEnabled(true);
                        btnSignIn.setText(R.string.auth_log_in);

                        if (task.isSuccessful()) {
                            Toast.makeText(Login_page.this, "Sign in successful!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(Login_page.this, Home.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(Login_page.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void handleSSO() {
        Toast.makeText(this, "University SSO login coming soon", Toast.LENGTH_SHORT).show();
    }

    private void handleForgotPassword() {
        Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show();
    }

    private void handleCreateAccount() {
        String fullNameText = fullName.getText().toString().trim();
        String emailText = emailSignUp.getText().toString().trim();
        String studentIdText = studentId.getText().toString().trim();
        String roleText = role.getText().toString().trim();
        String departmentText = department.getText().toString().trim();
        String passwordText = passwordSignUp.getText().toString().trim();
        String confirmPasswordText = confirmPassword.getText().toString().trim();

        boolean isValid = true;

        if (fullNameText.isEmpty()) {
            fullNameLayout.setError("Full name is required");
            isValid = false;
        } else {
            fullNameLayout.setError(null);
        }

        if (emailText.isEmpty()) {
            emailLayoutSignUp.setError("Email is required");
            isValid = false;
        } else if (!emailText.endsWith("@university.in") && !emailText.contains("@")) {
            emailLayoutSignUp.setError("Must be a valid university email");
            isValid = false;
        } else {
            emailLayoutSignUp.setError(null);
        }

        if (studentIdText.isEmpty()) {
            studentIdLayout.setError("Student/Staff ID is required");
            isValid = false;
        } else {
            studentIdLayout.setError(null);
        }

        if (roleText.isEmpty()) {
            roleLayout.setError("Role is required");
            isValid = false;
        } else {
            roleLayout.setError(null);
        }

        if (departmentText.isEmpty()) {
            departmentLayout.setError("Department is required");
            isValid = false;
        } else {
            departmentLayout.setError(null);
        }

        if (phone.getText().toString().trim().isEmpty()) {
            phoneLayout.setError("Phone number is required");
            isValid = false;
        } else {
            phoneLayout.setError(null);
        }

        if (passwordText.isEmpty()) {
            passwordLayoutSignUp.setError("Password is required");
            isValid = false;
        } else if (!isPasswordStrong(passwordText)) {
            passwordLayoutSignUp.setError("Password must be 8+ chars with uppercase and number");
            isValid = false;
        } else {
            passwordLayoutSignUp.setError(null);
        }

        if (confirmPasswordText.isEmpty()) {
            confirmPasswordLayout.setError("Please confirm password");
            isValid = false;
        } else if (!confirmPasswordText.equals(passwordText)) {
            confirmPasswordLayout.setError("Passwords do not match");
            isValid = false;
        } else {
            confirmPasswordLayout.setError(null);
        }

        if (!termsCheckbox.isChecked()) {
            Toast.makeText(this, "Please agree to terms and conditions", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (isValid) {
            btnCreateAccount.setEnabled(false);
            btnCreateAccount.setText(R.string.create_account_progress);

            FirebaseAuth.getInstance().createUserWithEmailAndPassword(emailText, passwordText)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if (user != null) {
                                String uid = user.getUid();
                                Map<String, Object> userMap = new HashMap<>();
                                userMap.put("fullName", fullNameText);
                                userMap.put("email", emailText);
                                userMap.put("studentId", studentIdText);
                                userMap.put("role", roleText);
                                userMap.put("department", departmentText);
                                userMap.put("phone", phone.getText().toString().trim());
                                userMap.put("uid", uid);

                                FirebaseFirestore.getInstance().collection("users")
                                        .document(uid).set(userMap)
                                        .addOnCompleteListener(dbTask -> {
                                            btnCreateAccount.setEnabled(true);
                                            btnCreateAccount.setText(R.string.auth_sign_up);

                                            if (dbTask.isSuccessful()) {
                                                showEmailVerificationDialog();
                                                clearCreateAccountForm();
                                            } else {
                                                Toast.makeText(Login_page.this, "Failed to store user data.", Toast.LENGTH_LONG).show();
                                            }
                                        });
                            }
                        } else {
                            btnCreateAccount.setEnabled(true);
                            btnCreateAccount.setText(R.string.auth_sign_up);
                            Toast.makeText(Login_page.this, "Account Creation Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private boolean isPasswordStrong(String password) {
        if (password.length() < 8) return false;
        if (!password.matches(".*[A-Z].*")) return false;
        if (!password.matches(".*[0-9].*")) return false;
        return true;
    }

    private void updatePasswordStrength(String password) {
        int strength = 0;

        if (password.length() >= 8) strength++;
        if (password.matches(".*[A-Z].*")) strength++;
        if (password.matches(".*[0-9].*")) strength++;

        resetStrengthBars();

        if (strength > 0) {
            strengthBar1.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getColor(strength == 1 ? R.color.error_red : R.color.success_green)));
        }
        if (strength > 1) {
            strengthBar2.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getColor(strength == 2 ? R.color.text_gray : R.color.success_green)));
        }
        if (strength > 2) {
            strengthBar3.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getColor(R.color.success_green)));
        }
    }

    private void resetStrengthBars() {
        strengthBar1.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                getColor(R.color.light_gray)));
        strengthBar2.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                getColor(R.color.light_gray)));
        strengthBar3.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                getColor(R.color.light_gray)));
    }

    private void showEmailVerificationDialog() {
        Toast.makeText(this, "Account created! Please verify your email.", Toast.LENGTH_LONG).show();
        switchToSignIn();
    }

    private void clearCreateAccountForm() {
        fullName.setText("");
        emailSignUp.setText("");
        studentId.setText("");
        role.setText("");
        department.setText("");
        phone.setText("");
        passwordSignUp.setText("");
        confirmPassword.setText("");
        termsCheckbox.setChecked(false);
    }
}
