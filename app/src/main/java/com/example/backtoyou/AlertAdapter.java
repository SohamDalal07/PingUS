package com.example.backtoyou;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

/**
 * Read-only history rows for past claims (no approve/reject actions here).
 */
public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.AlertViewHolder> {

    private final List<DocumentSnapshot> claimsList;

    public AlertAdapter(List<DocumentSnapshot> claimsList) {
        this.claimsList = claimsList;
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
        String status = doc.getString("status");
        if (status == null) status = "UNKNOWN";

        holder.tvItemTitle.setText(title != null ? title : "Unknown Item");
        holder.tvRowNumber.setText(String.valueOf(position + 1));
        holder.tvHistoryLine.setText("History · " + status + " · " + formatTime(doc.getLong("timestamp")));
        holder.tvClaimerName.setText("Claimer: " + (claimerName != null ? claimerName : "Unknown"));
        holder.tvColor.setText("Color: " + (color != null ? color : "—"));
        holder.tvBrand.setText("Brand: " + (brand != null ? brand : "—"));
        holder.tvMark.setText("Mark: " + (mark != null ? mark : "—"));
    }

    private String formatTime(Long timestamp) {
        if (timestamp == null || timestamp <= 0) return "—";
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
    public int getItemCount() {
        return claimsList.size();
    }

    static class AlertViewHolder extends RecyclerView.ViewHolder {
        TextView tvRowNumber, tvItemTitle, tvHistoryLine, tvClaimerName, tvColor, tvBrand, tvMark;

        AlertViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRowNumber = itemView.findViewById(R.id.tvAlertNumber);
            tvItemTitle = itemView.findViewById(R.id.tvAlertItemTitle);
            tvHistoryLine = itemView.findViewById(R.id.tvAlertHistoryLine);
            tvClaimerName = itemView.findViewById(R.id.tvAlertClaimerName);
            tvColor = itemView.findViewById(R.id.tvAlertColor);
            tvBrand = itemView.findViewById(R.id.tvAlertBrand);
            tvMark = itemView.findViewById(R.id.tvAlertMark);
        }
    }
}
