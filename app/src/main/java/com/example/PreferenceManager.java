package com.example;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class PreferenceManager {
    private static final String PREF_NAME = "ShopPrefs";
    private static final String KEY_SHOP_NAME = "shopName";
    private static final String KEY_OWNER_NAME = "ownerName";
    private static final String KEY_SHOP_LOCATION = "shopLocation";
    private static final String KEY_SHOP_PHONE = "shopPhone";
    private static final String KEY_CURRENCY = "currencySymbol";
    private static final String KEY_COUNTRY_CODE = "countryCode";
    private static final String KEY_PHONE_LENGTH = "phoneLength";
    private static final String KEY_REQUIRE_SIGNATURE = "requireSignature";
    private static final String KEY_TERMS = "shopTerms";
    private static final String KEY_WARRANTY_DAYS = "warrantyDays";
    private static final String KEY_FACEBOOK = "shopFacebook";
    private static final String KEY_INSTAGRAM = "shopInstagram";
    private static final String KEY_TIKTOK = "shopTiktok";
    private static final String KEY_SETTINGS_PIN = "settingsPin";
    private static final String KEY_LOGO_PATH = "shopLogoPath";
    private static final String KEY_SHOW_AMOUNTS = "showAmounts";
    private static final String KEY_THEME_MODE = "themeMode"; // 0: System, 1: Light, 2: Dark
    
    private static final String DEFAULT_CURRENCY = "Bs.";
    private static final String DEFAULT_COUNTRY_CODE = "+591";
    private static final int DEFAULT_PHONE_LENGTH = 8;
    private static final boolean DEFAULT_REQUIRE_SIGNATURE = true;
    private static final int DEFAULT_WARRANTY_DAYS = 30;
    private static final String DEFAULT_TERMS = "1. El taller no se hace responsable por la pérdida de datos; el cliente debe realizar su propio respaldo.\n2. Equipos no retirados después de 90 días serán considerados abandonados y pasarán a propiedad del taller.\n3. La garantía de reparación es de 30 días y no cubre daños por humedad, golpes o intervención de terceros.\n4. Todo equipo ingresa bajo riesgo de daño colateral si presenta fallas previas graves o humedad.";

    public static String getShopName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_SHOP_NAME, "Mi Taller");
    }

    public static String getShopLocation(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_SHOP_LOCATION, "Dirección no especificada");
    }

    public static String getShopPhone(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_SHOP_PHONE, "");
    }

    public static String getCurrencySymbol(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_CURRENCY, DEFAULT_CURRENCY);
    }

    public static String getCountryCode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_COUNTRY_CODE, DEFAULT_COUNTRY_CODE);
    }

    public static int getPhoneLength(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_PHONE_LENGTH, DEFAULT_PHONE_LENGTH);
    }

    public static boolean isSignatureRequired(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_REQUIRE_SIGNATURE, DEFAULT_REQUIRE_SIGNATURE);
    }

    public static void setSignatureRequired(Context context, boolean required) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_REQUIRE_SIGNATURE, required).apply();
    }

    public static String getTerms(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TERMS, DEFAULT_TERMS);
    }

    public static void setTerms(Context context, String terms) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_TERMS, terms).apply();
    }

    public static int getWarrantyDays(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_WARRANTY_DAYS, DEFAULT_WARRANTY_DAYS);
    }

    public static void setWarrantyDays(Context context, int days) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_WARRANTY_DAYS, days).apply();
    }

    public static String getFacebook(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_FACEBOOK, "");
    }

    public static String getInstagram(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_INSTAGRAM, "");
    }

    public static String getTiktok(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TIKTOK, "");
    }

    public static String getSettingsPin(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_SETTINGS_PIN, "");
    }

    public static void setSettingsPin(Context context, String pin) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_SETTINGS_PIN, pin).apply();
    }

    public static String getLogoPath(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LOGO_PATH, null);
    }

    public static void setLogoPath(Context context, String path) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LOGO_PATH, path).apply();
    }

    public static boolean shouldShowAmounts(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_SHOW_AMOUNTS, true);
    }

    public static void setShowAmounts(Context context, boolean show) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_SHOW_AMOUNTS, show).apply();
    }

    public static int getThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME_MODE, 0);
    }

    public static void setThemeMode(Context context, int mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
        applyTheme(mode);
    }

    public static void applyTheme(int mode) {
        switch (mode) {
            case 1:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 2:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}
