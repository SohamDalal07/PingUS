package com.example.backtoyou;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class FeedCardAdapter extends RecyclerView.Adapter<FeedCardAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Map<String, Object> item);
    }

    private final List<Map<String, Object>> items;
    private final OnItemClickListener listener;

    public FeedCardAdapter(List<Map<String, Object>> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home_feed_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Map<String, Object> item = items.get(position);

        String title = getString(item, "title");
        String type = getString(item, "type");
        String location = getString(item, "location");
        String category = getString(item, "category");
        String postedByName = getString(item, "postedByName");
        String postedByUid = getString(item, "postedByUid");
        long postedAt = item.get("postedAt") != null
                ? ((Number) item.get("postedAt")).longValue() : 0L;

        h.tvTitle.setText(title);
        h.tvLocation.setText(location);
        h.tvTimeBadge.setText(getTimeAgo(postedAt));

        int hero = CategoryDrawableHelper.drawableResForCategory(category, R.drawable.bg_home_header_gradient);
        h.ivItemImage.setImageResource(hero);
        h.ivItemImage.setImageTintList(null);
        h.tvUserName.setText(resolvePosterLabel(postedByName, postedByUid));

        if ("LOST".equals(type)) {
            h.tvStatusBadge.setText("LOST");
            h.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_lost);
            h.tvStatusBadge.setTextColor(Color.WHITE);
            h.btnCardAction.setText("Contact");
        } else if ("FOUND".equals(type)) {
            h.tvStatusBadge.setText("FOUND");
            h.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_found);
            h.tvStatusBadge.setTextColor(Color.WHITE);
            h.btnCardAction.setText("Claim");
        } else {
            h.tvStatusBadge.setText("CLAIMED");
            h.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_claimed);
            h.tvStatusBadge.setTextColor(Color.WHITE);
            h.btnCardAction.setText("View");
        }

        // Use the item bound to this row. Listener is replaced on every bind, so this stays
        // correct after recycling (unlike capturing a position int from bind).
        View.OnClickListener openDetailsClick = v -> {
            if (listener == null) return;
            listener.onItemClick(item);
        };

        h.itemView.setOnClickListener(openDetailsClick);
        h.ivItemImage.setOnClickListener(openDetailsClick);
        h.ivUserAvatar.setOnClickListener(openDetailsClick);
        h.tvTitle.setOnClickListener(openDetailsClick);
        h.tvLocation.setOnClickListener(openDetailsClick);
        h.tvStatusBadge.setOnClickListener(openDetailsClick);
        h.tvTimeBadge.setOnClickListener(openDetailsClick);
        h.tvUserName.setOnClickListener(openDetailsClick);
        h.btnCardAction.setOnClickListener(openDetailsClick);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String getTimeAgo(long postedAt) {
        if (postedAt == 0) return "";
        long diff = System.currentTimeMillis() - postedAt;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        long days = TimeUnit.MILLISECONDS.toDays(diff);
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + "m ago";
        if (hours < 24) return hours + "h ago";
        if (days == 1) return "Yesterday";
        return days + "d ago";
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    private String resolvePosterLabel(String postedByName, String postedByUid) {
        if (postedByName != null && !postedByName.trim().isEmpty()) return postedByName.trim();
        if (postedByUid != null && !postedByUid.trim().isEmpty()) {
            int len = Math.min(8, postedByUid.length());
            return "User " + postedByUid.substring(0, len);
        }
        return "User";
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivItemImage, ivUserAvatar;
        TextView tvTitle, tvLocation, tvStatusBadge, tvTimeBadge, tvUserName;
        Button btnCardAction;

        ViewHolder(@NonNull View v) {
            super(v);
            ivItemImage = v.findViewById(R.id.ivItemImage);
            ivUserAvatar = v.findViewById(R.id.ivUserAvatar);
            tvTitle = v.findViewById(R.id.tvItemTitle);
            tvLocation = v.findViewById(R.id.tvItemLocation);
            tvStatusBadge = v.findViewById(R.id.tvStatusBadge);
            tvTimeBadge = v.findViewById(R.id.tvTimeBadge);
            tvUserName = v.findViewById(R.id.tvUserName);
            btnCardAction = v.findViewById(R.id.btnCardAction);
        }
    }
}
