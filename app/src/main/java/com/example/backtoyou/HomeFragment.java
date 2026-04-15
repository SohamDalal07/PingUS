package com.example.backtoyou;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private RecyclerView       rvFeed;
    private LinearLayout       layoutEmptyState;
    private EditText           etSearch;
    private TextView           tvSectionTitle, tvViewAll;
    private TextView           tvActiveCount, tvFoundCount;
    private TextView           chipAll, chipClaims, chipLost, chipFound, chipMine;

    private FeedCardAdapter                 adapter;
    private final List<Map<String, Object>> allItems      = new ArrayList<>();
    private final List<Map<String, Object>> filteredItems = new ArrayList<>();
    private final Map<String, Map<String, Object>> approvedClaimsByItemId = new HashMap<>();
    private ListenerRegistration            listenerReg;
    private ListenerRegistration            claimsListenerReg;

    private String currentFilter = "ALL";
    private String searchQuery   = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupToolbar(view);
        setupRecyclerView();
        setupChips();
        setupSearch();
        setupViewAll();
        setFilter("ALL");
        listenToFeed();
        listenToApprovedClaimsForPoster();
    }

    private void bindViews(View view) {
        rvFeed           = view.findViewById(R.id.rvFeed);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        etSearch         = view.findViewById(R.id.etSearch);
        tvSectionTitle   = view.findViewById(R.id.tvSectionTitle);
        tvViewAll        = view.findViewById(R.id.tvViewAll);
        tvActiveCount    = view.findViewById(R.id.tvActiveCount);
        tvFoundCount     = view.findViewById(R.id.tvFoundCount);
        chipAll          = view.findViewById(R.id.chipAll);
        chipClaims       = view.findViewById(R.id.chipClaims);
        chipLost         = view.findViewById(R.id.chipLost);
        chipFound        = view.findViewById(R.id.chipFound);
        chipMine         = view.findViewById(R.id.chipMine);
    }

    private void setupToolbar(View view) {
        view.findViewById(R.id.btnAddPost).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), PostActivity.class)));
    }

    private void setupViewAll() {
        if (tvViewAll != null) {
            tvViewAll.setOnClickListener(v -> setFilter("ALL"));
        }
    }

    private void setupRecyclerView() {
        adapter = new FeedCardAdapter(filteredItems, item -> {
            if (isClaimModeItem(item)) {
                openClaimContactActions(item);
                return;
            }
            Intent intent = new Intent(getActivity(), ItemDetailActivity.class);
            intent.putExtra("title", getString(item, "title"));
            intent.putExtra("type", getString(item, "type"));
            intent.putExtra("category", getString(item, "category"));
            intent.putExtra("location", getString(item, "location"));
            intent.putExtra("description", getString(item, "description"));
            intent.putExtra("postedByName", getString(item, "postedByName"));
            intent.putExtra("postedByUid", getString(item, "postedByUid"));
            intent.putExtra("itemId", getString(item, "itemId"));
            
            Object postedAtObj = item.get("postedAt");
            if (postedAtObj instanceof Number) {
                intent.putExtra("postedAt", ((Number) postedAtObj).longValue());
            }
            
            startActivity(intent);
        });
        rvFeed.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFeed.setAdapter(adapter);
        rvFeed.setHasFixedSize(false);
        rvFeed.setNestedScrollingEnabled(false);
    }

    private void listenToApprovedClaimsForPoster() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String currentUserId = user.getUid();

        claimsListenerReg = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "lf26")
                .collection("claims")
                .whereEqualTo("posterUid", currentUserId)
                .whereEqualTo("status", "APPROVED")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    approvedClaimsByItemId.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String itemId = doc.getString("itemId");
                        if (itemId == null || itemId.trim().isEmpty()) continue;
                        Map<String, Object> claim = new HashMap<>();
                        if (doc.getData() != null) claim.putAll(doc.getData());
                        claim.put("claimId", doc.getId());
                        approvedClaimsByItemId.put(itemId, claim);
                    }
                    applyFilterAndSearch();
                });
    }

    private void setupChips() {
        chipAll.setOnClickListener(v    -> setFilter("ALL"));
        chipClaims.setOnClickListener(v -> setFilter("CLAIMS"));
        chipLost.setOnClickListener(v   -> setFilter("LOST"));
        chipFound.setOnClickListener(v  -> setFilter("FOUND"));
        chipMine.setOnClickListener(v   -> setFilter("MINE"));
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        resetChips();
        if (tvSectionTitle != null) {
            switch (filter) {
                case "ALL":    styleChipSelected(chipAll);    tvSectionTitle.setText("Recent Updates"); break;
                case "CLAIMS": styleChipSelected(chipClaims); tvSectionTitle.setText("Your Item Claims"); break;
                case "LOST":   styleChipSelected(chipLost);   tvSectionTitle.setText("Lost Items"); break;
                case "FOUND":  styleChipSelected(chipFound);  tvSectionTitle.setText("Found Items"); break;
                case "MINE":   styleChipSelected(chipMine);   tvSectionTitle.setText("My Posts"); break;
            }
        }
        applyFilterAndSearch();
    }

    private void resetChips() {
        for (TextView c : new TextView[]{chipAll, chipClaims, chipLost, chipFound, chipMine}) {
            c.setBackgroundResource(R.drawable.bg_home_chip_unselected);
            c.setTextColor(requireContext().getColor(R.color.home_chip_unselected_text));
        }
    }

    private void styleChipSelected(TextView chip) {
        chip.setBackgroundResource(R.drawable.bg_home_chip_selected);
        chip.setTextColor(requireContext().getColor(R.color.home_tab_active_text));
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                searchQuery = s.toString().trim().toLowerCase();
                applyFilterAndSearch();
            }
            @Override public void afterTextChanged(Editable e) {}
        });
    }

    private void listenToFeed() {
        listenerReg = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "lf26")
                .collection("items")
                .orderBy("postedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    allItems.clear();
                    int lost = 0, found = 0;
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Map<String, Object> item = doc.getData();
                        item.put("itemId", doc.getId());
                        String type = getString(item, "type");
                        // Home feed is public for all users and shows only active LOST/FOUND style posts.
                        if ("LOST".equals(type) || "FOUND".equals(type)) {
                            allItems.add(item);
                            if ("LOST".equals(type))  lost++;
                            if ("FOUND".equals(type)) found++;
                        }
                    }
                    tvActiveCount.setText(String.valueOf(allItems.size()));
                    tvFoundCount.setText(String.valueOf(found));
                    applyFilterAndSearch();
                });
    }

    private void applyFilterAndSearch() {
        filteredItems.clear();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserId = user != null ? user.getUid() : "test_user_001";

        for (Map<String, Object> item : allItems) {
            String type     = getString(item, "type");
            String location = getString(item, "location").toLowerCase();
            String title    = getString(item, "title").toLowerCase();
            String category = getString(item, "category").toLowerCase();
            String postedByUid = getString(item, "postedByUid");
            boolean passesFilter = true;
            switch (currentFilter) {
                case "LOST":   passesFilter = "LOST".equals(type);         break;
                case "FOUND":  passesFilter = "FOUND".equals(type);        break;
                case "CLAIMS": {
                    String itemId = getString(item, "itemId");
                    Map<String, Object> claim = approvedClaimsByItemId.get(itemId);
                    if (claim != null) {
                        item.put("isClaimModeItem", true);
                        item.put("claimClaimerUid", claim.get("claimerUid"));
                        item.put("claimClaimerName", claim.get("claimerName"));
                    } else {
                        item.remove("isClaimModeItem");
                        item.remove("claimClaimerUid");
                        item.remove("claimClaimerName");
                    }
                    passesFilter = currentUserId.equals(postedByUid) && claim != null;
                    break;
                }
                case "MINE":   passesFilter = currentUserId.equals(postedByUid); break;
            }
            boolean passesSearch = searchQuery.isEmpty()
                    || title.contains(searchQuery)
                    || category.contains(searchQuery)
                    || location.contains(searchQuery);
            if (passesFilter && passesSearch) filteredItems.add(item);
        }
        adapter.notifyDataSetChanged();
        boolean empty = filteredItems.isEmpty();
        rvFeed.setVisibility(empty ? View.GONE : View.VISIBLE);
        layoutEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private boolean isClaimModeItem(Map<String, Object> item) {
        Object flag = item.get("isClaimModeItem");
        return flag instanceof Boolean && (Boolean) flag;
    }

    private void openClaimContactActions(Map<String, Object> item) {
        String claimerUid = getString(item, "claimClaimerUid");
        if (claimerUid.isEmpty()) {
            Toast.makeText(requireContext(), "No claimer details available.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "lf26")
                .collection("users")
                .document(claimerUid)
                .get()
                .addOnSuccessListener(doc -> {
                    String phone = valueOrEmpty(doc.getString("phone"));
                    String email = valueOrEmpty(doc.getString("email"));
                    showClaimContactDialog(phone, email);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Unable to load contact details.", Toast.LENGTH_SHORT).show());
    }

    private void showClaimContactDialog(String phone, String email) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_home_claim_contact_actions, null, false);
        Button btnCall = dialogView.findViewById(R.id.btnHomeDialogCall);
        Button btnMessage = dialogView.findViewById(R.id.btnHomeDialogMessage);
        Button btnEmail = dialogView.findViewById(R.id.btnHomeDialogEmail);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Contact claimer")
                .setView(dialogView)
                .setNegativeButton("Close", null)
                .create();

        btnCall.setOnClickListener(v -> {
            if (phone.isEmpty()) {
                Toast.makeText(requireContext(), "No phone number available.", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)));
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(requireContext(), "No dialer app found.", Toast.LENGTH_SHORT).show();
            }
        });

        btnMessage.setOnClickListener(v -> {
            if (phone.isEmpty()) {
                Toast.makeText(requireContext(), "No phone number available.", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + phone)));
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(requireContext(), "No messaging app found.", Toast.LENGTH_SHORT).show();
            }
        });

        btnEmail.setOnClickListener(v -> {
            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "No email available.", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email)));
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(requireContext(), "No email app found.", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private String valueOrEmpty(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "Not available".equalsIgnoreCase(trimmed)) return "";
        return trimmed;
    }

    private String getEmptyStateMessage() {
        if (!searchQuery.isEmpty()) {
            return getString(R.string.home_empty_search);
        }

        switch (currentFilter) {
            case "LOST":
                return getString(R.string.home_empty_lost);
            case "FOUND":
                return getString(R.string.home_empty_found);
            case "CLAIMS":
                return "No claim-related items right now.";
            case "MINE":
                return getString(R.string.home_empty_mine);
            default:
                return getString(R.string.home_empty_default);
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerReg != null) listenerReg.remove();
        if (claimsListenerReg != null) claimsListenerReg.remove();
    }
}
