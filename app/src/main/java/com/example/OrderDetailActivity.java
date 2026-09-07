package com.example;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrderDetailActivity extends AppCompatActivity {

    private TextView tvStatus, tvPaymentStatus, tvModel, tvCustomer, tvImei, tvIssue, tvPrice, tvLockType, tvPin;
    private PatternView pvPattern;
    private MaterialButton btnMarkAsPaid, btnStatusInShop, btnStatusReady, btnStatusDelivered;
    private AppDatabase db;
    private RepairOrder currentOrder;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        db = AppDatabase.getInstance(this);
        initViews();
        
        int orderId = getIntent().getIntExtra("order_id", -1);
        if (orderId != -1) {
            loadOrder(orderId);
        }

        setupButtons();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvStatus = findViewById(R.id.tvDetailStatus);
        tvPaymentStatus = findViewById(R.id.tvDetailPaymentStatus);
        tvModel = findViewById(R.id.tvDetailModel);
        tvCustomer = findViewById(R.id.tvDetailCustomer);
        tvImei = findViewById(R.id.tvDetailImei);
        tvIssue = findViewById(R.id.tvDetailIssue);
        tvPrice = findViewById(R.id.tvDetailPrice);
        tvLockType = findViewById(R.id.tvDetailLockType);
        tvPin = findViewById(R.id.tvDetailPin);
        pvPattern = findViewById(R.id.pvDetailPattern);
        btnMarkAsPaid = findViewById(R.id.btnMarkAsPaid);
        btnStatusInShop = findViewById(R.id.btnStatusInShop);
        btnStatusReady = findViewById(R.id.btnStatusReady);
        btnStatusDelivered = findViewById(R.id.btnStatusDelivered);
    }

    private void loadOrder(int id) {
        executor.execute(() -> {
            currentOrder = db.repairOrderDao().getOrderById(id);
            if (currentOrder != null) {
                runOnUiThread(() -> {
                    updateUI(currentOrder);
                });
            }
        });
    }

    private void updateUI(RepairOrder order) {
        tvStatus.setText(order.status);
        tvPaymentStatus.setText(order.paymentStatus);
        
        updateStatusBadgeColor(order.status);

        if ("Cancelado".equals(order.paymentStatus)) {
            int paidColor = androidx.core.content.ContextCompat.getColor(this, R.color.status_delivered);
            tvPaymentStatus.setBackgroundTintList(ColorStateList.valueOf(paidColor));
            btnMarkAsPaid.setVisibility(View.GONE);
            btnStatusDelivered.setText("Entregado");
        } else {
            int pendingColor = androidx.core.content.ContextCompat.getColor(this, R.color.status_pending);
            tvPaymentStatus.setBackgroundTintList(ColorStateList.valueOf(pendingColor));
            btnMarkAsPaid.setVisibility(View.VISIBLE);
            btnStatusDelivered.setText("Cobrar y Entregar");
        }

        btnStatusInShop.setEnabled(true);
        btnStatusInShop.setAlpha(1.0f);
        btnStatusReady.setEnabled(true);
        btnStatusReady.setAlpha(1.0f);
        btnStatusDelivered.setEnabled(true);
        btnStatusDelivered.setAlpha(1.0f);

        if ("En Taller".equals(order.status)) {
            btnStatusInShop.setEnabled(false);
            btnStatusInShop.setAlpha(0.5f);
        } else if ("Listo".equals(order.status)) {
            btnStatusInShop.setEnabled(false);
            btnStatusInShop.setAlpha(0.5f);
            btnStatusReady.setEnabled(false);
            btnStatusReady.setAlpha(0.5f);
        } else if ("Entregado".equals(order.status)) {
            btnStatusInShop.setEnabled(false);
            btnStatusInShop.setAlpha(0.5f);
            btnStatusReady.setEnabled(false);
            btnStatusReady.setAlpha(0.5f);
            btnStatusDelivered.setEnabled(false);
            btnStatusDelivered.setAlpha(0.5f);
            btnStatusDelivered.setText("Entregado");
        }

        tvModel.setText(order.deviceModel);
        tvCustomer.setText("Cliente: " + order.customerName);
        tvImei.setText("IMEI: " + order.imei);
        tvIssue.setText("Falla: " + order.issue);
        
        String currency = PreferenceManager.getCurrencySymbol(this);
        tvPrice.setText("Precio: " + currency + " " + String.format("%.2f", order.estimatedPrice));

        tvLockType.setText("Bloqueo: " + order.lockType);

        if ("PIN".equals(order.lockType)) {
            tvPin.setVisibility(View.VISIBLE);
            tvPin.setText("PIN: " + order.lockCode);
            pvPattern.setVisibility(View.GONE);
        } else if ("Patrón".equals(order.lockType)) {
            tvPin.setVisibility(View.GONE);
            pvPattern.setVisibility(View.VISIBLE);
            pvPattern.setDisplayOnly(true);
            pvPattern.setPattern(order.patternSequence);
        } else {
            tvPin.setVisibility(View.GONE);
            pvPattern.setVisibility(View.GONE);
        }
    }

    private void updateStatusBadgeColor(String status) {
        int colorRes;
        switch (status) {
            case "En Taller":
                colorRes = R.color.status_in_repair;
                break;
            case "Listo":
                colorRes = R.color.status_ready;
                break;
            case "Entregado":
                colorRes = R.color.status_delivered;
                break;
            default:
                colorRes = R.color.status_pending;
                break;
        }
        int color = androidx.core.content.ContextCompat.getColor(this, colorRes);
        tvStatus.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    private void setupButtons() {
        btnStatusInShop.setOnClickListener(v -> updateStatus("En Taller"));
        btnStatusReady.setOnClickListener(v -> updateStatus("Listo"));
        
        btnStatusDelivered.setOnClickListener(v -> {
            if (currentOrder == null) return;
            if (!"Cancelado".equals(currentOrder.paymentStatus)) {
                showPaymentRequiredDialog();
            } else {
                updateStatus("Entregado");
            }
        });
        
        btnMarkAsPaid.setOnClickListener(v -> updatePaymentStatus("Cancelado"));

        findViewById(R.id.btnViewReceipt).setOnClickListener(v -> {
            if (currentOrder != null) {
                Intent intent = new Intent(this, ReceiptActivity.class);
                intent.putExtra("order_id", currentOrder.id);
                startActivity(intent);
            }
        });
    }

    private void showPaymentRequiredDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Pago Pendiente")
                .setMessage("No se puede entregar el equipo sin haber registrado el pago. ¿Desea cobrar y entregar ahora?")
                .setPositiveButton("Cobrar y Entregar", (dialog, which) -> {
                    currentOrder.paymentStatus = "Cancelado";
                    updateStatus("Entregado");
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void updateStatus(String status) {
        if (currentOrder == null) return;
        currentOrder.status = status;
        executor.execute(() -> {
            db.repairOrderDao().update(currentOrder);
            runOnUiThread(() -> {
                updateUI(currentOrder);
                Toast.makeText(this, "Estado actualizado a: " + status, Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void updatePaymentStatus(String status) {
        if (currentOrder == null) return;
        currentOrder.paymentStatus = status;
        executor.execute(() -> {
            db.repairOrderDao().update(currentOrder);
            runOnUiThread(() -> {
                updateUI(currentOrder);
                Toast.makeText(this, "Pago registrado: " + status, Toast.LENGTH_SHORT).show();
            });
        });
    }
}
