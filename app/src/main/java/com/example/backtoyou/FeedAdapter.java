package com.example.backtoyou;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Map<String, Object> item);
    }

    private final List<Map<String, Object>> items;
    private final OnItemClickListener       listener;

    public FeedAdapter(List<Map<String, Object>> items, OnItemClickListener listener) {
        this.items    = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_feed_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Map<String, Object> item = items.get(position);
        Context ctx = h.itemView.getContext();
        String title    = getString(item, "title");
        String type     = getString(item, "type");
        String location = getString(item, "location");
        String category = getString(item, "category");
        String poster   = getString(item, "postedByName");
        long   postedAt = item.get("postedAt") != null
                          ? ((Number) item.get("postedAt")).longValue() : 0L;

        h.tvTitle.setText(title);
        h.tvLocation.setText(location + " · " + getTimeAgo(postedAt));
        h.tvPoster.setText(poster.contains(" ")
                ? poster.split(" ")[0] + " " + poster.split(" ")[1].charAt(0) + "."
                : poster);

        switch (type) {
            case "LOST":
                h.tvBadge.setText("Lost");
                h.tvBadge.setBackgroundResource(R.drawable.bg_badge_lost);
                h.tvBadge.setTextColor(Color.WHITE);
                break;
            case "FOUND":
                h.tvBadge.setText("Found");
                h.tvBadge.setBackgroundResource(R.drawable.bg_badge_found);
                h.tvBadge.setTextColor(Color.WHITE);
                break;
            default:
                h.tvBadge.setText("Claimed");
                h.tvBadge.setBackgroundResource(R.drawable.bg_badge_claimed);
                h.tvBadge.setTextColor(Color.WHITE);
                break;
        }

        setCategoryIcon(ctx, h, category);

        h.tvDept.setText("NMIMS");
        h.tvDept.setBackgroundResource(R.drawable.bg_dept_tag);
        h.tvDept.setTextColor(ctx.getColor(R.color.colorMPSTMEText));

        h.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override public int getItemCount() { return items.size(); }

    private void setCategoryIcon(Context ctx, ViewHolder h, String category) {
        int res = CategoryDrawableHelper.drawableResForCategory(category, R.drawable.electronics);
        h.ivIcon.setImageResource(res);
        h.ivIcon.clearColorFilter();
        ImageViewCompat.setImageTintList(h.ivIcon, null);

        if (CategoryDrawableHelper.isCampusPhoto(res)) {
            if (res == R.drawable.electronics) {
                h.layoutIconBox.setBackgroundResource(R.drawable.bg_icon_blue);
            } else if (res == R.drawable.documents) {
                h.layoutIconBox.setBackgroundResource(R.drawable.bg_icon_purple);
            } else if (res == R.drawable.stationary) {
                h.layoutIconBox.setBackgroundResource(R.drawable.bg_icon_green);
            } else {
                h.layoutIconBox.setBackgroundResource(R.drawable.bg_icon_amber);
            }
            return;
        }
        h.layoutIconBox.setBackgroundResource(R.drawable.bg_icon_gray);
    }

    private String getTimeAgo(long postedAt) {
        if (postedAt == 0) return "";
        long diff    = System.currentTimeMillis() - postedAt;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        long hours   = TimeUnit.MILLISECONDS.toHours(diff);
        long days    = TimeUnit.MILLISECONDS.toDays(diff);
        if (minutes < 1)  return "Just now";
        if (minutes < 60) return minutes + "m ago";
        if (hours < 24)   return hours + "h ago";
        if (days == 1)    return "Yesterday";
        return days + "d ago";
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutIconBox;
        ImageView    ivIcon;
        TextView     tvTitle, tvLocation, tvBadge, tvDept, tvPoster;
        ViewHolder(@NonNull View v) {
            super(v);
            layoutIconBox = v.findViewById(R.id.layoutIconBox);
            ivIcon        = v.findViewById(R.id.ivCategoryIcon);
            tvTitle       = v.findViewById(R.id.tvItemTitle);
            tvLocation    = v.findViewById(R.id.tvItemLocation);
            tvBadge       = v.findViewById(R.id.tvStatusBadge);
            tvDept        = v.findViewById(R.id.tvDeptTag);
            tvPoster      = v.findViewById(R.id.tvPosterName);
        }
    }
}
