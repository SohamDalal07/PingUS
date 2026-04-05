package com.example.backtoyou;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AlertsActivity extends AppCompatActivity {

    private RecyclerView rvAlerts;
    private TextView tvNoAlerts;
    private TextView tvCurrentProcessingHeading;
    private View cardCurrentProcessing;
    private ImageView ivCurrentItem;
    private TextView tvCurrentStatus;
    private TextView tvCurrentItemTitle;
    private TextView tvCurrentItemMeta;
    private TextView tvCurrentClaimId;
    private TextView tvCurrentReceiver;
    private View layoutApproveReject;
    private MaterialButton btnApproveProcessing;
    private MaterialButton btnRejectProcessing;
    private AlertAdapter adapter;
    private BottomNavigationView bottomNavigation;
    private String currentUserId = "";
    private String activeContactPhone = "";
    private String activeContactEmail = "";
    private DocumentSnapshot currentProcessingClaim;
    private final List<DocumentSnapshot> unifiedClaimsList = new ArrayList<>();
    private final List<DocumentSnapshot> historyRows = new ArrayList<>();
    private List<DocumentSnapshot> posterClaimsList = new ArrayList<>();
    private List<DocumentSnapshot> claimerClaimsList = new ArrayList<>();

    private ListenerRegistration posterListenerReg;
    private ListenerRegistration claimerListenerReg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alerts);

        setupHeader();

        rvAlerts = findViewById(R.id.rvAlerts);
        tvNoAlerts = findViewById(R.id.tvNoAlerts);
        tvCurrentProcessingHeading = findViewById(R.id.tvCurrentProcessingHeading);
        cardCurrentProcessing = findViewById(R.id.cardCurrentProcessing);
        ivCurrentItem = findViewById(R.id.ivCurrentItem);
        tvCurrentStatus = findViewById(R.id.tvCurrentStatus);
        tvCurrentItemTitle = findViewById(R.id.tvCurrentItemTitle);
        tvCurrentItemMeta = findViewById(R.id.tvCurrentItemMeta);
        tvCurrentClaimId = findViewById(R.id.tvCurrentClaimId);
        tvCurrentReceiver = findViewById(R.id.tvCurrentReceiver);
        layoutApproveReject = findViewById(R.id.layoutApproveReject);
        btnApproveProcessing = findViewById(R.id.btnApproveProcessing);
        btnRejectProcessing = findViewById(R.id.btnRejectProcessing);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        View btnCurrentCall = findViewById(R.id.btnCurrentCall);
        View btnCurrentEmail = findViewById(R.id.btnCurrentEmail);
        View btnCurrentWhatsapp = findViewById(R.id.btnCurrentWhatsapp);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
        }

        adapter = new AlertAdapter(historyRows);
        rvAlerts.setLayoutManager(new LinearLayoutManager(this));
        rvAlerts.setAdapter(adapter);

        setupBottomNavigation();
        if (btnCurrentCall != null) btnCurrentCall.setOnClickListener(v -> makePhoneCall());
        if (btnCurrentEmail != null) btnCurrentEmail.setOnClickListener(v -> sendEmail());
        if (btnCurrentWhatsapp != null) btnCurrentWhatsapp.setOnClickListener(v -> sendWhatsapp());
        if (btnApproveProcessing != null) {
            btnApproveProcessing.setOnClickListener(v -> {
                if (currentProcessingClaim != null) {
                    updateClaimStatus(currentProcessingClaim, "APPROVED");
                }
            });
        }
        if (btnRejectProcessing != null) {
            btnRejectProcessing.setOnClickListener(v -> {
                if (currentProcessingClaim != null) {
                    updateClaimStatus(currentProcessingClaim, "REJECTED");
                }
            });
        }

        loadAlerts(currentUserId);
    }

    private void setupHeader() {
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void loadAlerts(String currentUserId) {
        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Please log in to view alerts", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        posterListenerReg = FirebaseFirestore.getInstance()
                .collection("claims")
                .whereEqualTo("posterUid", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("AlertsActivity", "Poster listen failed.", error);
                        return;
                    }
                    if (value != null) {
                        posterClaimsList = new ArrayList<>(value.getDocuments());
                        mergeAndRefreshList();
                    }
                });

        // All claims where this user is claimer (full history, not only approved)
        claimerListenerReg = FirebaseFirestore.getInstance()
                .collection("claims")
                .whereEqualTo("claimerUid", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("AlertsActivity", "Claimer listen failed.", error);
                        return;
                    }
                    if (value != null) {
                        claimerClaimsList = new ArrayList<>(value.getDocuments());
                        mergeAndRefreshList();
                    }
                });
    }

    private void setupBottomNavigation() {
        if (bottomNavigation == null) return;
        bottomNavigation.setSelectedItemId(R.id.navigation_alerts);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                startActivity(new Intent(this, Home.class));
                finish();
                return true;
            } else if (itemId == R.id.navigation_post) {
                startActivity(new Intent(this, PostActivity.class));
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

    private void mergeAndRefreshList() {
        unifiedClaimsList.clear();
        unifiedClaimsList.addAll(posterClaimsList);
        unifiedClaimsList.addAll(claimerClaimsList);

        Collections.sort(unifiedClaimsList, (o1, o2) -> {
            Long t1 = o1.getLong("timestamp");
            Long t2 = o2.getLong("timestamp");
            if (t1 == null) t1 = 0L;
            if (t2 == null) t2 = 0L;
            return t2.compareTo(t1);
        });

        currentProcessingClaim = pickCurrentProcessingClaim(unifiedClaimsList);
        bindCurrentProcessingCard();
        rebuildHistoryRows();
        adapter.notifyDataSetChanged();

        if (historyRows.isEmpty()) {
            tvNoAlerts.setVisibility(View.VISIBLE);
            rvAlerts.setVisibility(View.GONE);
        } else {
            tvNoAlerts.setVisibility(View.GONE);
            rvAlerts.setVisibility(View.VISIBLE);
        }

        refreshActiveContactFromLatestClaim();
    }

    private static DocumentSnapshot pickCurrentProcessingClaim(List<DocumentSnapshot> list) {
        for (DocumentSnapshot doc : list) {
            String status = safe(doc.getString("status")).toUpperCase();
            if ("PENDING".equals(status)) {
                return doc;
            }
        }
        return null;
    }

    private void rebuildHistoryRows() {
        historyRows.clear();
        String skipId = currentProcessingClaim != null ? currentProcessingClaim.getId() : null;
        for (DocumentSnapshot d : unifiedClaimsList) {
            if (skipId != null && skipId.equals(d.getId())) {
                continue;
            }
            historyRows.add(d);
        }
    }

    private void refreshActiveContactFromLatestClaim() {
        activeContactPhone = "";
        activeContactEmail = "";
        DocumentSnapshot sourceClaim = currentProcessingClaim != null
                ? currentProcessingClaim
                : (unifiedClaimsList.isEmpty() ? null : unifiedClaimsList.get(0));
        if (sourceClaim == null) return;

        String claimerUid = sourceClaim.getString("claimerUid");
        String posterUid = sourceClaim.getString("posterUid");
        String contactUid = currentUserId.equals(claimerUid) ? posterUid : claimerUid;
        if (contactUid == null || contactUid.isEmpty()) return;

        FirebaseFirestore.getInstance().collection("users").document(contactUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String email = doc.getString("email");
                        String phone = doc.getString("phone");
                        activeContactEmail = email != null ? email : "";
                        activeContactPhone = phone != null ? phone : "";
                    }
                });
    }

    private void bindCurrentProcessingCard() {
        if (cardCurrentProcessing == null) return;
        if (currentProcessingClaim == null) {
            if (tvCurrentProcessingHeading != null) {
                tvCurrentProcessingHeading.setVisibility(View.GONE);
            }
            cardCurrentProcessing.setVisibility(View.GONE);
            return;
        }
        if (tvCurrentProcessingHeading != null) {
            tvCurrentProcessingHeading.setVisibility(View.VISIBLE);
        }
        cardCurrentProcessing.setVisibility(View.VISIBLE);

        DocumentSnapshot latestClaim = currentProcessingClaim;
        String claimStatus = safe(latestClaim.getString("status")).toUpperCase();
        String title = safe(latestClaim.getString("itemTitle"));
        String claimerName = safe(latestClaim.getString("claimerName"));
        String claimId = latestClaim.getId();
        String posterUid = latestClaim.getString("posterUid");

        tvCurrentStatus.setText(claimStatus.isEmpty() ? "PENDING" : claimStatus);
        tvCurrentItemTitle.setText(title.isEmpty() ? "Claim Request" : title);
        tvCurrentItemMeta.setText("Claim update • " + getTimeAgo(latestClaim.getLong("timestamp")));
        tvCurrentClaimId.setText(claimId);
        tvCurrentReceiver.setText(claimerName.isEmpty() ? "Unknown" : claimerName);

        boolean posterPending = "PENDING".equals(claimStatus)
                && posterUid != null
                && posterUid.equals(currentUserId);
        if (layoutApproveReject != null) {
            layoutApproveReject.setVisibility(posterPending ? View.VISIBLE : View.GONE);
        }

        String itemId = latestClaim.getString("itemId");
        if (itemId == null || itemId.isEmpty()) {
            applyCurrentItemArt(R.drawable.electronics);
            return;
        }
        FirebaseFirestore.getInstance().collection("items").document(itemId).get()
                .addOnSuccessListener(itemDoc -> {
                    if (!itemDoc.exists()) {
                        applyCurrentItemArt(R.drawable.electronics);
                        return;
                    }
                    String itemTitle = safe(itemDoc.getString("title"));
                    String location = safe(itemDoc.getString("location"));
                    String category = safe(itemDoc.getString("category"));
                    if (!itemTitle.isEmpty()) {
                        tvCurrentItemTitle.setText(itemTitle);
                    }
                    if (!location.isEmpty()) {
                        tvCurrentItemMeta.setText(location + " • " + getTimeAgo(latestClaim.getLong("timestamp")));
                    }
                    int art = CategoryDrawableHelper.drawableResForCategory(category, R.drawable.electronics);
                    applyCurrentItemArt(art);
                })
                .addOnFailureListener(e -> applyCurrentItemArt(R.drawable.electronics));
    }

    private void applyCurrentItemArt(int drawableRes) {
        if (ivCurrentItem == null) return;
        ivCurrentItem.setImageResource(drawableRes);
        ivCurrentItem.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ivCurrentItem.setPadding(0, 0, 0, 0);
        ImageViewCompat.setImageTintList(ivCurrentItem, null);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String getTimeAgo(Long timestamp) {
        if (timestamp == null || timestamp <= 0) return "just now";
        long diff = System.currentTimeMillis() - timestamp;
        long minutes = diff / (60 * 1000);
        long hours = diff / (60 * 60 * 1000);
        long days = diff / (24 * 60 * 60 * 1000);
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + "m ago";
        if (hours < 24) return hours + "h ago";
        return days + "d ago";
    }

    private void makePhoneCall() {
        if (activeContactPhone == null || activeContactPhone.isEmpty()) {
            Toast.makeText(this, "Phone number not available yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:" + activeContactPhone));
            startActivity(callIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open dialer.", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendEmail() {
        if (activeContactEmail == null || activeContactEmail.isEmpty()) {
            Toast.makeText(this, "Email not available yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:" + activeContactEmail));
            startActivity(emailIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open email app.", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendWhatsapp() {
        if (activeContactPhone == null || activeContactPhone.isEmpty()) {
            Toast.makeText(this, "Phone number not available yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent whatsappIntent = new Intent(Intent.ACTION_VIEW);
            whatsappIntent.setData(Uri.parse("https://api.whatsapp.com/send?phone=" + activeContactPhone));
            startActivity(whatsappIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open WhatsApp.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateClaimStatus(DocumentSnapshot claimDoc, String newStatus) {
        String claimId = claimDoc.getId();

        FirebaseFirestore.getInstance().collection("claims").document(claimId)
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Claim " + newStatus.toLowerCase(), Toast.LENGTH_SHORT).show();

                    if ("APPROVED".equals(newStatus)) {
                        String itemId = claimDoc.getString("itemId");
                        if (itemId != null) {
                            FirebaseFirestore.getInstance().collection("items").document(itemId)
                                    .update("type", "CLAIMED")
                                    .addOnFailureListener(e -> Log.e("AlertsActivity", "Failed to update item status", e));
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error updating claim", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (posterListenerReg != null) posterListenerReg.remove();
        if (claimerListenerReg != null) claimerListenerReg.remove();
    }
}
