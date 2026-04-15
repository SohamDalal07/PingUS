package com.example.backtoyou;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseApp;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class Home extends AppCompatActivity {

    private ListenerRegistration pendingPosterListener;
    private ListenerRegistration pendingClaimerListener;
    private boolean pendingAsPoster;
    private boolean pendingAsClaimer;
    private boolean redirectedForFirstPost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        Toolbar toolbar = findViewById(R.id.top_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.home_toolbar_title);
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.home_brand_blue));

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_post) {
                startActivity(new Intent(Home.this, PostActivity.class));
                return true;
            } else if (itemId == R.id.navigation_alerts) {
                startActivity(new Intent(Home.this, AlertsActivity.class));
                return true;
            }
            else if (itemId == R.id.navigation_profile) {
                startActivity(new Intent(Home.this, ProfileActivity.class));
                return true;
            }

            else if (itemId == R.id.navigation_home) {
                // Already on Home
                return true;
            }
            return false;
        });


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        enforceFirstPostGate();
        attachPendingClaimBadgeListeners();
        Toolbar toolbar = findViewById(R.id.top_toolbar);
        if (toolbar != null) {
            toolbar.post(this::refreshAlertsBellFromPending);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (pendingPosterListener != null) {
            pendingPosterListener.remove();
            pendingPosterListener = null;
        }
        if (pendingClaimerListener != null) {
            pendingClaimerListener.remove();
            pendingClaimerListener = null;
        }
    }

    private void attachPendingClaimBadgeListeners() {
        if (pendingPosterListener != null) {
            pendingPosterListener.remove();
            pendingPosterListener = null;
        }
        if (pendingClaimerListener != null) {
            pendingClaimerListener.remove();
            pendingClaimerListener = null;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            updateAlertsBellIcon(false);
            return;
        }
        String uid = user.getUid();

        pendingPosterListener = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "lf26")
                .collection("claims")
                .whereEqualTo("posterUid", uid)
                .addSnapshotListener((snap, e) -> {
                    pendingAsPoster = false;
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            String status = doc.getString("status");
                            if ("PENDING".equalsIgnoreCase(status) || "APPROVED".equalsIgnoreCase(status)) {
                                pendingAsPoster = true;
                                break;
                            }
                        }
                    }
                    refreshAlertsBellFromPending();
                });

        pendingClaimerListener = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "lf26")
                .collection("claims")
                .whereEqualTo("claimerUid", uid)
                .addSnapshotListener((snap, e) -> {
                    pendingAsClaimer = false;
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            String status = doc.getString("status");
                            if ("PENDING_CLAIMER_CONFIRMATION".equalsIgnoreCase(status)) {
                                pendingAsClaimer = true;
                                break;
                            }
                        }
                    }
                    refreshAlertsBellFromPending();
                });
    }

    private void refreshAlertsBellFromPending() {
        updateAlertsBellIcon(pendingAsPoster || pendingAsClaimer);
    }

    private void enforceFirstPostGate() {
        if (redirectedForFirstPost) return;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "lf26")
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    boolean hasPostedFirstItem = doc.exists() && Boolean.TRUE.equals(doc.getBoolean("hasPostedFirstItem"));
                    if (!hasPostedFirstItem) {
                        redirectedForFirstPost = true;
                        Intent intent = new Intent(Home.this, PostActivity.class);
                        intent.putExtra("forceFirstPost", true);
                        startActivity(intent);
                        finish();
                    }
                });
    }

    private void updateAlertsBellIcon(boolean hasPending) {
        Toolbar toolbar = findViewById(R.id.top_toolbar);
        if (toolbar == null) return;
        Menu menu = toolbar.getMenu();
        if (menu == null) return;
        android.view.MenuItem item = menu.findItem(R.id.action_alerts);
        if (item == null) return;
        Drawable d = ContextCompat.getDrawable(this, R.drawable.ic_bell);
        if (d == null) return;
        d = DrawableCompat.wrap(d.mutate());
        int color = ContextCompat.getColor(this, hasPending ? R.color.error_red : R.color.home_meta_text);
        DrawableCompat.setTint(d, color);
        item.setIcon(d);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_alerts) {
            startActivity(new Intent(Home.this, AlertsActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(Home.this, ProfileActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
