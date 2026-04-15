package com.example.backtoyou;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Bitmap;
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

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PostActivity extends AppCompatActivity {

    private Button btnPostReport;
    private EditText etItemName, etDescription, etPostDateTime;
    private Spinner spinnerLocation, spinnerCategory;
    private LinearLayout layoutFound, layoutLost, layoutPhotoTap;
    private ImageView ivPhotoPreview;
    private BottomNavigationView bottomNavigation;

    private String selectedType = "FOUND";
    private String securityColor = "", securityBrand = "", securityMark = "";
    private Uri selectedImageUri;
    private byte[] selectedImageBytes;
    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<Void> captureImageLauncher;
    private long selectedPostedAtMillis;
    private boolean forceFirstPost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        bindViews();
        setupToolbar();
        setupTypeSelectors();
        setupSpinners();
        setupDateTimePicker();
        setupPhotoPicker();
        setupPostButton();
        setupBottomNavigation();
        forceFirstPost = getIntent() != null && getIntent().getBooleanExtra("forceFirstPost", false);
    }

    private void bindViews() {
        btnPostReport = findViewById(R.id.btnPostReport);
        etItemName = findViewById(R.id.etItemName);
        etDescription = findViewById(R.id.etDescription);
        etPostDateTime = findViewById(R.id.etPostDateTime);
        spinnerLocation = findViewById(R.id.spinnerLocation);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        layoutFound = findViewById(R.id.layoutFound);
        layoutLost = findViewById(R.id.layoutLost);
        layoutPhotoTap = findViewById(R.id.layoutPhotoTap);
        ivPhotoPreview = findViewById(R.id.ivPhotoPreview);
        bottomNavigation = findViewById(R.id.bottom_navigation);
    }

    private void setupDateTimePicker() {
        selectedPostedAtMillis = System.currentTimeMillis();
        bindSelectedDateTime();
        if (etPostDateTime == null) return;
        etPostDateTime.setOnClickListener(v -> openDatePicker());
    }

    private void openDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(selectedPostedAtMillis);
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> openTimePicker(year, month, dayOfMonth),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void openTimePicker(int year, int month, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(selectedPostedAtMillis);
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(Calendar.YEAR, year);
                    picked.set(Calendar.MONTH, month);
                    picked.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    picked.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    picked.set(Calendar.MINUTE, minute);
                    picked.set(Calendar.SECOND, 0);
                    picked.set(Calendar.MILLISECOND, 0);
                    selectedPostedAtMillis = picked.getTimeInMillis();
                    bindSelectedDateTime();
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
        );
        timePickerDialog.show();
    }

    private void bindSelectedDateTime() {
        if (etPostDateTime == null) return;
        SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        etPostDateTime.setText(format.format(new Date(selectedPostedAtMillis)));
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
            selectedImageBytes = null;
            if (ivPhotoPreview != null) {
                ivPhotoPreview.setVisibility(View.VISIBLE);
                Glide.with(this).load(uri).centerCrop().into(ivPhotoPreview);
            }
        });

        captureImageLauncher = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
            if (bitmap == null) return;
            selectedImageUri = null;
            selectedImageBytes = bitmapToJpegBytes(bitmap);
            if (ivPhotoPreview != null) {
                ivPhotoPreview.setVisibility(View.VISIBLE);
                ivPhotoPreview.setImageBitmap(bitmap);
            }
        });

        if (layoutPhotoTap != null) {
            layoutPhotoTap.setOnClickListener(v -> showImageSourceDialog());
        }
    }

    private void showImageSourceDialog() {
        String[] options = {"Upload from gallery", "Click from camera"};
        new AlertDialog.Builder(this)
                .setTitle("Add item photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        if (pickImageLauncher != null) pickImageLauncher.launch("image/*");
                    } else if (which == 1) {
                        if (captureImageLauncher != null) captureImageLauncher.launch(null);
                    }
                })
                .show();
    }

    private void updatePhotoSectionForType() {
        if (layoutPhotoTap == null) return;
        // Photo evidence is supported for both LOST and FOUND flows.
        layoutPhotoTap.setVisibility(View.VISIBLE);
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
                if (forceFirstPost) {
                    Toast.makeText(this, "Please submit your first post before opening Home.", Toast.LENGTH_SHORT).show();
                    return true;
                }
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
            if (selectedImageUri != null || selectedImageBytes != null) {
                uploadImageThenPost(itemName, uid, resolvedName);
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

    private void uploadImageThenPost(String itemName, String uid, String name) {
        StorageReference fileRef = FirebaseStorage.getInstance()
                .getReference()
                .child("item_reports")
                .child(selectedType.toLowerCase())
                .child(uid)
                .child(System.currentTimeMillis() + ".jpg");
        if (selectedImageUri != null) {
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
            return;
        }

        if (selectedImageBytes != null) {
            fileRef.putBytes(selectedImageBytes)
                    .addOnSuccessListener(taskSnapshot ->
                            fileRef.getDownloadUrl()
                                    .addOnSuccessListener(uri ->
                                            postItem(itemName, uid, name, uri.toString()))
                                    .addOnFailureListener(e -> {
                                        btnPostReport.setEnabled(true);
                                        Toast.makeText(this, "Could not fetch captured image URL.", Toast.LENGTH_SHORT).show();
                                    }))
                    .addOnFailureListener(e -> {
                        btnPostReport.setEnabled(true);
                        Toast.makeText(this, "Camera image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private byte[] bitmapToJpegBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        return stream.toByteArray();
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
        item.put("postedAt", selectedPostedAtMillis);
        item.put("status", "ACTIVE");
        item.put("securityColor", securityColor);
        item.put("securityBrand", securityBrand);
        item.put("securityMark", securityMark);
        item.put("photoUrl", photoUrl);

        FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "lf26").collection("items").add(item)
                .addOnSuccessListener(documentReference -> {
                    markFirstPostCompletedAndFinish(uid);
                })
                .addOnFailureListener(e -> {
                    btnPostReport.setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void markFirstPostCompletedAndFinish(String uid) {
        FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "lf26")
                .collection("users")
                .document(uid)
                .update("hasPostedFirstItem", true)
                .addOnSuccessListener(unused -> finishPostFlow())
                .addOnFailureListener(e -> finishPostFlow());
    }

    private void finishPostFlow() {
        Toast.makeText(this, "Posted successfully!", Toast.LENGTH_SHORT).show();
        if (forceFirstPost) {
            startActivity(new Intent(this, Home.class));
        }
        finish();
    }

    private interface PosterNameCallback {
        void onResolved(String name);
    }
}
