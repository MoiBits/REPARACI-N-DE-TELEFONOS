package com.example;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class RepairOrderAdapter extends RecyclerView.Adapter<RepairOrderAdapter.ViewHolder> {
    private List<RepairOrder> orders = new ArrayList<>();

    public void setOrders(List<RepairOrder> orders) {
        this.orders = orders;
        notifyDataSetChanged();
    }

    public List<RepairOrder> getOrders() {
        return orders;
    }

    public void removeItem(int position) {
        orders.remove(position);
        notifyItemRemoved(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_repair_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RepairOrder order = orders.get(position);
        Context context = holder.itemView.getContext();
        
        // Mostrar Marca + Modelo
        String fullDeviceName = (order.deviceBrand != null ? order.deviceBrand : "") + " " + (order.deviceModel != null ? order.deviceModel : "");
        holder.tvDeviceModel.setText(fullDeviceName.trim());
        
        holder.tvCustomerName.setText(order.customerName);
        holder.tvStatus.setText(order.status);
        
        // Aplicar fondos dinámicos según el estado
        applyStatusStyle(holder.tvStatus, order.status);
        
        String currency = PreferenceManager.getCurrencySymbol(context);
        holder.tvPrice.setText(currency + " " + String.format("%.2f", order.estimatedPrice));

        if ("Patrón".equals(order.lockType) && order.patternSequence != null && !order.patternSequence.isEmpty()) {
            holder.pvThumbnail.setVisibility(View.VISIBLE);
            holder.pvThumbnail.setDisplayOnly(true);
            holder.pvThumbnail.setPattern(order.patternSequence);
        } else {
            holder.pvThumbnail.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, OrderDetailActivity.class);
            intent.putExtra("order_id", order.id);
            context.startActivity(intent);
        });
    }

    private void applyStatusStyle(TextView tvStatus, String status) {
        if (status == null) return;
        
        int backgroundRes;
        int colorRes;
        
        switch (status) {
            case "En Taller":
                backgroundRes = R.drawable.bg_status_in_shop;
                colorRes = R.color.status_text_in_repair;
                break;
            case "Listo":
                backgroundRes = R.drawable.bg_status_ready;
                colorRes = R.color.status_text_ready;
                break;
            case "Entregado":
                backgroundRes = R.drawable.bg_status_delivered;
                colorRes = R.color.status_text_delivered;
                break;
            case "Pendiente":
            default:
                backgroundRes = R.drawable.bg_status_pending;
                colorRes = R.color.status_text_pending;
                break;
        }
        
        tvStatus.setBackgroundResource(backgroundRes);
        tvStatus.setTextColor(androidx.core.content.ContextCompat.getColor(tvStatus.getContext(), colorRes));
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeviceModel, tvCustomerName, tvStatus, tvPrice;
        PatternView pvThumbnail;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceModel = itemView.findViewById(R.id.tvDeviceModel);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            pvThumbnail = itemView.findViewById(R.id.pvThumbnail);
        }
    }
}
