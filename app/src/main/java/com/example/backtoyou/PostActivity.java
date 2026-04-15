package com.example.backtoyou;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class PostActivity extends AppCompatActivity {

    private Button btnPostReport;
    private EditText etItemName, etDescription;
    private Spinner spinnerLocation, spinnerCategory;
    private LinearLayout layoutFound, layoutLost, layoutPhotoTap;
    private ImageView ivPhotoPreview;
    private BottomNavigationView bottomNavigation;

    private String selectedType = "FOUND";
    private String securityColor = "", securityBrand = "", securityMark = "";
    private Uri selectedImageUri;
    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        bindViews();
        setupToolbar();
        setupTypeSelectors();
        setupSpinners();
        setupPhotoPicker();
        setupPostButton();
        setupBottomNavigation();
    }

    private void bindViews() {
        btnPostReport = findViewById(R.id.btnPostReport);
        etItemName = findViewById(R.id.etItemName);
        etDescription = findViewById(R.id.etDescription);
        spinnerLocation = findViewById(R.id.spinnerLocation);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        layoutFound = findViewById(R.id.layoutFound);
        layoutLost = findViewById(R.id.layoutLost);
        layoutPhotoTap = findViewById(R.id.layoutPhotoTap);
        ivPhotoPreview = findViewById(R.id.ivPhotoPreview);
        bottomNavigation = findViewById(R.id.bottom_navigation);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back_black);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupTypeSelectors() {
        layoutLost.setOnClickListener(v -> {
            selectedType = "LOST";
            layoutLost.setBackgroundResource(R.drawable.bg_post_type_lost);
            layoutFound.setBackgroundResource(R.drawable.bg_home_stat_card);
            updatePhotoSectionForType();
        });

        layoutFound.setOnClickListener(v -> {
            selectedType = "FOUND";
            layoutFound.setBackgroundResource(R.drawable.bg_post_type_found);
            layoutLost.setBackgroundResource(R.drawable.bg_home_stat_card);
            updatePhotoSectionForType();
        });
        
        layoutFound.performClick();
    }

    private void setupPhotoPicker() {
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null) return;
            selectedImageUri = uri;
            if (ivPhotoPreview != null) {
                ivPhotoPreview.setVisibility(View.VISIBLE);
                Glide.with(this).load(uri).centerCrop().into(ivPhotoPreview);
            }
        });

        if (layoutPhotoTap != null) {
            layoutPhotoTap.setOnClickListener(v -> {
                if (!"LOST".equals(selectedType)) {
                    Toast.makeText(this, "Image upload is available only for 'I Lost It'.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (pickImageLauncher != null) pickImageLauncher.launch("image/*");
            });
        }
    }

    private void updatePhotoSectionForType() {
        if (layoutPhotoTap == null) return;
        boolean showPhotoSection = "LOST".equals(selectedType);
        layoutPhotoTap.setVisibility(showPhotoSection ? View.VISIBLE : View.GONE);
        if (!showPhotoSection) {
            selectedImageUri = null;
            if (ivPhotoPreview != null) {
                ivPhotoPreview.setImageDrawable(null);
                ivPhotoPreview.setVisibility(View.GONE);
            }
        }
    }

    private void setupSpinners() {
        String[] locations = getResources().getStringArray(R.array.locations_campus);
        ArrayAdapter<String> locAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, locations);
        locAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLocation.setAdapter(locAdapter);

        String[] categories = getResources().getStringArray(R.array.categories_items);
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);
    }

    private void setupPostButton() {
        btnPostReport.setOnClickListener(v -> validateAndShowSecurityDialog());
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.navigation_post);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                startActivity(new Intent(this, Home.class));
                finish();
                return true;
            } else if (itemId == R.id.navigation_alerts) {
                startActivity(new Intent(this, AlertsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.navigation_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            }
            return true;
        });
    }

    private void validateAndShowSecurityDialog() {
        String itemName = etItemName.getText().toString().trim();
        if (TextUtils.isEmpty(itemName)) {
            etItemName.setError("Required");
            return;
        }
        if (spinnerCategory.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }
        if (spinnerLocation.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show();
            return;
        }
        showSecurityDialog(itemName);
    }

    private void showSecurityDialog(String itemName) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_security_questions, null);
        EditText etColor = dialogView.findViewById(R.id.etSecurityColor);
        EditText etBrand = dialogView.findViewById(R.id.etSecurityBrand);
        EditText etMark = dialogView.findViewById(R.id.etSecurityMark);

        new AlertDialog.Builder(this)
                .setTitle("Verify your item")
                .setView(dialogView)
                .setPositiveButton("Submit & Post", (dialog, which) -> {
                    securityColor = etColor.getText().toString().trim();
                    securityBrand = etBrand.getText().toString().trim();
                    securityMark = etMark.getText().toString().trim();
                    postToFirestore(itemName);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void postToFirestore(String itemName) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = user != null ? user.getUid() : "test_user";

        btnPostReport.setEnabled(false);
        resolvePosterName(uid, user, resolvedName -> {
            if ("LOST".equals(selectedType) && selectedImageUri != null) {
                uploadLostImageThenPost(itemName, uid, resolvedName);
            } else {
                postItem(itemName, uid, resolvedName, "");
            }
        });
    }

    private void resolvePosterName(String uid, FirebaseUser user, PosterNameCallback callback) {
        FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "lf26").collection("users").document(uid).get()
                .addOnSuccessListener(doc -> callback.onResolved(extractBestName(doc, user)))
                .addOnFailureListener(e -> callback.onResolved(fallbackName(user)));
    }

    private String extractBestName(DocumentSnapshot userDoc, FirebaseUser user) {
        if (userDoc != null && userDoc.exists()) {
            String fullName = userDoc.getString("fullName");
            if (fullName != null && !fullName.trim().isEmpty()) return fullName.trim();
            String name = userDoc.getString("name");
            if (name != null && !name.trim().isEmpty()) return name.trim();
        }
        return fallbackName(user);
    }

    private String fallbackName(FirebaseUser user) {
        if (user != null) {
            String display = user.getDisplayName();
            if (display != null && !display.trim().isEmpty()) return display.trim();
            String email = user.getEmail();
            if (email != null && email.contains("@")) return email.substring(0, email.indexOf('@'));
        }
        return "User";
    }

    private void uploadLostImageThenPost(String itemName, String uid, String name) {
        StorageReference fileRef = FirebaseStorage.getInstance()
                .getReference()
                .child("lost_reports")
                .child(uid)
                .child(System.currentTimeMillis() + ".jpg");
        fileRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot ->
                        fileRef.getDownloadUrl()
                                .addOnSuccessListener(uri ->
                                        postItem(itemName, uid, name, uri.toString()))
                                .addOnFailureListener(e -> {
                                    btnPostReport.setEnabled(true);
                                    Toast.makeText(this, "Could not fetch uploaded image URL.", Toast.LENGTH_SHORT).show();
                                }))
                .addOnFailureListener(e -> {
                    btnPostReport.setEnabled(true);
                    Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void postItem(String itemName, String uid, String name, String photoUrl) {

        Map<String, Object> item = new HashMap<>();
        item.put("title", itemName);
        item.put("type", selectedType);
        item.put("category", spinnerCategory.getSelectedItem().toString());
        item.put("location", spinnerLocation.getSelectedItem().toString());
        item.put("description", etDescription.getText().toString().trim());
        item.put("postedByUid", uid);
        item.put("postedByName", name);
        item.put("postedAt", System.currentTimeMillis());
        item.put("status", "ACTIVE");
        item.put("securityColor", securityColor);
        item.put("securityBrand", securityBrand);
        item.put("securityMark", securityMark);
        item.put("photoUrl", photoUrl);

        FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "lf26").collection("items").add(item)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Posted successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnPostReport.setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private interface PosterNameCallback {
        void onResolved(String name);
    }
}
