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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private RecyclerView       rvFeed;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout       layoutEmptyState;
    private EditText           etSearch;
    private TextView           tvEmptySubtitle;
    private TextView           tvActiveCount, tvLostCount, tvFoundCount;
    private TextView           chipAll, chipLost, chipFound, chipHostel, chipMine;

    private FeedCardAdapter                 adapter;
    private final List<Map<String, Object>> allItems      = new ArrayList<>();
    private final List<Map<String, Object>> filteredItems = new ArrayList<>();
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
        setupToolbar(view);
        setupRecyclerView();
        setupChips();
        setupSearch();
        setupSwipeRefresh();
        setFilter("ALL");
        listenToFeed();
    }

    private void bindViews(View view) {
        rvFeed           = view.findViewById(R.id.rvFeed);
        swipeRefresh     = view.findViewById(R.id.swipeRefresh);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        etSearch         = view.findViewById(R.id.etSearch);
        tvEmptySubtitle  = view.findViewById(R.id.tvEmptySubtitle);
        tvActiveCount    = view.findViewById(R.id.tvActiveCount);
        tvLostCount      = view.findViewById(R.id.tvLostCount);
        tvFoundCount     = view.findViewById(R.id.tvFoundCount);
        chipAll          = view.findViewById(R.id.chipAll);
        chipLost         = view.findViewById(R.id.chipLost);
        chipFound        = view.findViewById(R.id.chipFound);
        chipHostel       = view.findViewById(R.id.chipHostel);
        chipMine         = view.findViewById(R.id.chipMine);
    }

    private void setupToolbar(View view) {
        view.findViewById(R.id.btnAddPost).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), PostActivity.class)));
    }

    private void setupRecyclerView() {
        adapter = new FeedCardAdapter(filteredItems, item -> {
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

    private void setupChips() {
        chipAll.setOnClickListener(v    -> setFilter("ALL"));
        chipLost.setOnClickListener(v   -> setFilter("LOST"));
        chipFound.setOnClickListener(v  -> setFilter("FOUND"));
        chipHostel.setOnClickListener(v -> setFilter("HOSTEL"));
        chipMine.setOnClickListener(v   -> setFilter("MINE"));
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        resetChips();
        switch (filter) {
            case "ALL":    styleChipSelected(chipAll);    break;
            case "LOST":   styleChipSelected(chipLost);   break;
            case "FOUND":  styleChipSelected(chipFound);  break;
            case "HOSTEL": styleChipSelected(chipHostel); break;
            case "MINE":   styleChipSelected(chipMine);   break;
        }
        applyFilterAndSearch();
    }

    private void resetChips() {
        for (TextView c : new TextView[]{chipAll, chipLost, chipFound, chipHostel, chipMine}) {
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

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(requireContext().getColor(R.color.home_primary_blue));
        swipeRefresh.setOnRefreshListener(() -> swipeRefresh.setRefreshing(false));
    }

    private void listenToFeed() {
        listenerReg = FirebaseFirestore.getInstance()
                .collection("items")
                .whereEqualTo("status", "ACTIVE")
                .orderBy("postedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    allItems.clear();
                    int lost = 0, found = 0;
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Map<String, Object> item = doc.getData();
                        item.put("itemId", doc.getId());
                        allItems.add(item);
                        if ("LOST".equals(item.get("type")))  lost++;
                        if ("FOUND".equals(item.get("type"))) found++;
                    }
                    tvActiveCount.setText(String.valueOf(allItems.size()));
                    tvLostCount.setText(String.valueOf(lost));
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
                case "HOSTEL": passesFilter = location.contains("hostel"); break;
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
        tvEmptySubtitle.setText(getEmptyStateMessage());
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
            case "HOSTEL":
                return getString(R.string.home_empty_hostel);
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
    }
}
