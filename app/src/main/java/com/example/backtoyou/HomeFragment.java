package com.example.backtoyou;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private RecyclerView       rvFeed;
    private LinearLayout       layoutEmptyState;
    private EditText           etSearch;
    private TextView           tvActiveCount, tvFoundCount, tvSectionTitle, tvViewAll;
    private TextView           chipAll, chipClaims, chipLost, chipFound, chipMine;

    private FeedCardAdapter                 adapter;
    private final List<Map<String, Object>> allItems      = new ArrayList<>();
    private final List<Map<String, Object>> filteredItems = new ArrayList<>();
    private final Map<String, String> userNameCache       = new HashMap<>();
    private ListenerRegistration            listenerReg;

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
        setupFAB(view);
        setupRecyclerView();
        setupChips();
        setupSearch();
        setupViewAll();
        setFilter("ALL");
        listenToFeed();
    }

    private void bindViews(View view) {
        rvFeed           = view.findViewById(R.id.rvFeed);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        etSearch         = view.findViewById(R.id.etSearch);
        tvActiveCount    = view.findViewById(R.id.tvActiveCount);
        tvFoundCount     = view.findViewById(R.id.tvFoundCount);
        tvSectionTitle   = view.findViewById(R.id.tvSectionTitle);
        tvViewAll        = view.findViewById(R.id.tvViewAll);
        chipAll          = view.findViewById(R.id.chipAll);
        chipClaims       = view.findViewById(R.id.chipClaims);
        chipLost         = view.findViewById(R.id.chipLost);
        chipFound        = view.findViewById(R.id.chipFound);
        chipMine         = view.findViewById(R.id.chipMine);
    }

    private void setupFAB(View view) {
        View fab = view.findViewById(R.id.btnAddPost);
        if (fab != null) {
            fab.setOnClickListener(v ->
                    startActivity(new Intent(getActivity(), PostActivity.class)));
        }
    }

    private void setupViewAll() {
        if (tvViewAll != null) {
            tvViewAll.setOnClickListener(v -> {
                setFilter("ALL");
                Toast.makeText(getActivity(), "Showing all updates", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupRecyclerView() {
        adapter = new FeedCardAdapter(filteredItems, item -> {
            handleFeedItemClick(item);
        });
        rvFeed.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFeed.setAdapter(adapter);
    }

    private void handleFeedItemClick(Map<String, Object> item) {
        if (!isAdded() || getActivity() == null || item == null) return;
        // Always open item details from Home on item click.
        openItemDetails(item);
    }

    private void openItemDetails(Map<String, Object> item) {
        if (!isAdded() || getActivity() == null) return;
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
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Unable to open item details.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupChips() {
        if (chipAll != null) chipAll.setOnClickListener(v -> setFilter("ALL"));
        if (chipClaims != null) chipClaims.setOnClickListener(v -> setFilter("CLAIMS"));
        if (chipLost != null) chipLost.setOnClickListener(v -> setFilter("LOST"));
        if (chipFound != null) chipFound.setOnClickListener(v -> setFilter("FOUND"));
        if (chipMine != null) chipMine.setOnClickListener(v -> setFilter("MINE"));
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
        TextView[] chips = {chipAll, chipClaims, chipLost, chipFound, chipMine};
        for (TextView c : chips) {
            if (c == null) continue;
            c.setBackgroundResource(R.drawable.bg_home_chip_unselected);
            c.setTextColor(requireContext().getColor(R.color.home_chip_unselected_text));
        }
    }

    private void styleChipSelected(TextView chip) {
        if (chip == null) return;
        chip.setBackgroundResource(R.drawable.bg_home_chip_selected);
        chip.setTextColor(requireContext().getColor(R.color.home_tab_active_text));
    }

    private void setupSearch() {
        if (etSearch == null) return;
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
        listenerReg = FirebaseFirestore.getInstance()
                .collection("items")
                .whereEqualTo("status", "ACTIVE")
                .orderBy("postedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    allItems.clear();
                    int lostCount = 0, foundCount = 0;
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Map<String, Object> item = doc.getData();
                        item.put("itemId", doc.getId());
                        ensurePosterName(item);
                        allItems.add(item);
                        if ("LOST".equals(item.get("type"))) lostCount++;
                        if ("FOUND".equals(item.get("type"))) foundCount++;
                    }
                    if (tvActiveCount != null) tvActiveCount.setText(String.valueOf(lostCount + foundCount));
                    if (tvFoundCount != null) tvFoundCount.setText(String.valueOf(foundCount));
                    applyFilterAndSearch();
                });
    }

    private void applyFilterAndSearch() {
        filteredItems.clear();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String currentUid = user != null ? user.getUid() : "";

        for (Map<String, Object> item : allItems) {
            String type     = getString(item, "type");
            String title     = getString(item, "title").toLowerCase();
            String location  = getString(item, "location").toLowerCase();
            String categoryQ = getString(item, "category").toLowerCase();
            String postedBy  = getString(item, "postedByUid");
            
            boolean passesFilter = true;
            switch (currentFilter) {
                case "LOST":   passesFilter = "LOST".equals(type); break;
                case "FOUND":  passesFilter = "FOUND".equals(type); break;
                case "MINE":   passesFilter = currentUid.equals(postedBy); break;
                case "CLAIMS": passesFilter = currentUid.equals(postedBy); break; // Simplified
            }

            boolean passesSearch = searchQuery.isEmpty()
                    || title.contains(searchQuery)
                    || location.contains(searchQuery)
                    || categoryQ.contains(searchQuery);
            if (passesFilter && passesSearch) filteredItems.add(item);
        }
        if (adapter != null) adapter.notifyDataSetChanged();
        boolean empty = filteredItems.isEmpty();
        if (rvFeed != null) rvFeed.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (layoutEmptyState != null) layoutEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    private void ensurePosterName(Map<String, Object> item) {
        String currentName = getString(item, "postedByName").trim();
        if (!currentName.isEmpty() && !"anonymous".equalsIgnoreCase(currentName)) return;

        String uid = getString(item, "postedByUid").trim();
        if (uid.isEmpty()) return;

        String cached = userNameCache.get(uid);
        if (cached != null && !cached.isEmpty()) {
            item.put("postedByName", cached);
            return;
        }

        item.put("postedByName", "User " + uid.substring(0, Math.min(8, uid.length())));
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(userDoc -> {
                    String fullName = userDoc.getString("fullName");
                    String name = userDoc.getString("name");
                    String resolved = (fullName != null && !fullName.trim().isEmpty()) ? fullName.trim()
                            : ((name != null && !name.trim().isEmpty()) ? name.trim() : "");
                    if (resolved.isEmpty()) return;
                    userNameCache.put(uid, resolved);
                    for (Map<String, Object> row : allItems) {
                        if (uid.equals(getString(row, "postedByUid").trim())) {
                            row.put("postedByName", resolved);
                        }
                    }
                    if (adapter != null) adapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerReg != null) listenerReg.remove();
    }
}
