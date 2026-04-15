package com.example.backtoyou;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AlertsActivity extends AppCompatActivity implements AlertAdapter.OnClaimActionListener {

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
    private View btnApproveProcessing;
    private View btnRejectProcessing;
    private BottomNavigationView bottomNavigation;
    private AlertAdapter adapter;
    private DocumentSnapshot currentProcessingClaim;
    private List<DocumentSnapshot> posterClaimsList = new ArrayList<>();
    private List<DocumentSnapshot> claimerClaimsList = new ArrayList<>();
    private List<DocumentSnapshot> unifiedClaimsList = new ArrayList<>();
    
    private ListenerRegistration posterListenerReg;
    private ListenerRegistration claimerListenerReg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alerts);

        Toolbar toolbar = findViewById(R.id.toolbarAlerts);
        setSupportActionBar(toolbar);
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

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

        String currentUserId = "";
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
        }

        adapter = new AlertAdapter(unifiedClaimsList, currentUserId, this);
        rvAlerts.setLayoutManager(new LinearLayoutManager(this));
        rvAlerts.setAdapter(adapter);
        if (btnApproveProcessing != null) {
            btnApproveProcessing.setOnClickListener(v -> {
                if (currentProcessingClaim != null) onApprove(currentProcessingClaim);
            });
        }
        if (btnRejectProcessing != null) {
            btnRejectProcessing.setOnClickListener(v -> {
                if (currentProcessingClaim != null) onReject(currentProcessingClaim);
            });
        }
        if (bottomNavigation != null) {
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

        loadAlerts(currentUserId);
    }

    private void loadAlerts(String currentUserId) {
        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Please log in to view alerts", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Listener 1: As Poster (All claims for history)
        posterListenerReg = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "lf26")
                .collection("claims")
                .whereEqualTo("posterUid", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("AlertsActivity", "Poster listen failed.", error);
                        return;
                    }
                    if (value != null) {
                        posterClaimsList.clear();
                        posterClaimsList.addAll(value.getDocuments());
                        mergeAndRefreshList();
                    }
                });

        // Listener 2: As Claimer (Checking for APPROVED claims)
        claimerListenerReg = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "lf26")
                .collection("claims")
                .whereEqualTo("claimerUid", currentUserId)
                .whereEqualTo("status", "APPROVED")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("AlertsActivity", "Claimer listen failed.", error);
                        return;
                    }
                    if (value != null) {
                        claimerClaimsList.clear();
                        claimerClaimsList.addAll(value.getDocuments());
                        mergeAndRefreshList();
                    }
                });
    }

    private void mergeAndRefreshList() {
        unifiedClaimsList.clear();
        unifiedClaimsList.addAll(posterClaimsList);
        unifiedClaimsList.addAll(claimerClaimsList);

        // Sort by timestamp if possible
        Collections.sort(unifiedClaimsList, (o1, o2) -> {
            Long t1 = o1.getLong("timestamp");
            Long t2 = o2.getLong("timestamp");
            if (t1 == null) t1 = 0L;
            if (t2 == null) t2 = 0L;
            return t2.compareTo(t1); // Descending
        });

        bindCurrentProcessingCard();
        adapter.notifyDataSetChanged();

        if (unifiedClaimsList.isEmpty()) {
            tvNoAlerts.setVisibility(View.VISIBLE);
            rvAlerts.setVisibility(View.GONE);
        } else {
            tvNoAlerts.setVisibility(View.GONE);
            rvAlerts.setVisibility(View.VISIBLE);
        }
    }

    private void bindCurrentProcessingCard() {
        DocumentSnapshot pending = null;
        for (DocumentSnapshot d : unifiedClaimsList) {
            String status = d.getString("status");
            if ("PENDING".equalsIgnoreCase(status)) {
                pending = d;
                break;
            }
        }

        if (pending == null) {
            currentProcessingClaim = null;
            if (tvCurrentProcessingHeading != null) tvCurrentProcessingHeading.setVisibility(View.GONE);
            if (cardCurrentProcessing != null) cardCurrentProcessing.setVisibility(View.GONE);
            return;
        }
        currentProcessingClaim = pending;

        if (tvCurrentProcessingHeading != null) tvCurrentProcessingHeading.setVisibility(View.VISIBLE);
        if (cardCurrentProcessing != null) cardCurrentProcessing.setVisibility(View.VISIBLE);
        if (layoutApproveReject != null) layoutApproveReject.setVisibility(View.VISIBLE);
        if (tvCurrentStatus != null) tvCurrentStatus.setText("PENDING");
        if (tvCurrentItemTitle != null) {
            String title = pending.getString("itemTitle");
            tvCurrentItemTitle.setText(title != null && !title.trim().isEmpty() ? title : "Claim Request");
        }
        if (tvCurrentItemMeta != null) {
            tvCurrentItemMeta.setText("Claim update • " + getTimeAgo(pending.getLong("timestamp")));
        }
        final Long pendingTimestamp = pending.getLong("timestamp");
        if (tvCurrentClaimId != null) tvCurrentClaimId.setText(pending.getId());
        if (tvCurrentReceiver != null) {
            String receiver = pending.getString("claimerName");
            tvCurrentReceiver.setText(receiver != null && !receiver.trim().isEmpty() ? receiver : "Unknown");
        }

        String itemId = pending.getString("itemId");
        if (itemId == null || itemId.trim().isEmpty()) {
            if (ivCurrentItem != null) ivCurrentItem.setImageResource(R.drawable.electronics);
            return;
        }

        FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "lf26").collection("items").document(itemId).get()
                .addOnSuccessListener(itemDoc -> {
                    if (!itemDoc.exists()) {
                        if (ivCurrentItem != null) ivCurrentItem.setImageResource(R.drawable.electronics);
                        return;
                    }
                    String photoUrl = itemDoc.getString("photoUrl");
                    String category = itemDoc.getString("category");
                    if (tvCurrentItemTitle != null) {
                        String itemTitle = itemDoc.getString("title");
                        if (itemTitle != null && !itemTitle.trim().isEmpty()) tvCurrentItemTitle.setText(itemTitle.trim());
                    }
                    if (tvCurrentItemMeta != null) {
                        String location = itemDoc.getString("location");
                        if (location != null && !location.trim().isEmpty()) {
                            tvCurrentItemMeta.setText(location.trim() + " • " + getTimeAgo(pendingTimestamp));
                        }
                    }
                    if (ivCurrentItem == null) return;
                    if (photoUrl != null && !photoUrl.trim().isEmpty()) {
                        Glide.with(this)
                                .load(photoUrl)
                                .placeholder(R.drawable.electronics)
                                .centerCrop()
                                .into(ivCurrentItem);
                    } else {
                        int art = CategoryDrawableHelper.drawableResForCategory(category, R.drawable.electronics);
                        ivCurrentItem.setImageResource(art);
                    }
                })
                .addOnFailureListener(e -> {
                    if (ivCurrentItem != null) ivCurrentItem.setImageResource(R.drawable.electronics);
                });
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

    @Override
    public void onApprove(DocumentSnapshot claimDoc) {
        updateClaimStatus(claimDoc, "APPROVED");
    }

    @Override
    public void onReject(DocumentSnapshot claimDoc) {
        updateClaimStatus(claimDoc, "REJECTED");
    }

    private void updateClaimStatus(DocumentSnapshot claimDoc, String newStatus) {
        String claimId = claimDoc.getId();
        
        FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "lf26").collection("claims").document(claimId)
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Claim " + newStatus.toLowerCase(), Toast.LENGTH_SHORT).show();
                    
                    // If approved, globally tag the item as CLAIMED
                    if ("APPROVED".equals(newStatus)) {
                        String itemId = claimDoc.getString("itemId");
                        if (itemId != null) {
                            FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "lf26").collection("items").document(itemId)
                                .update("type", "CLAIMED")
                                .addOnFailureListener(e -> Log.e("AlertsActivity", "Failed to update item status", e));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating claim", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (posterListenerReg != null) posterListenerReg.remove();
        if (claimerListenerReg != null) claimerListenerReg.remove();
    }
}