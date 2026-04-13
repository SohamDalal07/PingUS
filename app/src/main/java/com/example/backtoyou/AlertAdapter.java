package com.example.backtoyou;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.AlertViewHolder> {

    private final List<DocumentSnapshot> claimsList;
    private final OnClaimActionListener listener;
    private final String currentUserId;

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

        String title       = doc.getString("itemTitle");
        String claimerName = doc.getString("claimerName");
        String color       = doc.getString("colorAns");
        String brand       = doc.getString("brandAns");
        String mark        = doc.getString("markAns");
        String photoUrl    = doc.getString("photoUrl");

        // Item photo + "Is this the item you found?" banner
        if (!TextUtils.isEmpty(photoUrl)) {
            holder.ivAlertItemPhoto.setVisibility(View.VISIBLE);
            holder.tvAlertPhotoQuestion.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(photoUrl)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.ivAlertItemPhoto);
        } else {
            holder.ivAlertItemPhoto.setVisibility(View.GONE);
            holder.tvAlertPhotoQuestion.setVisibility(View.GONE);
        }

        holder.tvItemTitle.setText(title != null ? title : "Unknown Item");
        holder.tvClaimerName.setText("Claimed by: " + (claimerName != null ? claimerName : "Unknown"));
        holder.tvColor.setText("Color: " + (color != null ? color : "-"));
        holder.tvBrand.setText("Brand: " + (brand != null ? brand : "-"));
        holder.tvMark.setText("Mark: "  + (mark  != null ? mark  : "-"));

        String claimerUid = doc.getString("claimerUid");
        String status     = doc.getString("status");
        boolean isClaimer = currentUserId != null && currentUserId.equals(claimerUid);

        if ("APPROVED".equals(status)) {
            holder.btnApprove.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);
            if (isClaimer) {
                holder.tvClaimerName.setText("Status: APPROVED by the poster!");
            } else {
                holder.tvClaimerName.setText("Status: You APPROVED this claim! (Item Claimed)");
            }
            holder.tvClaimerName.setTextColor(
                    holder.itemView.getContext().getColor(R.color.colorFoundBg));
        } else if ("REJECTED".equals(status)) {
            holder.btnApprove.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);
            holder.tvClaimerName.setText("Status: You REJECTED this claim.");
            holder.tvClaimerName.setTextColor(
                    holder.itemView.getContext().getColor(R.color.colorLostBg));
        } else {
            // PENDING
            if (isClaimer) {
                holder.tvClaimerName.setText("Status: PENDING poster approval.");
                holder.tvClaimerName.setTextColor(
                        holder.itemView.getContext().getColor(R.color.colorTextSecondary));
                holder.btnApprove.setVisibility(View.GONE);
                holder.btnReject.setVisibility(View.GONE);
            } else {
                holder.tvClaimerName.setText("Claimed by: " + (claimerName != null ? claimerName : "Unknown"));
                holder.tvClaimerName.setTextColor(
                        holder.itemView.getContext().getColor(R.color.colorTextSecondary));
                holder.btnApprove.setVisibility(View.VISIBLE);
                holder.btnReject.setVisibility(View.VISIBLE);
            }
        }

        holder.btnApprove.setOnClickListener(v -> {
            if (listener != null) listener.onApprove(doc);
        });

        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) listener.onReject(doc);
        });
    }

    @Override
    public int getItemCount() {
        return claimsList.size();
    }

    static class AlertViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAlertItemPhoto;
        TextView  tvAlertPhotoQuestion;
        TextView  tvItemTitle, tvClaimerName, tvColor, tvBrand, tvMark;
        Button    btnApprove, btnReject;

        AlertViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAlertItemPhoto     = itemView.findViewById(R.id.ivAlertItemPhoto);
            tvAlertPhotoQuestion = itemView.findViewById(R.id.tvAlertPhotoQuestion);
            tvItemTitle          = itemView.findViewById(R.id.tvAlertItemTitle);
            tvClaimerName        = itemView.findViewById(R.id.tvAlertClaimerName);
            tvColor              = itemView.findViewById(R.id.tvAlertColor);
            tvBrand              = itemView.findViewById(R.id.tvAlertBrand);
            tvMark               = itemView.findViewById(R.id.tvAlertMark);
            btnApprove           = itemView.findViewById(R.id.btnAlertApprove);
            btnReject            = itemView.findViewById(R.id.btnAlertReject);
        }
    }
}
