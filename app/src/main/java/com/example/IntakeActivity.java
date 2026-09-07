package com.example;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputLayout;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IntakeActivity extends AppCompatActivity {

    private EditText etCustomerName, etCustomerPhone, etDeviceModel, etDeviceColor, etImei, etCondition, etDownPayment, etPin, etOtherService, etServicePrice;
    private CheckBox cbSim, cbSd, cbCover;
    private RadioGroup rgLockType, rgPaymentOption;
    private AutoCompleteTextView spBrand, spCommonServices;
    private TextInputLayout tilPin, tilDownPayment, tilCustomerPhone, tilOtherService;
    private View llPatternPreview, llBalanceContainer;
    private PatternView pvPreview;
    private LinearLayout llServicesList;
    private TextView tvTotalAmount, tvBalanceAmount;
    
    private String currentPattern = "";
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private List<ServiceItem> selectedServices = new ArrayList<>();
    private double totalAmount = 0.0;

    private final String[] brands = {
            "Samsung", "Xiaomi", "Apple (iPhone)", "Honor", "Motorola", "Infinix", 
            "Tecno", "ZTE", "Realme", "Huawei", "Vivo", "Oppo", "TCL", "Dialn", 
            "Nokia", "Itel", "Nothing", "OnePlus", "Google Pixel", "Otro"
    };

    private static class ServiceItem {
        String name;
        double price;
        ServiceItem(String name, double price) {
            this.name = name;
            this.price = price;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intake);

        db = AppDatabase.getInstance(this);
        initViews();
        setupListeners();
        applyRegionalSettings();
    }

    private void initViews() {
        etCustomerName = findViewById(R.id.etCustomerName);
        etCustomerPhone = findViewById(R.id.etCustomerPhone);
        tilCustomerPhone = findViewById(R.id.tilCustomerPhone);
        spBrand = findViewById(R.id.spBrand);
        etDeviceModel = findViewById(R.id.etDeviceModel);
        etDeviceColor = findViewById(R.id.etDeviceColor);
        etImei = findViewById(R.id.etImei);
        etCondition = findViewById(R.id.etCondition);
        cbSim = findViewById(R.id.cbSim);
        cbSd = findViewById(R.id.cbSd);
        cbCover = findViewById(R.id.cbCover);
        
        spCommonServices = findViewById(R.id.spCommonServices);
        etOtherService = findViewById(R.id.etOtherService);
        tilOtherService = findViewById(R.id.tilOtherService);
        etServicePrice = findViewById(R.id.etServicePrice);
        llServicesList = findViewById(R.id.llServicesList);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvBalanceAmount = findViewById(R.id.tvBalanceAmount);
        
        rgPaymentOption = findViewById(R.id.rgPaymentOption);
        etDownPayment = findViewById(R.id.etDownPayment);
        tilDownPayment = findViewById(R.id.tilDownPayment);
        llBalanceContainer = findViewById(R.id.llBalanceContainer);
        
        etPin = findViewById(R.id.etPin);
        rgLockType = findViewById(R.id.rgLockType);
        tilPin = findViewById(R.id.tilPin);
        llPatternPreview = findViewById(R.id.llPatternContainer);
        pvPreview = findViewById(R.id.patternView);
        
        if (pvPreview != null) {
            pvPreview.setDisplayOnly(true);
        }

        ArrayAdapter<String> brandAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, brands);
        spBrand.setAdapter(brandAdapter);

        ArrayAdapter<CharSequence> serviceAdapter = ArrayAdapter.createFromResource(this,
                R.array.common_services, android.R.layout.simple_dropdown_item_1line);
        spCommonServices.setAdapter(serviceAdapter);
    }

    private void applyRegionalSettings() {
        String currency = PreferenceManager.getCurrencySymbol(this);
        String code = PreferenceManager.getCountryCode(this);
        int length = PreferenceManager.getPhoneLength(this);

        tilDownPayment.setHint(getString(R.string.hint_down_payment, currency));
        tilCustomerPhone.setPrefixText(code + " ");
        etCustomerPhone.setFilters(new InputFilter[]{new InputFilter.LengthFilter(length)});
    }

    private void setupListeners() {
        spCommonServices.setOnItemClickListener((parent, view, position, id) -> {
            String selected = parent.getItemAtPosition(position).toString();
            if (selected.contains("Otro")) {
                tilOtherService.setVisibility(View.VISIBLE);
            } else {
                tilOtherService.setVisibility(View.GONE);
            }
        });

        findViewById(R.id.btnAddService).setOnClickListener(v -> addServiceToList());

        rgPaymentOption.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbDownPayment) {
                tilDownPayment.setVisibility(View.VISIBLE);
                llBalanceContainer.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.rbPayFull) {
                tilDownPayment.setVisibility(View.GONE);
                llBalanceContainer.setVisibility(View.GONE);
                etDownPayment.setText(String.valueOf(totalAmount));
            } else { // Pagar al final
                tilDownPayment.setVisibility(View.GONE);
                llBalanceContainer.setVisibility(View.VISIBLE);
                etDownPayment.setText("0");
            }
            updateTotals();
        });

        etDownPayment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateTotals();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        rgLockType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbPin) {
                tilPin.setVisibility(View.VISIBLE);
                llPatternPreview.setVisibility(View.GONE);
            } else if (checkedId == R.id.rbPattern) {
                tilPin.setVisibility(View.GONE);
                llPatternPreview.setVisibility(View.VISIBLE);
                showPatternDialog();
            } else {
                tilPin.setVisibility(View.GONE);
                llPatternPreview.setVisibility(View.GONE);
                currentPattern = "";
            }
        });

        findViewById(R.id.btnSave).setOnClickListener(v -> {
            if (etCustomerName.getText().toString().isEmpty() || etCustomerPhone.getText().toString().isEmpty()) {
                Toast.makeText(this, "Nombre y teléfono son obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedServices.isEmpty()) {
                Toast.makeText(this, "Debe añadir al menos un servicio", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (PreferenceManager.isSignatureRequired(this)) {
                showSignatureDialog();
            } else {
                saveOrder(null);
            }
        });
    }

    private void addServiceToList() {
        String serviceName;
        Object selectedObj = spCommonServices.getText();
        String selected = selectedObj != null ? selectedObj.toString() : "";
        
        if (selected.contains("Otro")) {
            serviceName = etOtherService.getText().toString().trim();
        } else {
            serviceName = selected;
        }

        String priceStr = etServicePrice.getText().toString().trim();

        if (serviceName.isEmpty()) {
            Toast.makeText(this, "Seleccione o ingrese un servicio", Toast.LENGTH_SHORT).show();
            return;
        }

        if (priceStr.isEmpty()) {
            Toast.makeText(this, "Ingrese un precio", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double price = Double.parseDouble(priceStr);
            if (price <= 0) {
                Toast.makeText(this, "El precio debe ser mayor a 0", Toast.LENGTH_SHORT).show();
                return;
            }
            ServiceItem item = new ServiceItem(serviceName, price);
            selectedServices.add(item);

            renderServiceRow(item);
            updateTotals();

            // Reset inputs
            spCommonServices.setText("", false);
            etOtherService.setText("");
            etServicePrice.setText("");
            tilOtherService.setVisibility(View.GONE);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Precio inválido", Toast.LENGTH_SHORT).show();
        }
    }

    private void renderServiceRow(ServiceItem item) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_service_row, null);
        TextView tvName = row.findViewById(R.id.tvServiceName);
        TextView tvPrice = row.findViewById(R.id.tvServicePrice);
        ImageButton btnDelete = row.findViewById(R.id.btnDeleteService);
        
        String currency = PreferenceManager.getCurrencySymbol(this);
        tvName.setText(item.name);
        tvPrice.setText(String.format(Locale.getDefault(), "%s %.2f", currency, item.price));
        
        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Eliminar servicio")
                .setMessage("¿Desea quitar " + item.name + " de la lista?")
                .setPositiveButton("Eliminar", (d, w) -> {
                    selectedServices.remove(item);
                    llServicesList.removeView(row);
                    updateTotals();
                })
                .setNegativeButton("Cancelar", null)
                .show();
        });

        llServicesList.addView(row);
    }

    private void updateTotals() {
        totalAmount = 0;
        for (ServiceItem item : selectedServices) {
            totalAmount += item.price;
        }

        int checkedPaymentId = rgPaymentOption.getCheckedRadioButtonId();
        double downPayment = 0;

        if (checkedPaymentId == R.id.rbPayFull) {
            downPayment = totalAmount;
        } else if (checkedPaymentId == R.id.rbDownPayment) {
            try {
                downPayment = Double.parseDouble(etDownPayment.getText().toString());
            } catch (Exception ignored) {}
        } else {
            downPayment = 0;
        }

        double balance = totalAmount - downPayment;

        tvTotalAmount.setText(String.format(Locale.getDefault(), "%.2f", totalAmount));
        tvBalanceAmount.setText(String.format(Locale.getDefault(), "%.2f", balance));
        
        if (balance > 0) {
            tvBalanceAmount.setTextColor(Color.RED);
        } else {
            tvBalanceAmount.setTextColor(Color.parseColor("#388E3C")); // Material Green
        }
    }

    private void showPatternDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_pattern, null);
        PatternView pvDialog = dialogView.findViewById(R.id.pvDialog);
        
        new AlertDialog.Builder(this)
                .setTitle("Dibujar Patrón")
                .setView(dialogView)
                .setPositiveButton("Listo", (d, which) -> {
                    currentPattern = pvDialog.getPatternString();
                    if (pvPreview != null) {
                        pvPreview.setPattern(currentPattern);
                    }
                })
                .setNegativeButton("Cancelar", (d, which) -> {
                    if (currentPattern.isEmpty()) {
                        rgLockType.check(R.id.rbNone);
                    }
                })
                .show();
    }

    private void showSignatureDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_signature, null);
        SignatureView signatureView = dialogView.findViewById(R.id.signatureView);
        dialogView.findViewById(R.id.btnClearSignature).setOnClickListener(v -> signatureView.clear());

        new AlertDialog.Builder(this)
                .setTitle("Firma del Cliente")
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("Guardar Orden", (d, which) -> {
                    Bitmap signatureBitmap = signatureView.getSignatureBitmap();
                    String encodedSig = bitmapToBase64(signatureBitmap);
                    saveOrder(encodedSig);
                })
                .setNegativeButton("Atrás", null)
                .show();
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 50, outputStream);
        byte[] byteArray = outputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void saveOrder(String signature) {
        RepairOrder order = new RepairOrder();
        order.customerName = etCustomerName.getText().toString();
        
        String code = PreferenceManager.getCountryCode(this);
        order.customerPhone = code + " " + etCustomerPhone.getText().toString();
        
        order.deviceBrand = spBrand.getText().toString();
        order.deviceModel = etDeviceModel.getText().toString();
        order.deviceColor = etDeviceColor.getText().toString();
        order.imei = etImei.getText().toString();
        order.physicalCondition = etCondition.getText().toString();
        
        StringBuilder accs = new StringBuilder();
        if (cbSim.isChecked()) accs.append("SIM, ");
        if (cbSd.isChecked()) accs.append("SD, ");
        if (cbCover.isChecked()) accs.append("Funda");
        order.accessories = accs.toString().replaceAll(", $", "");
        
        StringBuilder issues = new StringBuilder();
        String currency = PreferenceManager.getCurrencySymbol(this);
        for (ServiceItem item : selectedServices) {
            String name = item.name;
            String price = currency + " " + String.format(Locale.getDefault(), "%.2f", item.price);
            
            int dotsCount = Math.max(1, 32 - name.length() - price.length());
            StringBuilder line = new StringBuilder(name);
            for (int i = 0; i < dotsCount; i++) line.append(".");
            line.append(price);
            
            issues.append(line.toString()).append("\n");
        }
        order.issue = issues.toString().trim();
        
        order.estimatedPrice = totalAmount;
        
        int checkedPaymentId = rgPaymentOption.getCheckedRadioButtonId();
        if (checkedPaymentId == R.id.rbPayFull) {
            order.downPayment = totalAmount;
        } else if (checkedPaymentId == R.id.rbDownPayment) {
            try {
                order.downPayment = Double.parseDouble(etDownPayment.getText().toString());
            } catch (Exception e) {
                order.downPayment = 0.0;
            }
        } else {
            order.downPayment = 0.0;
        }
        
        order.status = "Pendiente";
        order.paymentStatus = (order.downPayment >= order.estimatedPrice) ? "Cancelado" : "Pendiente";
        order.customerSignature = signature;
        order.entryDate = System.currentTimeMillis();

        int checkedId = rgLockType.getCheckedRadioButtonId();
        if (checkedId == R.id.rbPin) {
            order.lockType = "PIN";
            order.lockCode = etPin.getText().toString();
        } else if (checkedId == R.id.rbPattern) {
            order.lockType = "Patrón";
            order.patternSequence = currentPattern;
        } else {
            order.lockType = "Ninguno";
        }

        executor.execute(() -> {
            long id = db.repairOrderDao().insert(order);
            runOnUiThread(() -> {
                Toast.makeText(IntakeActivity.this, "Orden guardada con éxito", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(IntakeActivity.this, ReceiptActivity.class);
                intent.putExtra("order_id", (int) id);
                startActivity(intent);
                finish();
            });
        });
    }
}
