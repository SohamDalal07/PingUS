package com.example.backtoyou;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AlertsActivity extends AppCompatActivity implements AlertAdapter.OnClaimActionListener {

    private RecyclerView rvAlerts;
    private TextView tvNoAlerts;
    private AlertAdapter adapter;
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
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvAlerts = findViewById(R.id.rvAlerts);
        tvNoAlerts = findViewById(R.id.tvNoAlerts);

        String currentUserId = "";
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
        }

        adapter = new AlertAdapter(unifiedClaimsList, currentUserId, this);
        rvAlerts.setLayoutManager(new LinearLayoutManager(this));
        rvAlerts.setAdapter(adapter);

        loadAlerts(currentUserId);
    }

    private void loadAlerts(String currentUserId) {
        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Please log in to view alerts", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Listener 1: As Poster (All claims for history)
        posterListenerReg = FirebaseFirestore.getInstance()
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
        claimerListenerReg = FirebaseFirestore.getInstance()
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

        adapter.notifyDataSetChanged();

        if (unifiedClaimsList.isEmpty()) {
            tvNoAlerts.setVisibility(View.VISIBLE);
            rvAlerts.setVisibility(View.GONE);
        } else {
            tvNoAlerts.setVisibility(View.GONE);
            rvAlerts.setVisibility(View.VISIBLE);
        }
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
        
        FirebaseFirestore.getInstance().collection("claims").document(claimId)
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Claim " + newStatus.toLowerCase(), Toast.LENGTH_SHORT).show();
                    
                    // If approved, globally tag the item as CLAIMED
                    if ("APPROVED".equals(newStatus)) {
                        String itemId = claimDoc.getString("itemId");
                        if (itemId != null) {
                            FirebaseFirestore.getInstance().collection("items").document(itemId)
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