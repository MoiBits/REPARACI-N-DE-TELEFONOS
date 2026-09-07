package com.example;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.FileProvider;

import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private TextView tvReceived, tvInRepair, tvReady, tvDelivered, tvTotalRepairs, tvAdminName;
    private TextView tvTotalEarned, tvTotalPending, tvInProcessShort, tvCompletedShort;
    private MaterialCardView summaryCard;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = AppDatabase.getInstance(this);
        initViews();
        loadStats();
    }

    private void initViews() {
        tvTotalRepairs = findViewById(R.id.tvTotalRepairs);
        tvReceived = findViewById(R.id.tvReceivedCount);
        tvInRepair = findViewById(R.id.tvInRepairCount);
        tvReady = findViewById(R.id.tvReadyCount);
        tvDelivered = findViewById(R.id.tvDeliveredCount);
        tvAdminName = findViewById(R.id.tvAdminName);
        summaryCard = findViewById(R.id.summaryCard);
        
        tvTotalEarned = findViewById(R.id.tvTotalEarned);
        tvTotalPending = findViewById(R.id.tvTotalPending);
        tvInProcessShort = findViewById(R.id.tvInProcessShort);
        tvCompletedShort = findViewById(R.id.tvCompletedShort);

        if (tvAdminName != null) {
            tvAdminName.setText(PreferenceManager.getShopName(this));
        }

        View btnAdd = findViewById(R.id.fabNewRepair);
        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, IntakeActivity.class);
                startActivity(intent);
            });
        }

        ImageButton btnMenu = findViewById(R.id.btnMenuPopup);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(this::showOptionsMenu);
        }

        setupClickListeners();
    }

    private void loadStats() {
        executor.execute(() -> {
            List<RepairOrder> orders = db.repairOrderDao().getAllOrders();
            int total = orders.size();
            int received = 0, inRepair = 0, ready = 0, delivered = 0;
            double earned = 0, pending = 0;

            for (RepairOrder order : orders) {
                if (order.status != null) {
                    switch (order.status) {
                        case "Pendiente":
                        case "Recibido":
                            received++;
                            break;
                        case "En Reparación":
                        case "En Taller":
                            inRepair++;
                            break;
                        case "Listo":
                            ready++;
                            break;
                        case "Entregado":
                            delivered++;
                            break;
                    }
                }
                earned += order.downPayment;
                pending += (order.estimatedPrice - order.downPayment);
            }

            final int fTotal = total, fReceived = received, fInRepair = inRepair, fReady = ready, fDelivered = delivered;
            final double fEarned = earned, fPending = pending;
            
            // Obtener la moneda configurada
            final String currency = PreferenceManager.getCurrencySymbol(this);
            
            runOnUiThread(() -> {
                if (tvTotalRepairs != null) tvTotalRepairs.setText(String.valueOf(fTotal));
                if (tvReceived != null) tvReceived.setText(String.valueOf(fReceived));
                if (tvInRepair != null) tvInRepair.setText(String.valueOf(fInRepair));
                if (tvReady != null) tvReady.setText(String.valueOf(fReady));
                if (tvDelivered != null) tvDelivered.setText(String.valueOf(fDelivered));
                
                if (tvTotalEarned != null) tvTotalEarned.setText(String.format(Locale.getDefault(), "%s %.2f", currency, fEarned));
                if (tvTotalPending != null) tvTotalPending.setText(String.format(Locale.getDefault(), "%s %.2f", currency, fPending));
                
                if (tvInProcessShort != null) tvInProcessShort.setText(String.valueOf(fReceived + fInRepair));
                if (tvCompletedShort != null) tvCompletedShort.setText(String.valueOf(fReady + fDelivered));
            });
        });
    }

    private void setupClickListeners() {
        View.OnClickListener statusClickListener = v -> {
            int tabIndex = 0;
            int id = v.getId();
            if (id == R.id.cardReceived || id == R.id.tvReceivedCount) tabIndex = 1;
            else if (id == R.id.cardInRepair || id == R.id.tvInRepairCount) tabIndex = 2;
            else if (id == R.id.cardReady || id == R.id.tvReadyCount) tabIndex = 3;
            else if (id == R.id.cardDelivered || id == R.id.tvDeliveredCount) tabIndex = 4;

            Intent intent = new Intent(MainActivity.this, RepairListActivity.class);
            intent.putExtra("selected_tab", tabIndex);
            startActivity(intent);
        };

        findViewById(R.id.cardReceived).setOnClickListener(statusClickListener);
        findViewById(R.id.cardInRepair).setOnClickListener(statusClickListener);
        findViewById(R.id.cardReady).setOnClickListener(statusClickListener);
        findViewById(R.id.cardDelivered).setOnClickListener(statusClickListener);

        if (summaryCard != null) {
            summaryCard.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, RepairListActivity.class);
                intent.putExtra("selected_tab", 0);
                startActivity(intent);
            });
        }
    }

    private void showOptionsMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenu().add(0, 1, 1, "Configuración");
        popup.getMenu().add(0, 2, 2, "Compartir Mi tarjeta Personal");
        popup.getMenu().add(0, 3, 3, "Venta de Teléfonos Móviles");
        popup.getMenu().add(0, 4, 4, "Historial de Ventas");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    openSettings();
                    return true;
                case 2:
                    shareBusinessCardImage();
                    return true;
                case 3:
                    startActivity(new Intent(MainActivity.this, PhoneSaleActivity.class));
                    return true;
                case 4:
                    startActivity(new Intent(MainActivity.this, SalesHistoryActivity.class));
                    return true;
                default:
                    return false;
            }
        });
        popup.show();
    }

    private void openSettings() {
        Intent intent = new Intent(this, SetupActivity.class);
        intent.putExtra("isEditMode", true);
        startActivity(intent);
    }

    private void shareBusinessCardImage() {
        try {
            View cardView = getLayoutInflater().inflate(R.layout.business_card_layout, null);
            
            TextView tvName = cardView.findViewById(R.id.tvBusinessName);
            TextView tvAddress = cardView.findViewById(R.id.tvBusinessAddress);
            TextView tvPhone = cardView.findViewById(R.id.tvBusinessPhone);

            if (tvName != null) tvName.setText(PreferenceManager.getShopName(this));
            if (tvAddress != null) tvAddress.setText(PreferenceManager.getShopLocation(this));
            if (tvPhone != null) tvPhone.setText(PreferenceManager.getShopPhone(this));

            cardView.measure(View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            cardView.layout(0, 0, cardView.getMeasuredWidth(), cardView.getMeasuredHeight());

            Bitmap bitmap = Bitmap.createBitmap(cardView.getMeasuredWidth(), cardView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);
            cardView.draw(canvas);

            File cachePath = new File(getCacheDir(), "images");
            if (!cachePath.exists()) {
                cachePath.mkdirs();
            }
            File file = new File(cachePath, "tarjeta_personal.png");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            String authority = getPackageName() + ".fileprovider";
            Uri contentUri = FileProvider.getUriForFile(this, authority, file);

            if (contentUri != null) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.setType("image/png");
                startActivity(Intent.createChooser(shareIntent, "Compartir Tarjeta Personal"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al compartir: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tvAdminName != null) {
            tvAdminName.setText(PreferenceManager.getShopName(this));
        }
        loadStats();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
