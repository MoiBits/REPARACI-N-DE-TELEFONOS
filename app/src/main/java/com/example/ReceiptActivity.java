package com.example;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.google.android.material.button.MaterialButton;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReceiptActivity extends AppCompatActivity {

    private TextView tvShopName, tvShopDetails, tvOrderId, tvDate, tvCustomer, tvPhone, tvModel, tvImei, tvIssue, tvTotal, tvDownPayment, tvBalance, tvTerms;
    private View layoutReceipt;
    private MaterialButton btnDeliverFromReceipt;
    private AppDatabase db;
    private RepairOrder currentOrder;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt);

        db = AppDatabase.getInstance(this);
        initViews();
        
        int orderId = getIntent().getIntExtra("order_id", -1);
        if (orderId != -1) {
            loadOrder(orderId);
        }
        
        setupActions();
    }

    private void initViews() {
        tvShopName = findViewById(R.id.tvReceiptShopName);
        tvShopDetails = findViewById(R.id.tvReceiptShopDetails);
        tvOrderId = findViewById(R.id.tvReceiptOrderId);
        tvDate = findViewById(R.id.tvReceiptDate);
        tvCustomer = findViewById(R.id.tvReceiptCustomer);
        tvPhone = findViewById(R.id.tvReceiptPhone);
        tvModel = findViewById(R.id.tvReceiptModel);
        tvImei = findViewById(R.id.tvReceiptImei);
        tvIssue = findViewById(R.id.tvReceiptIssue);
        tvTotal = findViewById(R.id.tvReceiptTotal);
        tvDownPayment = findViewById(R.id.tvReceiptDownPayment);
        tvBalance = findViewById(R.id.tvReceiptBalance);
        tvTerms = findViewById(R.id.tvReceiptTerms);
        layoutReceipt = findViewById(R.id.layoutReceipt);
        btnDeliverFromReceipt = findViewById(R.id.btnDeliverFromReceipt);

        tvShopName.setText(PreferenceManager.getShopName(this));
        
        StringBuilder details = new StringBuilder(PreferenceManager.getShopLocation(this));
        String shopPhone = PreferenceManager.getShopPhone(this);
        if (!shopPhone.isEmpty()) {
            details.append(" | Cel: ").append(shopPhone);
        }
        
        String fb = PreferenceManager.getFacebook(this);
        String ig = PreferenceManager.getInstagram(this);
        String tk = PreferenceManager.getTiktok(this);
        
        if (!fb.isEmpty() || !ig.isEmpty() || !tk.isEmpty()) {
            details.append("\n");
            if (!fb.isEmpty()) details.append("FB: ").append(fb).append(" ");
            if (!ig.isEmpty()) details.append("IG: ").append(ig).append(" ");
            if (!tk.isEmpty()) details.append("TK: ").append(tk);
        }

        tvShopDetails.setText(details.toString().trim());
        tvTerms.setText(PreferenceManager.getTerms(this));
    }

    private void loadOrder(int id) {
        executor.execute(() -> {
            currentOrder = db.repairOrderDao().getOrderById(id);
            if (currentOrder != null) {
                runOnUiThread(this::populateOrderData);
            }
        });
    }

    private void populateOrderData() {
        tvOrderId.setText("ORDEN #" + String.format(Locale.getDefault(), "%03d", currentOrder.id));
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        tvDate.setText("Fecha: " + sdf.format(new Date(currentOrder.entryDate)));
        
        tvCustomer.setText("Nombre: " + currentOrder.customerName);
        tvPhone.setText("Teléfono: " + currentOrder.customerPhone);
        
        String device = (currentOrder.deviceBrand != null ? currentOrder.deviceBrand : "") + " " + 
                       (currentOrder.deviceModel != null ? currentOrder.deviceModel : "");
        tvModel.setText("Modelo: " + device.trim());
        
        tvImei.setText("IMEI: " + (currentOrder.imei != null && !currentOrder.imei.isEmpty() ? currentOrder.imei : "No registrado"));
        tvIssue.setText("Falla/Servicios:\n" + currentOrder.issue);
        
        String currency = PreferenceManager.getCurrencySymbol(this);
        tvTotal.setText(currency + " " + String.format(Locale.getDefault(), "%.2f", currentOrder.estimatedPrice));
        tvDownPayment.setText(currency + " " + String.format(Locale.getDefault(), "%.2f", currentOrder.downPayment));
        
        double balance = currentOrder.estimatedPrice - currentOrder.downPayment;
        if ("Cancelado".equals(currentOrder.paymentStatus) || balance <= 0) {
            tvBalance.setText("¡PAGADO!");
            tvBalance.setTextColor(Color.parseColor("#10b981"));
            btnDeliverFromReceipt.setVisibility(View.GONE);
        } else {
            tvBalance.setText(currency + " " + String.format(Locale.getDefault(), "%.2f", balance));
            tvBalance.setTextColor(Color.parseColor("#ef4444"));
            
            if (!"Entregado".equals(currentOrder.status)) {
                btnDeliverFromReceipt.setVisibility(View.VISIBLE);
                btnDeliverFromReceipt.setText("Cobrar " + currency + " " + String.format(Locale.getDefault(), "%.2f", balance) + " y Entregar");
            } else {
                btnDeliverFromReceipt.setVisibility(View.GONE);
            }
        }
    }

    private void setupActions() {
        btnDeliverFromReceipt.setOnClickListener(v -> {
            if (currentOrder != null) {
                currentOrder.downPayment = currentOrder.estimatedPrice;
                currentOrder.paymentStatus = "Cancelado";
                currentOrder.status = "Entregado";
                
                executor.execute(() -> {
                    db.repairOrderDao().update(currentOrder);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Equipo cobrado y entregado", Toast.LENGTH_SHORT).show();
                        populateOrderData();
                    });
                });
            }
        });

        findViewById(R.id.btnSaveImage).setOnClickListener(v -> saveReceiptAsImage(true));
        
        findViewById(R.id.btnShareWhatsapp).setOnClickListener(v -> shareViaWhatsapp());

        findViewById(R.id.btnPrintBluetooth).setOnClickListener(v -> printViaBluetooth());
    }

    private void printViaBluetooth() {
        if (currentOrder == null) return;
        Toast.makeText(this, "Conectando con impresora...", Toast.LENGTH_SHORT).show();
        
        executor.execute(() -> {
            BluetoothPrinterHelper printer = new BluetoothPrinterHelper(this);
            boolean success = printer.connectAndPrint(currentOrder);
            
            new Handler(Looper.getMainLooper()).post(() -> {
                if (success) {
                    Toast.makeText(this, "Ticket impreso correctamente", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Error al conectar con la impresora Bluetooth", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private Bitmap createBitmapFromView(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    private Uri saveReceiptAsImage(boolean showToast) {
        Bitmap bitmap = createBitmapFromView(layoutReceipt);
        try {
            File cachePath = new File(getCacheDir(), "images");
            if (!cachePath.exists()) cachePath.mkdirs();
            
            File file = new File(cachePath, "recibo_" + currentOrder.id + ".png");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();
            
            if (showToast) {
                Toast.makeText(this, "Recibo guardado temporalmente", Toast.LENGTH_SHORT).show();
            }
            return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        } catch (IOException e) {
            e.printStackTrace();
            if (showToast) {
                Toast.makeText(this, "Error al guardar imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            return null;
        }
    }

    private void shareViaWhatsapp() {
        if (currentOrder == null) return;

        Uri imageUri = saveReceiptAsImage(false);
        String message = "¡Hola " + currentOrder.customerName + "! Adjuntamos el comprobante de recepción de su equipo (" + 
                currentOrder.deviceBrand + " " + currentOrder.deviceModel + ") en " + 
                PreferenceManager.getShopName(this) + ". ¡Muchas gracias por su confianza!";

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/png");
        intent.putExtra(Intent.EXTRA_TEXT, message);
        intent.putExtra(Intent.EXTRA_STREAM, imageUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        String cleanPhone = currentOrder.customerPhone.replaceAll("[^0-9]", "");
        intent.putExtra("jid", cleanPhone + "@s.whatsapp.net");
        intent.setPackage("com.whatsapp");

        try {
            startActivity(intent);
        } catch (Exception e) {
            intent.setPackage(null);
            try {
                startActivity(Intent.createChooser(intent, "Compartir Recibo"));
            } catch (Exception ex) {
                Toast.makeText(this, "No se encontró ninguna aplicación para compartir", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
