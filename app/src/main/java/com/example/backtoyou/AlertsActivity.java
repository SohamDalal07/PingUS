package com.example.backtoyou;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private TextView tvCurrentColorAnswer;
    private TextView tvCurrentBrandAnswer;
    private TextView tvCurrentMarkAnswer;
    private View layoutApproveReject;
    private View btnApproveProcessing;
    private View btnRejectProcessing;
    private View btnCurrentCall;
    private View btnCurrentEmail;
    private View btnCurrentWhatsapp;
    private BottomNavigationView bottomNavigation;
    private AlertAdapter adapter;
    private DocumentSnapshot currentProcessingClaim;
    private List<DocumentSnapshot> posterClaimsList = new ArrayList<>();
    private List<DocumentSnapshot> claimerClaimsList = new ArrayList<>();
    private List<DocumentSnapshot> unifiedClaimsList = new ArrayList<>();
    private String currentUserId = "";
    private String currentContactPhone = "";
    private String currentContactEmail = "";
    
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
        tvCurrentColorAnswer = findViewById(R.id.tvCurrentColorAnswer);
        tvCurrentBrandAnswer = findViewById(R.id.tvCurrentBrandAnswer);
        tvCurrentMarkAnswer = findViewById(R.id.tvCurrentMarkAnswer);
        layoutApproveReject = findViewById(R.id.layoutApproveReject);
        btnApproveProcessing = findViewById(R.id.btnApproveProcessing);
        btnRejectProcessing = findViewById(R.id.btnRejectProcessing);
        btnCurrentCall = findViewById(R.id.btnCurrentCall);
        btnCurrentEmail = findViewById(R.id.btnCurrentEmail);
        btnCurrentWhatsapp = findViewById(R.id.btnCurrentWhatsapp);
        bottomNavigation = findViewById(R.id.bottom_navigation);

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

        // Listener 2: As Claimer (all statuses; filter in UI for actions/history)
        claimerListenerReg = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "lf26")
                .collection("claims")
                .whereEqualTo("claimerUid", currentUserId)
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
            boolean isPoster = isCurrentUserPosterForClaim(d);
            boolean needsPosterReview = isPoster && "PENDING".equalsIgnoreCase(status);
            boolean needsClaimerConfirmation = !isPoster && "PENDING_CLAIMER_CONFIRMATION".equalsIgnoreCase(status);
            if (needsPosterReview || needsClaimerConfirmation) {
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
        boolean isPoster = isCurrentUserPosterForClaim(pending);
        String status = pending.getString("status");

        if (tvCurrentProcessingHeading != null) tvCurrentProcessingHeading.setVisibility(View.VISIBLE);
        if (cardCurrentProcessing != null) cardCurrentProcessing.setVisibility(View.VISIBLE);
        if (layoutApproveReject != null) layoutApproveReject.setVisibility(View.VISIBLE);
        if (tvCurrentStatus != null) {
            if ("PENDING_CLAIMER_CONFIRMATION".equalsIgnoreCase(status)) {
                tvCurrentStatus.setText("CONFIRMATION REQUIRED");
            } else {
                tvCurrentStatus.setText("PENDING");
            }
        }
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
            String receiver = isPoster ? pending.getString("claimerName") : pending.getString("posterName");
            tvCurrentReceiver.setText(receiver != null && !receiver.trim().isEmpty() ? receiver : "Unknown");
        }
        if (btnApproveProcessing instanceof TextView) {
            ((TextView) btnApproveProcessing).setText(isPoster ? "Approve" : "Yes");
        }
        if (btnRejectProcessing instanceof TextView) {
            ((TextView) btnRejectProcessing).setText(isPoster ? "Reject" : "No");
        }
        bindCurrentContactActions(pending);
        if (tvCurrentColorAnswer != null) {
            String colorAns = pending.getString("colorAns");
            tvCurrentColorAnswer.setText("Color: " + (colorAns != null && !colorAns.trim().isEmpty() ? colorAns.trim() : "-"));
        }
        if (tvCurrentBrandAnswer != null) {
            String brandAns = pending.getString("brandAns");
            tvCurrentBrandAnswer.setText("Brand: " + (brandAns != null && !brandAns.trim().isEmpty() ? brandAns.trim() : "-"));
        }
        if (tvCurrentMarkAnswer != null) {
            String markAns = pending.getString("markAns");
            tvCurrentMarkAnswer.setText("Mark: " + (markAns != null && !markAns.trim().isEmpty() ? markAns.trim() : "-"));
        }

        String claimUploadedImageUrl = firstNonEmpty(
                pending.getString("claimerPhotoUrl"),
                pending.getString("claimPhotoUrl"),
                pending.getString("uploadedImageUrl"),
                pending.getString("proofImageUrl"),
                pending.getString("imageUrl")
        );
        if (claimUploadedImageUrl != null && ivCurrentItem != null) {
            Glide.with(this)
                    .load(claimUploadedImageUrl)
                    .placeholder(R.drawable.electronics)
                    .centerCrop()
                    .into(ivCurrentItem);
            return;
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

    private String firstNonEmpty(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return null;
    }

    private void bindCurrentContactActions(DocumentSnapshot claimDoc) {
        String posterUid = claimDoc.getString("posterUid");
        String claimerUid = claimDoc.getString("claimerUid");
        String otherUserUid = null;
        if (!currentUserId.isEmpty()) {
            if (currentUserId.equals(posterUid)) {
                otherUserUid = claimerUid;
            } else if (currentUserId.equals(claimerUid)) {
                otherUserUid = posterUid;
            }
        }
        if (otherUserUid == null || otherUserUid.trim().isEmpty()) {
            otherUserUid = claimerUid != null && !claimerUid.trim().isEmpty() ? claimerUid : posterUid;
        }
        if (otherUserUid == null || otherUserUid.trim().isEmpty()) {
            setContactButtonsEnabled(false);
            return;
        }

        setContactButtonsEnabled(false);
        FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "lf26")
                .collection("users")
                .document(otherUserUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    currentContactPhone = safeText(documentSnapshot.getString("phone"));
                    currentContactEmail = safeText(documentSnapshot.getString("email"));
                    setupCurrentContactClickListeners();
                    setContactButtonsEnabled(true);
                })
                .addOnFailureListener(e -> setContactButtonsEnabled(false));
    }

    private void setupCurrentContactClickListeners() {
        if (btnCurrentCall != null) {
            btnCurrentCall.setOnClickListener(v -> {
                if (currentContactPhone.isEmpty()) {
                    Toast.makeText(this, "No phone number available.", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    Intent callIntent = new Intent(Intent.ACTION_DIAL);
                    callIntent.setData(Uri.parse("tel:" + currentContactPhone));
                    startActivity(callIntent);
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(this, "No dialer app found.", Toast.LENGTH_SHORT).show();
                }
            });
        }
        if (btnCurrentEmail != null) {
            btnCurrentEmail.setOnClickListener(v -> {
                if (currentContactEmail.isEmpty()) {
                    Toast.makeText(this, "No email available.", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                    emailIntent.setData(Uri.parse("mailto:" + currentContactEmail));
                    startActivity(emailIntent);
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(this, "No email app found.", Toast.LENGTH_SHORT).show();
                }
            });
        }
        if (btnCurrentWhatsapp != null) {
            btnCurrentWhatsapp.setOnClickListener(v -> {
                if (currentContactPhone.isEmpty()) {
                    Toast.makeText(this, "No phone number available.", Toast.LENGTH_SHORT).show();
                    return;
                }
                String url = "https://api.whatsapp.com/send?phone=" + currentContactPhone;
                try {
                    Intent whatsappIntent = new Intent(Intent.ACTION_VIEW);
                    whatsappIntent.setData(Uri.parse(url));
                    startActivity(whatsappIntent);
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(this, "WhatsApp not installed.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setContactButtonsEnabled(boolean enabled) {
        if (btnCurrentCall != null) {
            btnCurrentCall.setEnabled(enabled);
            btnCurrentCall.setAlpha(enabled ? 1f : 0.5f);
        }
        if (btnCurrentEmail != null) {
            btnCurrentEmail.setEnabled(enabled);
            btnCurrentEmail.setAlpha(enabled ? 1f : 0.5f);
        }
        if (btnCurrentWhatsapp != null) {
            btnCurrentWhatsapp.setEnabled(enabled);
            btnCurrentWhatsapp.setAlpha(enabled ? 1f : 0.5f);
        }
    }

    private String safeText(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "Not available".equalsIgnoreCase(trimmed)) return "";
        return trimmed;
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
        boolean isPoster = isCurrentUserPosterForClaim(claimDoc);
        String status = claimDoc.getString("status");
        if (isPoster && "PENDING".equalsIgnoreCase(status)) {
            updateClaimStatus(claimDoc, "PENDING_CLAIMER_CONFIRMATION");
        } else if (!isPoster && "PENDING_CLAIMER_CONFIRMATION".equalsIgnoreCase(status)) {
            updateClaimStatus(claimDoc, "APPROVED");
        }
    }

    @Override
    public void onReject(DocumentSnapshot claimDoc) {
        boolean isPoster = isCurrentUserPosterForClaim(claimDoc);
        String status = claimDoc.getString("status");
        if (isPoster && "PENDING".equalsIgnoreCase(status)) {
            updateClaimStatus(claimDoc, "REJECTED");
        } else if (!isPoster && "PENDING_CLAIMER_CONFIRMATION".equalsIgnoreCase(status)) {
            updateClaimStatus(claimDoc, "REJECTED");
        }
    }

    @Override
    public void onOpenClaim(DocumentSnapshot claimDoc) {
        String status = claimDoc.getString("status");
        if (!"APPROVED".equalsIgnoreCase(status)) {
            return;
        }
        bindCurrentContactActions(claimDoc);
        showApprovedClaimContactDialog();
    }

    private void updateClaimStatus(DocumentSnapshot claimDoc, String newStatus) {
        String claimId = claimDoc.getId();
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        if ("APPROVED".equals(newStatus)) {
            updates.put("approvedByName", resolveCurrentUserName());
        }

        FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "lf26").collection("claims").document(claimId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Claim " + newStatus.toLowerCase().replace('_', ' '), Toast.LENGTH_SHORT).show();
                    
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

    private boolean isCurrentUserPosterForClaim(DocumentSnapshot claimDoc) {
        String posterUid = claimDoc.getString("posterUid");
        return posterUid != null && posterUid.equals(currentUserId);
    }

    private String resolveCurrentUserName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return "Poster";
        String displayName = user.getDisplayName();
        if (displayName != null && !displayName.trim().isEmpty()) return displayName.trim();
        String email = user.getEmail();
        if (email != null && !email.trim().isEmpty()) return email.trim();
        return "Poster";
    }

    private void showApprovedClaimContactDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_claim_contact_actions, null, false);
        Button btnCall = dialogView.findViewById(R.id.btnDialogCall);
        Button btnEmail = dialogView.findViewById(R.id.btnDialogEmail);
        Button btnWhatsApp = dialogView.findViewById(R.id.btnDialogWhatsapp);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Contact options")
                .setView(dialogView)
                .setNegativeButton("Close", null)
                .create();

        if (btnCall != null) btnCall.setOnClickListener(v -> {
            if (currentContactPhone.isEmpty()) {
                Toast.makeText(this, "No phone number available.", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + currentContactPhone));
                startActivity(callIntent);
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, "No dialer app found.", Toast.LENGTH_SHORT).show();
            }
        });

        if (btnEmail != null) btnEmail.setOnClickListener(v -> {
            if (currentContactEmail.isEmpty()) {
                Toast.makeText(this, "No email available.", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:" + currentContactEmail));
                startActivity(emailIntent);
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, "No email app found.", Toast.LENGTH_SHORT).show();
            }
        });

        if (btnWhatsApp != null) btnWhatsApp.setOnClickListener(v -> {
            if (currentContactPhone.isEmpty()) {
                Toast.makeText(this, "No phone number available.", Toast.LENGTH_SHORT).show();
                return;
            }
            String url = "https://api.whatsapp.com/send?phone=" + currentContactPhone;
            try {
                Intent whatsappIntent = new Intent(Intent.ACTION_VIEW);
                whatsappIntent.setData(Uri.parse(url));
                startActivity(whatsappIntent);
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, "WhatsApp not installed.", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (posterListenerReg != null) posterListenerReg.remove();
        if (claimerListenerReg != null) claimerListenerReg.remove();
    }
}