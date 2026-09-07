package com.example;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhoneSaleActivity extends AppCompatActivity {

    private EditText etCustomerName, etCustomerPhone, etDeviceModel, etDeviceColor, etImei, etPrice, etWarranty, etNotes;
    private AutoCompleteTextView spBrand;
    private RadioGroup rgCondition;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final String[] brands = {
            "Samsung", "Xiaomi", "Apple (iPhone)", "Honor", "Motorola", "Infinix", 
            "Tecno", "ZTE", "Realme", "Huawei", "Vivo", "Oppo", "TCL", "Dialn", 
            "Nokia", "Itel", "Nothing", "OnePlus", "Google Pixel", "Otro"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_sale);

        db = AppDatabase.getInstance(this);
        initViews();
    }

    private void initViews() {
        etCustomerName = findViewById(R.id.etCustomerName);
        etCustomerPhone = findViewById(R.id.etCustomerPhone);
        spBrand = findViewById(R.id.spBrand);
        etDeviceModel = findViewById(R.id.etDeviceModel);
        etDeviceColor = findViewById(R.id.etDeviceColor);
        etImei = findViewById(R.id.etImei);
        rgCondition = findViewById(R.id.rgCondition);
        etPrice = findViewById(R.id.etPrice);
        etWarranty = findViewById(R.id.etWarranty);
        etNotes = findViewById(R.id.etNotes);

        ArrayAdapter<String> brandAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, brands);
        spBrand.setAdapter(brandAdapter);

        etWarranty.setText(String.valueOf(PreferenceManager.getWarrantyDays(this)));

        findViewById(R.id.btnSaveSale).setOnClickListener(v -> {
            if (validateFields()) {
                if (PreferenceManager.isSignatureRequired(this)) {
                    showSignatureDialog();
                } else {
                    saveSale(null);
                }
            }
        });
    }

    private boolean validateFields() {
        if (etCustomerName.getText().toString().isEmpty() || etCustomerPhone.getText().toString().isEmpty()) {
            Toast.makeText(this, "Nombre y teléfono son obligatorios", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etImei.getText().toString().isEmpty()) {
            Toast.makeText(this, "El IMEI es obligatorio para la garantía", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etPrice.getText().toString().isEmpty()) {
            Toast.makeText(this, "Ingrese el precio de venta", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (rgCondition.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Seleccione la condición del equipo", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void showSignatureDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_signature, null);
        SignatureView signatureView = dialogView.findViewById(R.id.signatureView);
        dialogView.findViewById(R.id.btnClearSignature).setOnClickListener(v -> signatureView.clear());

        new AlertDialog.Builder(this)
                .setTitle("Firma del Comprador")
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("Confirmar Venta", (d, which) -> {
                    Bitmap signatureBitmap = signatureView.getSignatureBitmap();
                    String encodedSig = bitmapToBase64(signatureBitmap);
                    saveSale(encodedSig);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 50, outputStream);
        byte[] byteArray = outputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void saveSale(String signature) {
        PhoneSale sale = new PhoneSale();
        sale.customerName = etCustomerName.getText().toString();
        sale.customerPhone = etCustomerPhone.getText().toString();
        sale.deviceBrand = spBrand.getText().toString();
        sale.deviceModel = etDeviceModel.getText().toString();
        sale.deviceColor = etDeviceColor.getText().toString();
        sale.imei = etImei.getText().toString();
        
        int selectedConditionId = rgCondition.getCheckedRadioButtonId();
        RadioButton rbCondition = findViewById(selectedConditionId);
        sale.condition = rbCondition != null ? rbCondition.getText().toString() : "";
        
        try {
            sale.price = Double.parseDouble(etPrice.getText().toString());
            sale.warrantyDays = Integer.parseInt(etWarranty.getText().toString());
        } catch (Exception e) {
            sale.price = 0;
            sale.warrantyDays = 0;
        }
        
        sale.notes = etNotes.getText().toString();
        sale.customerSignature = signature;
        sale.saleDate = System.currentTimeMillis();

        executor.execute(() -> {
            long id = db.phoneSaleDao().insert(sale);
            runOnUiThread(() -> {
                Toast.makeText(PhoneSaleActivity.this, "Venta registrada con éxito", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(PhoneSaleActivity.this, SaleReceiptActivity.class);
                intent.putExtra("sale_id", (int) id);
                startActivity(intent);
                finish();
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
