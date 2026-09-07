package com.example;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "phone_sales")
public class PhoneSale {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String customerName;
    public String customerPhone;
    public String deviceBrand;
    public String deviceModel;
    public String deviceColor;
    public String imei;
    public String condition; // Nuevo, Usado, Seminuevo
    public double price;
    public double downPayment;
    public String paymentStatus; // Pagado, Pendiente
    public int warrantyDays;
    public String notes;
    public String customerSignature;
    public long saleDate;

    public PhoneSale() {
        this.saleDate = System.currentTimeMillis();
        this.paymentStatus = "Pagado";
    }
}
