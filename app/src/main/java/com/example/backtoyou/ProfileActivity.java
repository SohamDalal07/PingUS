package com.example.backtoyou;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.backtoyou.databinding.ActivityProfileBinding;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private UserProfile currentProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        currentProfile = new UserProfile(
                "",
                "",
                "",
                "",
                "",
                "",
                0,
                0
        );
        loadUserProfile();
        setupClicks();
        setupBottomNavigation();
    }

    private void bindProfile(@NonNull UserProfile userProfile) {
        binding.tvAvatarInitial.setText(extractInitial(userProfile.getName()));
        binding.tvProfileName.setText(userProfile.getName());
        binding.tvProfileEmail.setText(userProfile.getEmail());
        binding.chipRole.setText(userProfile.getRole());
        binding.chipId.setText(userProfile.getStudentId());
        binding.tvPostsCount.setText(String.valueOf(userProfile.getPostsCount()));
        binding.tvAlertsCount.setText(String.valueOf(userProfile.getAlertsCount()));
        binding.tvDeptCode.setText(userProfile.getDepartmentShortCode());
        binding.tvRoleValue.setText(userProfile.getRole());
        binding.tvStudentIdValue.setText(userProfile.getStudentId());
        binding.tvDepartmentValue.setText(userProfile.getDepartment());
        binding.tvPhoneValue.setText(userProfile.getPhoneNumber());
    }

    private void setupClicks() {
        binding.btnEditProfile.setOnClickListener(v ->
                Toast.makeText(this, "Edit profile coming soon", Toast.LENGTH_SHORT).show());

        binding.btnSignOut.setOnClickListener(v -> showSignOutDialog());

        binding.actionSearch.setOnClickListener(v ->
                startActivity(new Intent(this, Home.class)));
        binding.actionNotifications.setOnClickListener(v ->
                startActivity(new Intent(this, AlertsActivity.class)));
        binding.actionMore.setOnClickListener(v -> showMoreMenu());
        binding.ivPhoneEdit.setOnClickListener(v -> showPhoneEditDialog());
    }

    private void loadUserProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            signOutAndGoToLogin();
            return;
        }

        FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "lf26")
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        bindAuthFallbackProfile(user);
                        return;
                    }

                    currentProfile = new UserProfile(
                            valueOrFallback(document.getString("fullName"), ""),
                            valueOrFallback(document.getString("email"), user.getEmail()),
                            valueOrFallback(document.getString("role"), ""),
                            valueOrFallback(document.getString("studentId"), ""),
                            valueOrFallback(document.getString("department"), ""),
                            valueOrFallback(document.getString("phone"), "Not available"),
                            0,
                            0
                    );
                    bindProfile(currentProfile);
                })
                .addOnFailureListener(e -> bindAuthFallbackProfile(user));
    }

    private void bindAuthFallbackProfile(@NonNull FirebaseUser user) {
        String email = valueOrFallback(user.getEmail(), "");
        String displayName = valueOrFallback(user.getDisplayName(),
                email.contains("@") ? email.substring(0, email.indexOf('@')) : "PingUS User");
        currentProfile = new UserProfile(
                displayName,
                email,
                valueOrFallback(currentProfile.getRole(), "Member"),
                valueOrFallback(currentProfile.getStudentId(), "Not available"),
                valueOrFallback(currentProfile.getDepartment(), "Not available"),
                valueOrFallback(currentProfile.getPhoneNumber(), "Not available"),
                currentProfile.getPostsCount(),
                currentProfile.getAlertsCount()
        );
        bindProfile(currentProfile);
        ensureProfileDocument(user, currentProfile);
        Toast.makeText(this, "Profile loaded from account session.", Toast.LENGTH_SHORT).show();
    }

    private void ensureProfileDocument(@NonNull FirebaseUser user, @NonNull UserProfile profile) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("uid", user.getUid());
        userMap.put("fullName", valueOrFallback(profile.getName(), "PingUS User"));
        userMap.put("email", valueOrFallback(profile.getEmail(), valueOrFallback(user.getEmail(), "")));
        userMap.put("role", valueOrFallback(profile.getRole(), "Member"));
        userMap.put("studentId", valueOrFallback(profile.getStudentId(), "Not available"));
        userMap.put("department", valueOrFallback(profile.getDepartment(), "Not available"));
        userMap.put("phone", valueOrFallback(profile.getPhoneNumber(), "Not available"));

        FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "lf26")
                .collection("users")
                .document(user.getUid())
                .set(userMap, SetOptions.merge());
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setSelectedItemId(R.id.navigation_profile);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Intent intent;

            if (itemId == R.id.navigation_home) {
                intent = new Intent(this, Home.class);
            } else if (itemId == R.id.navigation_post) {
                intent = new Intent(this, PostActivity.class);
            } else if (itemId == R.id.navigation_alerts) {
                intent = new Intent(this, AlertsActivity.class);
            } else if (itemId == R.id.navigation_profile) {
                return true;
            } else {
                return false;
            }

            startActivity(intent);
            return true;
        });
    }

    private String extractInitial(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "P";
        }
        return String.valueOf(Character.toUpperCase(name.trim().charAt(0)));
    }

    private String valueOrFallback(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private void showPhoneEditDialog() {
        final EditText phoneInput = new EditText(this);
        phoneInput.setText(binding.tvPhoneValue.getText());
        phoneInput.setHint("Enter phone number");
        phoneInput.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        phoneInput.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle("Change phone number")
                .setView(phoneInput)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String updatedPhone = phoneInput.getText().toString().trim();
                    if (updatedPhone.isEmpty()) {
                        updatedPhone = "Not available";
                    }
                    binding.tvPhoneValue.setText(updatedPhone);
                    currentProfile = new UserProfile(
                            currentProfile.getName(),
                            currentProfile.getEmail(),
                            currentProfile.getRole(),
                            currentProfile.getStudentId(),
                            currentProfile.getDepartment(),
                            updatedPhone,
                            currentProfile.getPostsCount(),
                            currentProfile.getAlertsCount()
                    );
                    persistPhoneNumber(updatedPhone);
                    Toast.makeText(this, "Phone number updated", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void persistPhoneNumber(String updatedPhone) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "lf26")
                .collection("users")
                .document(user.getUid())
                .update("phone", updatedPhone);
    }

    private void showMoreMenu() {
        PopupMenu popupMenu = new PopupMenu(this, binding.actionMore);
        popupMenu.getMenu().add(0, 1, 0, "Sign out");
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                showSignOutDialog();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void showSignOutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Sign out")
                .setMessage("Are you sure you want to sign out?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Sign out", (dialog, which) -> signOutAndGoToLogin())
                .show();
    }

    private void signOutAndGoToLogin() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, Login_page.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
