package com.example.backtoyou;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ItemDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvStatus, tvCategory, tvLocationTime, tvDescription, tvPoster;
    private MaterialButton btnContactCall, btnContactEmail, btnContactWhatsapp;
    private LinearLayout layoutContactButtons, layoutClaimItem;
    private MaterialButton btnClaimItem;
    private TextView tvClaimStatusHint;
    private ImageView ivDetailImage;
    private NestedScrollView nestedScrollDetail;
    private View layoutBottomActions;

    private String posterEmail = "";
    private String posterPhone = "";
    private String currentUserId, posterUid, itemId;
    /** Item category from Firestore (also stored on new claims as itemCategory). */
    private String itemCategory = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        bindViews();
        setupToolbar();
        populateData();
    }

    private void bindViews() {
        tvTitle = findViewById(R.id.tvDetailTitle);
        tvStatus = findViewById(R.id.tvDetailStatus);
        tvCategory = findViewById(R.id.tvDetailCategory);
        tvLocationTime = findViewById(R.id.tvDetailLocationTime);
        tvDescription = findViewById(R.id.tvDetailDescription);
        tvPoster = findViewById(R.id.tvDetailPoster);
        
        layoutContactButtons = findViewById(R.id.layoutContactButtons);
        layoutClaimItem = findViewById(R.id.layoutClaimItem);
        btnClaimItem = findViewById(R.id.btnClaimItem);
        tvClaimStatusHint = findViewById(R.id.tvClaimStatusHint);

        btnContactCall = findViewById(R.id.btnContactCall);
        btnContactEmail = findViewById(R.id.btnContactEmail);
        btnContactWhatsapp = findViewById(R.id.btnContactWhatsapp);
        ivDetailImage = findViewById(R.id.ivDetailImage);
        nestedScrollDetail = findViewById(R.id.nestedScrollDetail);
        layoutBottomActions = findViewById(R.id.layoutBottomActions);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarDetail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Item Details");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void populateData() {
        Intent intent = getIntent();
        if (intent == null) return;

        itemId = intent.getStringExtra("itemId");
        String title = intent.getStringExtra("title");
        String type = intent.getStringExtra("type");
        String category = intent.getStringExtra("category");
        String location = intent.getStringExtra("location");
        String description = intent.getStringExtra("description");
        String postedByName = intent.getStringExtra("postedByName");
        posterUid = intent.getStringExtra("postedByUid");
        long postedAt = intent.getLongExtra("postedAt", 0L);

        tvTitle.setText(title != null ? title : "Unknown Item");
        
        if (type != null) {
            if (type.equals("LOST")) {
                tvStatus.setText("Lost");
                tvStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.colorLostBg));
            } else if (type.equals("FOUND")) {
                tvStatus.setText("Found");
                tvStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.colorFoundBg));
            } else {
                tvStatus.setText("Claimed");
                tvStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.colorClaimedBg));
            }
        }
        
        itemCategory = category != null ? category : "";
        tvCategory.setText(!itemCategory.isEmpty() ? itemCategory : "Unspecified");

        if (ivDetailImage != null) {
            int hero = CategoryDrawableHelper.drawableResForCategory(
                    itemCategory, R.drawable.electronics);
            ivDetailImage.setImageResource(hero);
            ImageViewCompat.setImageTintList(ivDetailImage, null);
        }

        String timeStr = getTimeAgo(postedAt);
        tvLocationTime.setText((location != null ? location : "Unknown Location") + " · " + timeStr);
        
        tvDescription.setText(description != null && !description.isEmpty() ? description : "No description provided.");
        tvPoster.setText(postedByName != null ? postedByName : "Unknown Poster");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = user != null ? user.getUid() : "test_user_001";
        
        if (currentUserId.equals(posterUid)) {
            // Owner viewing own listing: no contact shortcuts here (claim flow is for other users).
            layoutContactButtons.setVisibility(View.GONE);
            layoutClaimItem.setVisibility(View.GONE);
            if (tvClaimStatusHint != null) {
                tvClaimStatusHint.setVisibility(View.GONE);
            }
            if (layoutBottomActions != null) {
                layoutBottomActions.setVisibility(View.GONE);
            }
        } else {
            // Other user: claim → security questions (dialog) → pending message until poster acts.
            layoutContactButtons.setVisibility(View.GONE);
            layoutClaimItem.setVisibility(View.VISIBLE);
            if (tvClaimStatusHint != null) {
                tvClaimStatusHint.setVisibility(View.VISIBLE);
                tvClaimStatusHint.setText("Tap Claim and answer the security questions. The poster will review your request.");
            }
            checkClaimStatus();
            if (layoutBottomActions != null) {
                layoutBottomActions.setVisibility(View.VISIBLE);
            }
        }

        setupHeroImageScrollToActions();
    }

    /** Tap header image to scroll down to contact / claim actions. */
    private void setupHeroImageScrollToActions() {
        if (ivDetailImage == null || nestedScrollDetail == null) return;
        ivDetailImage.setClickable(true);
        ivDetailImage.setFocusable(true);
        ivDetailImage.setOnClickListener(v -> {
            nestedScrollDetail.post(() -> {
                if (layoutBottomActions != null) {
                    int[] loc = new int[2];
                    layoutBottomActions.getLocationOnScreen(loc);
                    int[] scrollLoc = new int[2];
                    nestedScrollDetail.getLocationOnScreen(scrollLoc);
                    int dy = loc[1] - scrollLoc[1] - nestedScrollDetail.getPaddingTop();
                    nestedScrollDetail.smoothScrollBy(0, Math.max(0, dy));
                } else {
                    nestedScrollDetail.fullScroll(View.FOCUS_DOWN);
                }
            });
        });
    }

    private void checkClaimStatus() {
        if (itemId == null || currentUserId == null) return;
        btnClaimItem.setEnabled(false);
        btnClaimItem.setText("Loading...");
        if (tvClaimStatusHint != null) {
            tvClaimStatusHint.setText("Checking your claim status…");
        }

        FirebaseFirestore.getInstance().collection("claims")
            .whereEqualTo("itemId", itemId)
            .whereEqualTo("claimerUid", currentUserId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (queryDocumentSnapshots.isEmpty()) {
                    btnClaimItem.setEnabled(true);
                    btnClaimItem.setText("Claim this item");
                    btnClaimItem.setOnClickListener(v -> showClaimDialog());
                    if (tvClaimStatusHint != null) {
                        tvClaimStatusHint.setVisibility(View.VISIBLE);
                        tvClaimStatusHint.setText("Tap Claim and answer the security questions. The poster will review your request.");
                    }
                } else {
                    DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                    String status = doc.getString("status");
                    if ("PENDING".equals(status)) {
                        btnClaimItem.setEnabled(false);
                        btnClaimItem.setText("Request pending");
                        if (tvClaimStatusHint != null) {
                            tvClaimStatusHint.setVisibility(View.VISIBLE);
                            tvClaimStatusHint.setText("Pending request sent. Waiting for the poster to approve or reject your claim.");
                        }
                    } else if ("APPROVED".equals(status)) {
                        layoutClaimItem.setVisibility(View.GONE);
                        layoutContactButtons.setVisibility(View.VISIBLE);
                        if (layoutBottomActions != null) {
                            layoutBottomActions.setVisibility(View.VISIBLE);
                        }
                        if (posterUid != null) {
                            fetchContactInfo(posterUid);
                        }
                        setupContactClickListeners();
                    } else if ("REJECTED".equals(status)) {
                        btnClaimItem.setEnabled(false);
                        btnClaimItem.setText("Request rejected");
                        if (tvClaimStatusHint != null) {
                            tvClaimStatusHint.setVisibility(View.VISIBLE);
                            tvClaimStatusHint.setText("The poster did not approve this claim. You cannot claim this item again from here.");
                        }
                    }
                }
            })
            .addOnFailureListener(e -> {
                btnClaimItem.setEnabled(true);
                btnClaimItem.setText("Retry");
                btnClaimItem.setOnClickListener(v -> checkClaimStatus());
                if (tvClaimStatusHint != null) {
                    tvClaimStatusHint.setText("Could not load claim status. Tap Retry.");
                }
            });
    }

    private void showClaimDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_claim);
        dialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        EditText etColor = dialog.findViewById(R.id.etClaimColor);
        EditText etBrand = dialog.findViewById(R.id.etClaimBrand);
        EditText etMark = dialog.findViewById(R.id.etClaimMark);
        Button btnCancel = dialog.findViewById(R.id.btnClaimCancel);
        Button btnSubmit = dialog.findViewById(R.id.btnClaimSubmit);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSubmit.setOnClickListener(v -> {
            String color = etColor.getText().toString().trim();
            String brand = etBrand.getText().toString().trim();
            String mark = etMark.getText().toString().trim();

            if (color.isEmpty() || brand.isEmpty() || mark.isEmpty()) {
                Toast.makeText(this, "Please answer all questions.", Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.dismiss();
            submitClaim(color, brand, mark);
        });

        dialog.show();
    }

    private void submitClaim(String color, String brand, String mark) {
        btnClaimItem.setEnabled(false);
        btnClaimItem.setText("Sending request…");
        if (tvClaimStatusHint != null) {
            tvClaimStatusHint.setVisibility(View.VISIBLE);
            tvClaimStatusHint.setText("Sending your claim request…");
        }

        String claimerName = "Unknown User";
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            claimerName = user.getDisplayName();
        } else if (user != null && user.getEmail() != null) {
            claimerName = user.getEmail().split("@")[0];
        }

        Map<String, Object> claimData = new HashMap<>();
        claimData.put("itemId", itemId);
        claimData.put("itemTitle", tvTitle.getText().toString());
        claimData.put("itemCategory", itemCategory);
        claimData.put("claimerUid", currentUserId);
        claimData.put("claimerName", claimerName);
        claimData.put("posterUid", posterUid);
        claimData.put("colorAns", color);
        claimData.put("brandAns", brand);
        claimData.put("markAns", mark);
        claimData.put("status", "PENDING");
        claimData.put("timestamp", System.currentTimeMillis());

        FirebaseFirestore.getInstance().collection("claims").add(claimData)
            .addOnSuccessListener(documentReference -> {
                Toast.makeText(this, "Pending request sent. The poster will be notified.", Toast.LENGTH_LONG).show();
                if (tvClaimStatusHint != null) {
                    tvClaimStatusHint.setVisibility(View.VISIBLE);
                    tvClaimStatusHint.setText("Pending request sent. Waiting for the poster to approve or reject your claim.");
                }
                checkClaimStatus();
            })
            .addOnFailureListener(e -> {
                btnClaimItem.setEnabled(true);
                btnClaimItem.setText("Claim Item");
                Toast.makeText(this, "Failed to submit claim.", Toast.LENGTH_SHORT).show();
            });
    }

    private void fetchContactInfo(String uid) {
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    posterEmail = documentSnapshot.getString("email");
                    posterPhone = documentSnapshot.getString("phone");
                    if (posterEmail == null) posterEmail = "";
                    if (posterPhone == null) posterPhone = "";
                }
            })
            .addOnFailureListener(e -> {
                Log.e("ItemDetail", "Error fetching user info", e);
            });
    }

    private void setupContactClickListeners() {
        btnContactCall.setOnClickListener(v -> {
            if (posterPhone.isEmpty()) {
                Toast.makeText(this, "No phone number available.", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + posterPhone));
                startActivity(callIntent);
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, "No dialer app found.", Toast.LENGTH_SHORT).show();
            }
        });

        btnContactEmail.setOnClickListener(v -> {
            if (posterEmail.isEmpty()) {
                Toast.makeText(this, "No email available.", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:" + posterEmail));
                startActivity(emailIntent);
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, "No email app found.", Toast.LENGTH_SHORT).show();
            }
        });

        btnContactWhatsapp.setOnClickListener(v -> {
            if (posterPhone.isEmpty()) {
                Toast.makeText(this, "No phone number available.", Toast.LENGTH_SHORT).show();
                return;
            }
            // Passing raw number, user must have included country code on signup for this to work natively
            String url = "https://api.whatsapp.com/send?phone=" + posterPhone;
            try {
                Intent whatsappIntent = new Intent(Intent.ACTION_VIEW);
                whatsappIntent.setData(Uri.parse(url));
                startActivity(whatsappIntent);
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, "WhatsApp not installed.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getTimeAgo(long postedAt) {
        if (postedAt == 0) return "";
        long diff = System.currentTimeMillis() - postedAt;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        long days = TimeUnit.MILLISECONDS.toDays(diff);
        if (minutes < 1)  return "Just now";
        if (minutes < 60) return minutes + "m ago";
        if (hours < 24)   return hours + "h ago";
        if (days == 1)    return "Yesterday";
        return days + "d ago";
    }
}
