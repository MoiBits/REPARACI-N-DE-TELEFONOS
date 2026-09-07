package com.example;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SaleReceiptActivity extends AppCompatActivity {

    private TextView tvShopName, tvShopDetails, tvSaleId, tvDate, tvCustomer, tvPhone, tvModel, tvImei, tvCondition, tvTotal, tvWarranty, tvNotes;
    private View layoutReceipt;
    private AppDatabase db;
    private PhoneSale currentSale;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sale_receipt);

        db = AppDatabase.getInstance(this);
        initViews();

        int saleId = getIntent().getIntExtra("sale_id", -1);
        if (saleId != -1) {
            loadSale(saleId);
        }

        setupActions();
    }

    private void initViews() {
        tvShopName = findViewById(R.id.tvReceiptShopName);
        tvShopDetails = findViewById(R.id.tvReceiptShopDetails);
        tvSaleId = findViewById(R.id.tvReceiptSaleId);
        tvDate = findViewById(R.id.tvReceiptDate);
        tvCustomer = findViewById(R.id.tvReceiptCustomer);
        tvPhone = findViewById(R.id.tvReceiptPhone);
        tvModel = findViewById(R.id.tvReceiptModel);
        tvImei = findViewById(R.id.tvReceiptImei);
        tvCondition = findViewById(R.id.tvReceiptCondition);
        tvTotal = findViewById(R.id.tvReceiptTotal);
        tvWarranty = findViewById(R.id.tvReceiptWarranty);
        tvNotes = findViewById(R.id.tvReceiptNotes);
        layoutReceipt = findViewById(R.id.layoutReceipt);

        tvShopName.setText(PreferenceManager.getShopName(this));

        StringBuilder details = new StringBuilder(PreferenceManager.getShopLocation(this));
        String shopPhone = PreferenceManager.getShopPhone(this);
        if (!shopPhone.isEmpty()) {
            details.append(" | Cel: ").append(shopPhone);
        }
        tvShopDetails.setText(details.toString());
    }

    private void loadSale(int id) {
        executor.execute(() -> {
            currentSale = db.phoneSaleDao().getSaleById(id);
            if (currentSale != null) {
                runOnUiThread(this::populateSaleData);
            }
        });
    }

    private void populateSaleData() {
        tvSaleId.setText("VENTA #" + String.format(Locale.getDefault(), "%03d", currentSale.id));

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        tvDate.setText("Fecha: " + sdf.format(new Date(currentSale.saleDate)));

        tvCustomer.setText("Cliente: " + currentSale.customerName);
        tvPhone.setText("Teléfono: " + currentSale.customerPhone);

        String device = (currentSale.deviceBrand != null ? currentSale.deviceBrand : "") + " " + 
                       (currentSale.deviceModel != null ? currentSale.deviceModel : "");
        if (currentSale.deviceColor != null && !currentSale.deviceColor.isEmpty()) {
            device += " (" + currentSale.deviceColor + ")";
        }
        tvModel.setText("Equipo: " + device.trim());

        tvImei.setText("IMEI: " + currentSale.imei);
        tvCondition.setText("Condición: " + currentSale.condition);

        String currency = PreferenceManager.getCurrencySymbol(this);
        tvTotal.setText(currency + " " + String.format(Locale.getDefault(), "%.2f", currentSale.price));

        tvWarranty.setText("Este equipo cuenta con " + currentSale.warrantyDays + " días de garantía contra defectos técnicos. No cubre golpes ni humedad.");

        if (currentSale.notes != null && !currentSale.notes.isEmpty()) {
            tvNotes.setVisibility(View.VISIBLE);
            tvNotes.setText("Notas: " + currentSale.notes);
        } else {
            tvNotes.setVisibility(View.GONE);
        }
    }

    private void setupActions() {
        findViewById(R.id.btnSaveImage).setOnClickListener(v -> saveReceiptAsImage(true));
        findViewById(R.id.btnShareWhatsapp).setOnClickListener(v -> shareViaWhatsapp());
        findViewById(R.id.btnPrintBluetooth).setOnClickListener(v -> printViaBluetooth());
        findViewById(R.id.btnFinish).setOnClickListener(v -> finish());
    }

    private void printViaBluetooth() {
        if (currentSale == null) return;
        Toast.makeText(this, "Conectando con impresora...", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            BluetoothPrinterHelper printer = new BluetoothPrinterHelper(this);
            boolean success = printer.connectAndPrintSale(currentSale);

            new Handler(Looper.getMainLooper()).post(() -> {
                if (success) {
                    Toast.makeText(this, "Nota de venta impresa correctamente", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Error al conectar con la impresora", Toast.LENGTH_LONG).show();
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

            File file = new File(cachePath, "venta_" + currentSale.id + ".png");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            if (showToast) {
                Toast.makeText(this, "Nota guardada temporalmente", Toast.LENGTH_SHORT).show();
            }
            return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        } catch (IOException e) {
            e.printStackTrace();
            if (showToast) {
                Toast.makeText(this, "Error al guardar imagen", Toast.LENGTH_SHORT).show();
            }
            return null;
        }
    }

    private void shareViaWhatsapp() {
        if (currentSale == null) return;

        Uri imageUri = saveReceiptAsImage(false);
        String message = "¡Hola " + currentSale.customerName + "! Adjuntamos la nota de venta de su equipo (" + 
                currentSale.deviceBrand + " " + currentSale.deviceModel + "). ¡Gracias por su compra en " + 
                PreferenceManager.getShopName(this) + "!";

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/png");
        intent.putExtra(Intent.EXTRA_TEXT, message);
        intent.putExtra(Intent.EXTRA_STREAM, imageUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        String cleanPhone = currentSale.customerPhone.replaceAll("[^0-9]", "");
        intent.putExtra("jid", cleanPhone + "@s.whatsapp.net");
        intent.setPackage("com.whatsapp");

        try {
            startActivity(intent);
        } catch (Exception e) {
            intent.setPackage(null);
            try {
                startActivity(Intent.createChooser(intent, "Compartir Nota de Venta"));
            } catch (Exception ex) {
                Toast.makeText(this, "No se encontró app para compartir", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
