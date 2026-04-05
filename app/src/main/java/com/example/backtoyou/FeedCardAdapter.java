package com.example.backtoyou;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
        Context ctx = h.itemView.getContext();
        String title = getString(item, "title");
        String type = getString(item, "type");
        String location = getString(item, "location");
        String category = getString(item, "category");
        long postedAt = item.get("postedAt") != null
                ? ((Number) item.get("postedAt")).longValue() : 0L;

        h.tvTitle.setText(title);
        h.tvLocation.setText(location + " - " + getTimeAgo(postedAt));

        switch (type) {
            case "LOST":
                h.tvBadge.setText("Lost");
                h.tvBadge.setBackgroundResource(R.drawable.bg_badge_lost);
                h.tvBadge.setTextColor(ctx.getColor(R.color.home_lost_text));
                break;
            case "FOUND":
                h.tvBadge.setText("Found");
                h.tvBadge.setBackgroundResource(R.drawable.bg_badge_found);
                h.tvBadge.setTextColor(ctx.getColor(R.color.home_found_text));
                break;
            default:
                h.tvBadge.setText("Claimed");
                h.tvBadge.setBackgroundResource(R.drawable.bg_badge_claimed);
                h.tvBadge.setTextColor(ctx.getColor(R.color.home_claimed_text));
                break;
        }

        setCategoryIcon(ctx, h, category);

        h.tvDept.setText("NMIMS");
        h.tvDept.setBackgroundResource(R.drawable.bg_dept_tag);
        h.tvDept.setTextColor(ctx.getColor(R.color.home_hint_text));

        h.itemView.setOnClickListener(v -> listener.onItemClick(item));
        h.itemView.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(120).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                    break;
                default:
                    break;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void setCategoryIcon(Context ctx, ViewHolder h, String category) {
        String cat = category.toLowerCase();
        h.layoutIconBox.setBackgroundResource(R.drawable.bg_home_icon_box);

        if (cat.contains("phone") || cat.contains("tablet") || cat.contains("charger")
                || cat.contains("earphone") || cat.contains("airpod") || cat.contains("power bank")) {
            h.ivIcon.setImageResource(R.drawable.ic_category_electronics);
            h.ivIcon.setColorFilter(ctx.getColor(R.color.home_primary_blue));
        } else if (cat.contains("key")) {
            h.ivIcon.setImageResource(R.drawable.ic_category_keys);
            h.ivIcon.setColorFilter(ctx.getColor(R.color.home_found_text));
        } else if (cat.contains("lab coat") || cat.contains("apron")
                || cat.contains("goggles") || cat.contains("lab equipment")
                || cat.contains("mortar") || cat.contains("spatula")) {
            h.ivIcon.setImageResource(R.drawable.ic_category_lab);
            h.ivIcon.setColorFilter(ctx.getColor(R.color.home_found_text));
        } else if (cat.contains("wallet") || cat.contains("cash") || cat.contains("card")) {
            h.ivIcon.setImageResource(R.drawable.ic_category_wallet);
            h.ivIcon.setColorFilter(ctx.getColor(R.color.home_lost_text));
        } else if (cat.contains("bag") || cat.contains("backpack")) {
            h.ivIcon.setImageResource(R.drawable.ic_category_bag);
            h.ivIcon.setColorFilter(ctx.getColor(R.color.home_meta_text));
        } else if (cat.contains("water bottle")) {
            h.ivIcon.setImageResource(R.drawable.ic_category_bottle);
            h.ivIcon.setColorFilter(ctx.getColor(R.color.home_primary_blue));
        } else if (cat.contains("calculator") || cat.contains("usb") || cat.contains("hard drive")) {
            h.ivIcon.setImageResource(R.drawable.ic_category_electronics);
            h.ivIcon.setColorFilter(ctx.getColor(R.color.home_primary_blue));
        } else if (cat.contains("book") || cat.contains("notes") || cat.contains("drawing")) {
            h.ivIcon.setImageResource(R.drawable.ic_category_book);
            h.ivIcon.setColorFilter(ctx.getColor(R.color.home_found_text));
        } else if (cat.contains("id card") || cat.contains("library card")) {
            h.ivIcon.setImageResource(R.drawable.ic_category_id);
            h.ivIcon.setColorFilter(ctx.getColor(R.color.home_primary_blue));
        } else {
            h.ivIcon.setImageResource(R.drawable.ic_category_other);
            h.ivIcon.setColorFilter(ctx.getColor(R.color.home_meta_text));
        }
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutIconBox;
        ImageView ivIcon;
        TextView tvTitle, tvLocation, tvBadge, tvDept;

        ViewHolder(@NonNull View v) {
            super(v);
            layoutIconBox = v.findViewById(R.id.layoutIconBox);
            ivIcon = v.findViewById(R.id.ivCategoryIcon);
            tvTitle = v.findViewById(R.id.tvItemTitle);
            tvLocation = v.findViewById(R.id.tvItemLocation);
            tvBadge = v.findViewById(R.id.tvStatusBadge);
            tvDept = v.findViewById(R.id.tvDeptTag);
        }
    }
}
