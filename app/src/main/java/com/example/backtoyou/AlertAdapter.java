package com.example.backtoyou;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.AlertViewHolder> {

    private final List<DocumentSnapshot> claimsList;
    private final OnClaimActionListener listener;
    private final String currentUserId;
    private final Map<String, ItemArtData> itemArtCache = new HashMap<>();

    public interface OnClaimActionListener {
        void onApprove(DocumentSnapshot claimDoc);
        void onReject(DocumentSnapshot claimDoc);
    }

    public AlertAdapter(List<DocumentSnapshot> claimsList, String currentUserId, OnClaimActionListener listener) {
        this.claimsList = claimsList;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alert_card, parent, false);
        return new AlertViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        DocumentSnapshot doc = claimsList.get(position);
        
        String title = doc.getString("itemTitle");
        String claimerName = doc.getString("claimerName");
        String color = doc.getString("colorAns");
        String brand = doc.getString("brandAns");
        String mark = doc.getString("markAns");
        String itemId = doc.getString("itemId");

        holder.tvItemTitle.setText(title != null ? title : "Unknown Item");
        holder.tvClaimerName.setText("Claimed by: " + (claimerName != null ? claimerName : "Unknown"));
        holder.tvColor.setText("Color: " + (color != null ? color : "-"));
        holder.tvBrand.setText("Brand: " + (brand != null ? brand : "-"));
        holder.tvMark.setText("Mark: " + (mark != null ? mark : "-"));
        bindItemImage(holder, itemId);

        String claimerUid = doc.getString("claimerUid");
        String status = doc.getString("status");
        boolean isClaimer = currentUserId != null && currentUserId.equals(claimerUid);
        holder.btnApprove.setVisibility(View.GONE);
        holder.btnReject.setVisibility(View.GONE);

        if ("APPROVED".equals(status)) {
            if (isClaimer) {
                holder.tvClaimerName.setText("Status: APPROVED by the poster!");
            } else {
                holder.tvClaimerName.setText("Status: You APPROVED this claim! (Item Claimed)");
            }
            holder.tvClaimerName.setTextColor(holder.itemView.getContext().getColor(R.color.colorFoundBg));
        } else if ("REJECTED".equals(status)) {
            holder.tvClaimerName.setText("Status: You REJECTED this claim.");
            holder.tvClaimerName.setTextColor(holder.itemView.getContext().getColor(R.color.colorLostBg));
        } else {
            // PENDING state
            if (isClaimer) {
                holder.tvClaimerName.setText("Status: PENDING poster approval.");
                holder.tvClaimerName.setTextColor(holder.itemView.getContext().getColor(R.color.colorTextSecondary));
            } else {
                holder.tvClaimerName.setText("Claimed by: " + (claimerName != null ? claimerName : "Unknown"));
                holder.tvClaimerName.setTextColor(holder.itemView.getContext().getColor(R.color.colorTextSecondary));
            }
        }
    }

    @Override
    public int getItemCount() {
        return claimsList.size();
    }

    static class AlertViewHolder extends RecyclerView.ViewHolder {
        ImageView ivItemPhoto;
        TextView tvItemTitle, tvClaimerName, tvColor, tvBrand, tvMark;
        Button btnApprove, btnReject;

        public AlertViewHolder(@NonNull View itemView) {
            super(itemView);
            ivItemPhoto = itemView.findViewById(R.id.ivAlertItemPhoto);
            tvItemTitle = itemView.findViewById(R.id.tvAlertItemTitle);
            tvClaimerName = itemView.findViewById(R.id.tvAlertClaimerName);
            tvColor = itemView.findViewById(R.id.tvAlertColor);
            tvBrand = itemView.findViewById(R.id.tvAlertBrand);
            tvMark = itemView.findViewById(R.id.tvAlertMark);
            btnApprove = itemView.findViewById(R.id.btnAlertApprove);
            btnReject = itemView.findViewById(R.id.btnAlertReject);
        }
    }

    private void bindItemImage(AlertViewHolder holder, String itemId) {
        if (holder.ivItemPhoto == null) return;
        int fallback = R.drawable.electronics;
        if (itemId == null || itemId.trim().isEmpty()) {
            holder.ivItemPhoto.setImageResource(fallback);
            return;
        }

        ItemArtData cached = itemArtCache.get(itemId);
        if (cached != null) {
            applyArt(holder.ivItemPhoto, cached, fallback);
            return;
        }

        holder.ivItemPhoto.setImageResource(fallback);
        FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "lf26").collection("items").document(itemId).get()
                .addOnSuccessListener(itemDoc -> {
                    String photoUrl = itemDoc.getString("photoUrl");
                    String category = itemDoc.getString("category");
                    ItemArtData data = new ItemArtData(photoUrl, category);
                    itemArtCache.put(itemId, data);
                    applyArt(holder.ivItemPhoto, data, fallback);
                });
    }

    private void applyArt(ImageView imageView, ItemArtData data, int fallback) {
        if (data.photoUrl != null && !data.photoUrl.trim().isEmpty()) {
            Glide.with(imageView.getContext())
                    .load(data.photoUrl)
                    .placeholder(fallback)
                    .centerCrop()
                    .into(imageView);
            return;
        }
        int drawable = CategoryDrawableHelper.drawableResForCategory(data.category, fallback);
        imageView.setImageResource(drawable);
    }

    private static class ItemArtData {
        final String photoUrl;
        final String category;

        ItemArtData(String photoUrl, String category) {
            this.photoUrl = photoUrl;
            this.category = category;
        }
    }
}
