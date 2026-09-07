package com.example;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class SetupActivity extends AppCompatActivity {

    private EditText etShopName, etOwnerName, etShopLocation, etShopPhone, etShopTerms, etWarrantyDays, etSettingsPin;
    private EditText etShopFacebook, etShopInstagram, etShopTiktok;
    private SwitchCompat swRequireSignature, swEnableSocialMedia, swShowAmounts;
    private Spinner spCountry, spThemeMode;
    private TextInputLayout tilShopPhone;
    private ImageView ivLogo;
    private LinearLayout llStep1, llStep2, llStep3, llStepIndicator, llBackupSection, llSocialMediaFields;
    private View vStep1, vStep2, vStep3;
    private TextView tvWelcome, tvStepDescription;
    private AppBarLayout appBarLayout;
    private boolean isEditMode = false;
    private String selectedLogoPath = null;

    private static class CountryConfig {
        String name;
        String code;
        String currency;
        int phoneLength;

        CountryConfig(String name, String code, String currency, int phoneLength) {
            this.name = name;
            this.code = code;
            this.currency = currency;
            this.phoneLength = phoneLength;
        }

        @Override
        public String toString() {
            return name + " (" + code + ")";
        }
    }

    private final CountryConfig[] countries = {
            new CountryConfig("Bolivia", "+591", "Bs.", 8),
            new CountryConfig("Argentina", "+54", "$", 10),
            new CountryConfig("Chile", "+56", "$", 9),
            new CountryConfig("Colombia", "+57", "$", 10),
            new CountryConfig("Ecuador", "+593", "$", 9),
            new CountryConfig("España", "+34", "€", 9),
            new CountryConfig("Estados Unidos", "+1", "$", 10),
            new CountryConfig("México", "+52", "$", 10),
            new CountryConfig("Paraguay", "+595", "Gs.", 9),
            new CountryConfig("Perú", "+51", "S/.", 9),
            new CountryConfig("Uruguay", "+598", "$", 8),
            new CountryConfig("Venezuela", "+58", "Bs.", 10),
            new CountryConfig("Otro País", "+", "$", 15)
    };

    private ActivityResultLauncher<String> imagePickerLauncher;
    private ActivityResultLauncher<String> exportBackupLauncher;
    private ActivityResultLauncher<String> importBackupLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        isEditMode = getIntent().getBooleanExtra("isEditMode", false);
        
        if (!isEditMode && isSetupCompleted()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_setup);

        setupLaunchers();
        initViews();
        loadExistingData();

        if (isEditMode) {
            setupEditModeUI();
        } else {
            setupWizardUI();
        }
    }

    private void setupLaunchers() {
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                saveLogoToInternalStorage(uri);
            }
        });

        exportBackupLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument("application/octet-stream"), uri -> {
            if (uri != null) {
                exportDatabase(uri);
            }
        });

        importBackupLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                importDatabase(uri);
            }
        });
    }

    private void initViews() {
        appBarLayout = findViewById(R.id.appBarLayout);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        llStepIndicator = findViewById(R.id.llStepIndicator);
        vStep1 = findViewById(R.id.vStep1);
        vStep2 = findViewById(R.id.vStep2);
        vStep3 = findViewById(R.id.vStep3);

        tvWelcome = findViewById(R.id.tvWelcome);
        tvStepDescription = findViewById(R.id.tvStepDescription);

        ivLogo = findViewById(R.id.ivLogo);
        etShopName = findViewById(R.id.etShopName);
        etOwnerName = findViewById(R.id.etOwnerName);
        etShopLocation = findViewById(R.id.etShopLocation);
        spCountry = findViewById(R.id.spCountry);
        etShopPhone = findViewById(R.id.etShopPhone);
        tilShopPhone = findViewById(R.id.tilShopPhone);

        swEnableSocialMedia = findViewById(R.id.swEnableSocialMedia);
        llSocialMediaFields = findViewById(R.id.llSocialMediaFields);
        etShopFacebook = findViewById(R.id.etShopFacebook);
        etShopInstagram = findViewById(R.id.etShopInstagram);
        etShopTiktok = findViewById(R.id.etShopTiktok);
        etSettingsPin = findViewById(R.id.etSettingsPin);

        spThemeMode = findViewById(R.id.spThemeMode);
        etWarrantyDays = findViewById(R.id.etWarrantyDays);
        etShopTerms = findViewById(R.id.etShopTerms);
        swRequireSignature = findViewById(R.id.swRequireSignature);
        swShowAmounts = findViewById(R.id.swShowAmounts);

        llStep1 = findViewById(R.id.llStep1);
        llStep2 = findViewById(R.id.llStep2);
        llStep3 = findViewById(R.id.llStep3);
        llBackupSection = findViewById(R.id.llBackupSection);

        // Logo click
        ivLogo.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        // Setup country adapter with simple list item
        ArrayAdapter<CountryConfig> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, countries);
        spCountry.setAdapter(adapter);

        // Setup theme spinner
        ArrayAdapter<String> themeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, 
                new String[]{"Automático (Sistema)", "Modo Claro", "Modo Oscuro"});
        spThemeMode.setAdapter(themeAdapter);
        spThemeMode.setSelection(PreferenceManager.getThemeMode(this));

        spCountry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                CountryConfig selected = countries[position];
                tilShopPhone.setPrefixText(selected.code + " ");
                etShopPhone.setFilters(new InputFilter[]{new InputFilter.LengthFilter(selected.phoneLength)});
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        swEnableSocialMedia.setOnCheckedChangeListener((buttonView, isChecked) -> {
            llSocialMediaFields.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        // Backup buttons
        findViewById(R.id.btnBackup).setOnClickListener(v -> exportBackupLauncher.launch("MoiBit_Backup.db"));
        findViewById(R.id.btnRestore).setOnClickListener(v -> importBackupLauncher.launch("*/*"));
    }

    private void saveLogoToInternalStorage(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            File logoFile = new File(getFilesDir(), "shop_logo.png");
            OutputStream outputStream = new FileOutputStream(logoFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();

            selectedLogoPath = logoFile.getAbsolutePath();
            ivLogo.setImageBitmap(BitmapFactory.decodeFile(selectedLogoPath));
            Toast.makeText(this, "Logo cargado", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error al guardar el logo", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadExistingData() {
        SharedPreferences prefs = getSharedPreferences("ShopPrefs", MODE_PRIVATE);
        etShopName.setText(prefs.getString("shopName", ""));
        etOwnerName.setText(prefs.getString("ownerName", ""));
        etShopLocation.setText(prefs.getString("shopLocation", ""));
        etShopPhone.setText(prefs.getString("shopPhone", ""));

        String savedCode = prefs.getString("countryCode", "+591");
        for (int i = 0; i < countries.length; i++) {
            if (countries[i].code.equals(savedCode)) {
                spCountry.setSelection(i);
                break;
            }
        }

        String fb = PreferenceManager.getFacebook(this);
        String ig = PreferenceManager.getInstagram(this);
        String tk = PreferenceManager.getTiktok(this);
        etShopFacebook.setText(fb);
        etShopInstagram.setText(ig);
        etShopTiktok.setText(tk);

        boolean hasSocial = !fb.isEmpty() || !ig.isEmpty() || !tk.isEmpty();
        swEnableSocialMedia.setChecked(hasSocial);
        llSocialMediaFields.setVisibility(hasSocial ? View.VISIBLE : View.GONE);

        etSettingsPin.setText(PreferenceManager.getSettingsPin(this));

        etWarrantyDays.setText(String.valueOf(PreferenceManager.getWarrantyDays(this)));
        etShopTerms.setText(PreferenceManager.getTerms(this));
        swRequireSignature.setChecked(PreferenceManager.isSignatureRequired(this));
        swShowAmounts.setChecked(PreferenceManager.shouldShowAmounts(this));

        selectedLogoPath = PreferenceManager.getLogoPath(this);
        if (selectedLogoPath != null && new File(selectedLogoPath).exists()) {
            ivLogo.setImageBitmap(BitmapFactory.decodeFile(selectedLogoPath));
        }
    }

    private void setupWizardUI() {
        appBarLayout.setVisibility(View.GONE);
        llStepIndicator.setVisibility(View.VISIBLE);
        showStep(1);

        findViewById(R.id.btnNext1).setOnClickListener(v -> {
            if (validateStep1()) showStep(2);
        });

        findViewById(R.id.btnSkipSetup).setOnClickListener(v -> {
            saveSetupData();
            completeSetup();
        });

        findViewById(R.id.btnNext2).setOnClickListener(v -> showStep(3));
        findViewById(R.id.tvBack1).setOnClickListener(v -> showStep(1));

        findViewById(R.id.btnSaveSetup).setOnClickListener(v -> {
            saveSetupData();
            completeSetup();
        });
        findViewById(R.id.tvBack2).setOnClickListener(v -> showStep(2));
    }

    private void setupEditModeUI() {
        appBarLayout.setVisibility(View.VISIBLE);
        llStepIndicator.setVisibility(View.GONE);
        tvWelcome.setText("Configuración General");
        tvStepDescription.setVisibility(View.GONE);

        llStep1.setVisibility(View.VISIBLE);
        llStep2.setVisibility(View.VISIBLE);
        llStep3.setVisibility(View.VISIBLE);
        llBackupSection.setVisibility(View.VISIBLE);

        findViewById(R.id.btnNext1).setVisibility(View.GONE);
        findViewById(R.id.btnSkipSetup).setVisibility(View.GONE);
        findViewById(R.id.btnNext2).setVisibility(View.GONE);
        findViewById(R.id.tvBack1).setVisibility(View.GONE);
        findViewById(R.id.tvBack2).setVisibility(View.GONE);

        findViewById(R.id.btnSaveSetup).setOnClickListener(v -> {
            if (validateStep1()) {
                saveSetupData();
                Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void showStep(int step) {
        llStep1.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        llStep2.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        llStep3.setVisibility(step == 3 ? View.VISIBLE : View.GONE);

        tvStepDescription.setText("Paso " + step + " de 3");

        int activeColor = getResources().getColor(R.color.brandBlue);
        int inactiveColor = getResources().getColor(R.color.divider);

        vStep1.setBackgroundColor(step >= 1 ? activeColor : inactiveColor);
        vStep2.setBackgroundColor(step >= 2 ? activeColor : inactiveColor);
        vStep3.setBackgroundColor(step >= 3 ? activeColor : inactiveColor);
    }

    private boolean validateStep1() {
        if (etShopName.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "El nombre del taller es obligatorio", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void saveSetupData() {
        SharedPreferences prefs = getSharedPreferences("ShopPrefs", MODE_PRIVATE);
        CountryConfig selected = (CountryConfig) spCountry.getSelectedItem();

        String name = etShopName.getText().toString().trim();
        if (name.isEmpty()) name = "Mi Taller";

        prefs.edit()
                .putString("shopName", name)
                .putString("ownerName", etOwnerName.getText().toString().trim())
                .putString("shopLocation", etShopLocation.getText().toString().trim())
                .putString("shopPhone", etShopPhone.getText().toString().trim())
                .putString("countryCode", selected.code)
                .putString("currencySymbol", selected.currency)
                .putInt("phoneLength", selected.phoneLength)
                .putBoolean("setupCompleted", true)
                .apply();

        PreferenceManager.setLogoPath(this, selectedLogoPath);

        if (swEnableSocialMedia.isChecked()) {
            prefs.edit()
                    .putString("shopFacebook", etShopFacebook.getText().toString().trim())
                    .putString("shopInstagram", etShopInstagram.getText().toString().trim())
                    .putString("shopTiktok", etShopTiktok.getText().toString().trim())
                    .apply();
        } else {
            prefs.edit()
                    .putString("shopFacebook", "")
                    .putString("shopInstagram", "")
                    .putString("shopTiktok", "")
                    .apply();
        }

        PreferenceManager.setSettingsPin(this, etSettingsPin.getText().toString().trim());

        try {
            int days = Integer.parseInt(etWarrantyDays.getText().toString().trim());
            PreferenceManager.setWarrantyDays(this, days);
        } catch (Exception ignored) {}

        PreferenceManager.setTerms(this, etShopTerms.getText().toString().trim());
        PreferenceManager.setSignatureRequired(this, swRequireSignature.isChecked());
        PreferenceManager.setShowAmounts(this, swShowAmounts.isChecked());

        PreferenceManager.setThemeMode(this, spThemeMode.getSelectedItemPosition());
    }

    private void completeSetup() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private boolean isSetupCompleted() {
        return getSharedPreferences("ShopPrefs", MODE_PRIVATE).getBoolean("setupCompleted", false);
    }

    private void exportDatabase(Uri uri) {
        try {
            File currentDB = getDatabasePath("moibitrepair_db");
            if (!currentDB.exists()) {
                Toast.makeText(this, "No hay datos para exportar", Toast.LENGTH_SHORT).show();
                return;
            }

            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            FileInputStream inputStream = new FileInputStream(currentDB);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            Toast.makeText(this, "Copia de seguridad exportada con éxito", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error al exportar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void importDatabase(Uri uri) {
        new AlertDialog.Builder(this)
                .setTitle("Restaurar Copia")
                .setMessage("Esta acción reemplazará los datos actuales. ¿Deseas continuar?")
                .setPositiveButton("Restaurar", (dialog, which) -> {
                    try {
                        AppDatabase.resetInstance();
                        File currentDB = getDatabasePath("moibitrepair_db");

                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        FileOutputStream outputStream = new FileOutputStream(currentDB);

                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, length);
                        }

                        outputStream.flush();
                        outputStream.close();
                        inputStream.close();

                        Toast.makeText(this, "Base de datos restaurada con éxito", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } catch (Exception e) {
                        Toast.makeText(this, "Error al importar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
