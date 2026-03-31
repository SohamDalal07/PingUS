package com.example.backtoyou;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PostActivity extends AppCompatActivity {

    // ── Views ──
    private Button   btnLost, btnFound, btnPostReport;
    private EditText etItemName, etDescription;
    private Spinner  spinnerPersonal, spinnerAcademic, spinnerLocation;
    private TextView tvPersonalLabel, tvAcademicLabel;

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
    }

    // ─────────────────────────────────────────
    // 1. Bind views
    // ─────────────────────────────────────────
    private void bindViews() {
        btnLost         = findViewById(R.id.btnLost);
        btnFound        = findViewById(R.id.btnFound);
        btnPostReport   = findViewById(R.id.btnPostReport);
        etItemName      = findViewById(R.id.etItemName);
        etDescription   = findViewById(R.id.etDescription);
        spinnerPersonal = findViewById(R.id.spinnerPersonal);
        spinnerAcademic = findViewById(R.id.spinnerAcademic);
        spinnerLocation = findViewById(R.id.spinnerLocation);
        tvPersonalLabel = findViewById(R.id.tvPersonalLabel);
        tvAcademicLabel = findViewById(R.id.tvAcademicLabel);

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
        btnLost.setOnClickListener(v  -> setType("LOST"));
        btnFound.setOnClickListener(v -> setType("FOUND"));
        setType("LOST"); // default
    }

    private void setType(String type) {
        selectedType = type;
        if (type.equals("LOST")) {
            btnLost.setTextColor(ContextCompat.getColor(this, R.color.colorTextOnPrimary));
            btnLost.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
            btnFound.setTextColor(ContextCompat.getColor(this, R.color.colorTextSecondary));
            btnFound.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        } else {
            btnFound.setTextColor(ContextCompat.getColor(this, R.color.colorTextOnPrimary));
            btnFound.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
            btnLost.setTextColor(ContextCompat.getColor(this, R.color.colorTextSecondary));
            btnLost.setBackgroundColor(android.graphics.Color.TRANSPARENT);
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

//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) {
//            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
//            return;
//        }
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

// TODO: remove this bypass when LoginActivity is built
        String postedByUid  = currentUser != null ? currentUser.getUid()         : "test_user_001";
        String postedByName = currentUser != null ? currentUser.getDisplayName() : "Test User";

        btnPostReport.setEnabled(false);
        btnPostReport.setText("Posting…");

        Map<String, Object> item = new HashMap<>();
        item.put("title",         itemName);
        item.put("category",      selectedCategory);
        item.put("type",          selectedType);
        item.put("status",        "ACTIVE");
        item.put("location",      selectedLocation);
        item.put("description",   description);
//        item.put("postedByUid",   currentUser.getUid());
//        item.put("postedByName",  currentUser.getDisplayName() != null
//                ? currentUser.getDisplayName() : "Anonymous");
        item.put("postedByUid",  postedByUid);
        item.put("postedByName", postedByName);
        item.put("postedAt",      System.currentTimeMillis());
        item.put("expiresAt",     System.currentTimeMillis() + (14L * 24 * 60 * 60 * 1000));
        item.put("photoUrl",      "");

        // security answers — private, never shown on feed
        item.put("securityColor", securityColor);
        item.put("securityBrand", securityBrand);
        item.put("securityMark",  securityMark);

        FirebaseFirestore.getInstance()
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
}