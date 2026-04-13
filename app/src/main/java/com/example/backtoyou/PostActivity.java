package com.example.backtoyou;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class PostActivity extends AppCompatActivity {

    // ── Views ──
    private Button   btnPostReport;
    private EditText etItemName, etDescription;
    private RadioButton rbLost, rbFound;
    private RadioGroup rgType;
    private Spinner  spinnerPersonal, spinnerAcademic, spinnerLocation;
    private TextView tvPersonalLabel, tvAcademicLabel;
    private Button   btnSelectImage;
    private ImageView ivSelectedImage;
    private android.widget.LinearLayout layoutImageSection;

    // ── Firebase Storage (via ImageStorageConfig) ──
    private StorageReference storageReference;
    private Uri selectedImageUri;
    private String uploadedImageUrl = "";

    // ── State ──
    private String selectedType     = "LOST";
    private String selectedCategory = "";
    private String selectedLocation = "";

    // which spinner was actively chosen by user
    private boolean personalTouched = false;
    private boolean academicTouched = false;

    // security question answers — stored privately, never shown on feed
    private String securityColor = "";
    private String securityBrand = "";
    private String securityMark  = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        bindViews();
        setupToolbar();
        setupToggle();
        setupSpinners();
        setupPostButton();
        setupImagePicker();
        // Initialize Firebase Storage using the default app (Project B)
        storageReference = com.google.firebase.storage.FirebaseStorage.getInstance().getReference().child("images");
    }

    // ─────────────────────────────────────────
    // 1. Bind views
    // ─────────────────────────────────────────
    private void bindViews() {
        btnPostReport   = findViewById(R.id.btnPostReport);
        etItemName      = findViewById(R.id.etItemName);
        etDescription   = findViewById(R.id.etDescription);
        rbLost          = findViewById(R.id.rb_lost);
        rbFound         = findViewById(R.id.rb_found);
        rgType          = findViewById(R.id.rg_type);
        spinnerPersonal = findViewById(R.id.spinnerPersonal);
        spinnerAcademic = findViewById(R.id.spinnerAcademic);
        spinnerLocation = findViewById(R.id.spinnerLocation);
        tvPersonalLabel = findViewById(R.id.tvPersonalLabel);
        tvAcademicLabel = findViewById(R.id.tvAcademicLabel);
        btnSelectImage  = findViewById(R.id.btnSelectImage);
        ivSelectedImage = findViewById(R.id.ivSelectedImage);
        layoutImageSection = findViewById(R.id.layoutImageSection);

        // hint color set in Java — android:hintTextColor not valid on plain EditText in XML
        int hintColor = ContextCompat.getColor(this, R.color.colorTextHint);
        etItemName.setHintTextColor(hintColor);
        etDescription.setHintTextColor(hintColor);
    }

    // ─────────────────────────────────────────
    // 2. Toolbar
    // ─────────────────────────────────────────
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Report an Item");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    // ─────────────────────────────────────────
    // 3. Lost / Found toggle
    // ─────────────────────────────────────────
    private void setupToggle() {
        rgType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_lost) {
                setType("LOST");
            } else if (checkedId == R.id.rb_found) {
                setType("FOUND");
            }
        });
        rgType.check(R.id.rb_lost);
    }

    private void setType(String type) {
        selectedType = type;
        if ("LOST".equals(type)) {
            rbLost.setTextColor(ContextCompat.getColor(this, R.color.white));
            rbLost.setBackgroundResource(R.drawable.seg_lost);
            rbFound.setTextColor(ContextCompat.getColor(this, R.color.home_hint_text));
            rbFound.setBackgroundResource(R.drawable.seg_unselected);
            // Show image upload only for LOST items
            layoutImageSection.setVisibility(View.VISIBLE);
        } else {
            rbFound.setTextColor(ContextCompat.getColor(this, R.color.white));
            rbFound.setBackgroundResource(R.drawable.seg_found);
            rbLost.setTextColor(ContextCompat.getColor(this, R.color.home_hint_text));
            rbLost.setBackgroundResource(R.drawable.seg_unselected);
            // Hide image upload for FOUND items and clear any chosen image
            layoutImageSection.setVisibility(View.GONE);
            uploadedImageUrl = "";
            selectedImageUri = null;
            ivSelectedImage.setVisibility(View.GONE);
            btnSelectImage.setText("📷 Select Image");
        }
    }

    // ─────────────────────────────────────────
    // 4. Spinners with mutual exclusion
    // ─────────────────────────────────────────
    private void setupSpinners() {

        String[] personal  = getResources().getStringArray(R.array.categories_personal);
        String[] academic  = getResources().getStringArray(R.array.categories_academic);
        String[] locations = getResources().getStringArray(R.array.locations_shirpur);

        // ── Personal spinner ──
        ArrayAdapter<String> adapterPersonal = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, personal);
        adapterPersonal.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPersonal.setAdapter(adapterPersonal);
        spinnerPersonal.setSelection(0, false);

        spinnerPersonal.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                if (!personalTouched) return;
                if (position == 0) {
                    selectedCategory = "";
                    enableSpinner(spinnerAcademic, tvAcademicLabel, true);
                } else {
                    selectedCategory = personal[position];
                    spinnerAcademic.setSelection(0, true);
                    academicTouched = false;
                    enableSpinner(spinnerAcademic, tvAcademicLabel, false);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerPersonal.setOnTouchListener((v, event) -> {
            personalTouched = true;
            v.performClick();
            return false;
        });

        // ── Academic spinner ──
        ArrayAdapter<String> adapterAcademic = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, academic);
        adapterAcademic.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAcademic.setAdapter(adapterAcademic);
        spinnerAcademic.setSelection(0, false);

        spinnerAcademic.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                if (!academicTouched) return;
                if (position == 0) {
                    selectedCategory = "";
                    enableSpinner(spinnerPersonal, tvPersonalLabel, true);
                } else {
                    selectedCategory = academic[position];
                    spinnerPersonal.setSelection(0, true);
                    personalTouched = false;
                    enableSpinner(spinnerPersonal, tvPersonalLabel, false);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerAcademic.setOnTouchListener((v, event) -> {
            academicTouched = true;
            v.performClick();
            return false;
        });

        // ── Location spinner ──
        ArrayAdapter<String> adapterLocation = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, locations);
        adapterLocation.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLocation.setAdapter(adapterLocation);
        spinnerLocation.setSelection(0, false);

        spinnerLocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                selectedLocation = (position == 0) ? "" : locations[position];
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // enable or disable a spinner + its label visually
    private void enableSpinner(Spinner spinner, TextView label, boolean enable) {
        spinner.setEnabled(enable);
        spinner.setAlpha(enable ? 1.0f : 0.4f);
        label.setAlpha(enable ? 1.0f : 0.4f);
    }

    // ─────────────────────────────────────────
    // 4. Image Picker Setup
    // ─────────────────────────────────────────
    private void setupImagePicker() {
        btnSelectImage.setOnClickListener(v -> openGallery());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            ivSelectedImage.setImageURI(selectedImageUri);
            ivSelectedImage.setVisibility(View.VISIBLE);
            uploadImageToFirebase(selectedImageUri);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, try upload again if we have an image
                if (selectedImageUri != null) {
                    uploadImageToFirebase(selectedImageUri);
                }
            } else {
                Toast.makeText(this, "Storage permission denied. Cannot upload images.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadImageToFirebase(Uri imageUri) {
        if (imageUri == null) {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if user is authenticated
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            btnSelectImage.setText("Select Image");
            return;
        }

        // Temporary URI read access is already granted by Android's ACTION_PICK intent.
        // No READ_EXTERNAL_STORAGE permission check is needed here.

        btnSelectImage.setEnabled(false);
        btnSelectImage.setText("Uploading...");

        try {
            // Create unique filename with timestamp and user ID
            String filename = currentUser.getUid() + "_" + System.currentTimeMillis() + ".jpg";
            StorageReference fileRef = storageReference.child(filename);

            Log.d("Upload", "Starting upload for: " + filename);
            Log.d("Upload", "Image URI: " + imageUri);
            Log.d("Upload", "Storage Reference: " + fileRef.getPath());
            Log.d("Upload", "User ID: " + currentUser.getUid());
            Log.d("Upload", "User Email: " + currentUser.getEmail());

            fileRef.putFile(imageUri)
                    .addOnProgressListener(taskSnapshot -> {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                        Log.d("Upload", "Upload progress: " + progress + "%");
                    })
                    .addOnSuccessListener(taskSnapshot -> {
                        Log.d("Upload", "Upload successful, getting download URL");
                        // Get download URL after successful upload
                        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            uploadedImageUrl = uri.toString();
                            btnSelectImage.setEnabled(true);
                            btnSelectImage.setText("✓ Image uploaded");
                            Toast.makeText(PostActivity.this, "Image uploaded successfully!", Toast.LENGTH_SHORT).show();
                            Log.i("Upload", "Image URL: " + uploadedImageUrl);
                        }).addOnFailureListener(e -> {
                            btnSelectImage.setEnabled(true);
                            btnSelectImage.setText("Select Image");
                            uploadedImageUrl = ""; // Reset on failure
                            String errorMsg = "Failed to get image URL: " + e.getMessage();
                            Toast.makeText(PostActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                            Log.e("Upload", errorMsg, e);
                        });
                    })
                    .addOnFailureListener(e -> {
                        btnSelectImage.setEnabled(true);
                        btnSelectImage.setText("Select Image");
                        uploadedImageUrl = ""; // Reset on failure
                        String errorMsg = "Upload failed: " + e.getMessage();
                        Toast.makeText(PostActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        Log.e("Upload", errorMsg, e);
                        e.printStackTrace();
                    });
        } catch (Exception e) {
            btnSelectImage.setEnabled(true);
            btnSelectImage.setText("Select Image");
            uploadedImageUrl = "";
            String errorMsg = "Error: " + e.getMessage();
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
            Log.e("Upload", errorMsg, e);
        }
    }

    // ─────────────────────────────────────────
    // 5. Post button → validate → security dialog
    // ─────────────────────────────────────────
    private void setupPostButton() {
        btnPostReport.setOnClickListener(v -> validateAndShowSecurityDialog());
    }

    private void validateAndShowSecurityDialog() {
        String itemName = etItemName.getText().toString().trim();

        if (TextUtils.isEmpty(itemName)) {
            etItemName.setError("Item name is required");
            etItemName.requestFocus();
            return;
        }
        if (itemName.length() < 3) {
            etItemName.setError("Name is too short");
            etItemName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(selectedCategory)) {
            Toast.makeText(this,
                    "Please select a category from either list",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(selectedLocation)) {
            Toast.makeText(this,
                    "Please select the last seen location",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        showSecurityDialog(itemName);
    }

    // ─────────────────────────────────────────
    // 6. Security question dialog
    // ─────────────────────────────────────────
    private void showSecurityDialog(String itemName) {

        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_security_questions, null);

        EditText etColor = dialogView.findViewById(R.id.etSecurityColor);
        EditText etBrand = dialogView.findViewById(R.id.etSecurityBrand);
        EditText etMark  = dialogView.findViewById(R.id.etSecurityMark);

        int hintColor = ContextCompat.getColor(this, R.color.colorTextHint);
        etColor.setHintTextColor(hintColor);
        etBrand.setHintTextColor(hintColor);
        etMark.setHintTextColor(hintColor);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Verify your item")
                .setMessage("These answers are private. Used only to confirm the real owner. Never shown publicly.")
                .setView(dialogView)
                .setPositiveButton("Submit & Post", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();

        // override positive button to validate before dismissing
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

            String color = etColor.getText().toString().trim();
            String brand = etBrand.getText().toString().trim();
            String mark  = etMark.getText().toString().trim();

            int filled = 0;
            if (!TextUtils.isEmpty(color)) filled++;
            if (!TextUtils.isEmpty(brand)) filled++;
            if (!TextUtils.isEmpty(mark))  filled++;

            if (filled < 2) {
                Toast.makeText(this,
                        "Please fill at least 2 security fields",
                        Toast.LENGTH_SHORT).show();
                return; // keep dialog open
            }

            securityColor = color;
            securityBrand = brand;
            securityMark  = mark;

            dialog.dismiss();
            postItem(itemName);
        });
    }

    // ─────────────────────────────────────────
    // 7. Post to Firestore
    // ─────────────────────────────────────────
    private void postItem(String itemName) {
        String description = etDescription.getText().toString().trim();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Not logged in! Please login first.", Toast.LENGTH_SHORT).show();
            // Don't log a local success if we aren't logged in.
            return;
        }

        String postedByUid  = currentUser.getUid();
        String postedByName = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Test User";

        btnPostReport.setEnabled(false);
        btnPostReport.setText("Posting…");

        Map<String, Object> item = new HashMap<>();
        item.put("title",         itemName);
        item.put("category",      selectedCategory);
        item.put("type",          selectedType);
        item.put("status",        "ACTIVE");
        item.put("location",      selectedLocation);
        item.put("description",   description);
        item.put("postedByUid",  postedByUid);
        item.put("postedByName", postedByName);
        item.put("postedAt",      System.currentTimeMillis());
        item.put("expiresAt",     System.currentTimeMillis() + (14L * 24 * 60 * 60 * 1000));
        item.put("photoUrl",      uploadedImageUrl);

        // security answers — private, never shown on feed
        item.put("securityColor", securityColor);
        item.put("securityBrand", securityBrand);
        item.put("securityMark",  securityMark);

        FirebaseFirestore.getInstance(com.google.firebase.FirebaseApp.getInstance(), "lf26")
                .collection("items")
                .add(item)
                .addOnSuccessListener(documentReference -> {
                    btnPostReport.setEnabled(true);
                    btnPostReport.setText("Post report");
                    Toast.makeText(this,
                            "Posted successfully!",
                            Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnPostReport.setEnabled(true);
                    btnPostReport.setText("Post report");
                    Toast.makeText(this,
                            "Failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Firebase Storage handles cleanup automatically
    }
}
