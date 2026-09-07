package com.example;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "repair_orders")
public class RepairOrder {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String customerName;
    public String customerPhone;
    public String deviceBrand; // Samsung, Xiaomi, etc.
    public String deviceModel; // A14, iPhone 13, etc.
    public String deviceColor;
    public String imei;
    public String physicalCondition;
    public String accessories;
    public String issue;
    public String status; // Pendiente, En Taller, Listo, Entregado
    public String paymentStatus; // Por Pagar, Cancelado
    public double estimatedPrice;
    public double downPayment;
    public String lockType; // Ninguno, PIN, Patrón
    public String lockCode; // For PIN
    public String patternSequence; // For Pattern (e.g., "1245")
    public String customerSignature; // Base64 string of the signature
    public long entryDate;

    public RepairOrder() {
        this.entryDate = System.currentTimeMillis();
        this.paymentStatus = "Por Pagar"; // Default value
    }
}
