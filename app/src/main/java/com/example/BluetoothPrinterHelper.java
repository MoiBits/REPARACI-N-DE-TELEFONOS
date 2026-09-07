package com.example;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Base64;
import java.io.IOException;
import java.io.OutputStream;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class BluetoothPrinterHelper {

    private static final UUID PRINTER_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private Context context;

    public BluetoothPrinterHelper(Context context) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @SuppressLint("MissingPermission")
    public boolean connectAndPrint(RepairOrder order) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) return false;

        BluetoothDevice printerDevice = findPrinter();
        if (printerDevice == null) return false;

        try {
            socket = printerDevice.createRfcommSocketToServiceRecord(PRINTER_UUID);
            socket.connect();
            outputStream = socket.getOutputStream();
            printRepairReceipt(order);
            outputStream.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (socket != null) socket.close();
            } catch (IOException ignored) {}
        }
    }

    @SuppressLint("MissingPermission")
    public boolean connectAndPrintSale(PhoneSale sale) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) return false;

        BluetoothDevice printerDevice = findPrinter();
        if (printerDevice == null) return false;

        try {
            socket = printerDevice.createRfcommSocketToServiceRecord(PRINTER_UUID);
            socket.connect();
            outputStream = socket.getOutputStream();
            printSaleReceipt(sale);
            outputStream.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (socket != null) socket.close();
            } catch (IOException ignored) {}
        }
    }

    @SuppressLint("MissingPermission")
    private BluetoothDevice findPrinter() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices != null && pairedDevices.size() > 0) {
            BluetoothDevice fallback = null;
            for (BluetoothDevice device : pairedDevices) {
                if (fallback == null) {
                    fallback = device;
                }
                String name = device.getName();
                if (!TextUtils.isEmpty(name)) {
                    String normalizedName = name.toLowerCase(Locale.ROOT);
                    if (normalizedName.contains("printer")
                            || normalizedName.contains("print")
                            || normalizedName.contains("pos")
                            || normalizedName.contains("thermal")
                            || normalizedName.contains("tpv")) {
                        return device;
                    }
                }
            }
            return fallback;
        }
        return null;
    }

    private String stripAccents(String s) {
        if (s == null) return "";
        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        s = s.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return s;
    }

    private void writeText(String text) throws IOException {
        outputStream.write(stripAccents(text).getBytes("GBK"));
    }

    private void printRepairReceipt(RepairOrder order) throws IOException {
        SharedPreferences prefs = context.getSharedPreferences("ShopPrefs", Context.MODE_PRIVATE);
        String shopName = prefs.getString("shopName", "MoiBit Repair");
        String shopLocation = prefs.getString("shopLocation", "");
        String shopPhone = prefs.getString("shopPhone", "");
        String currency = PreferenceManager.getCurrencySymbol(context);

        byte[] boldOn = {27, 69, 1};
        byte[] boldOff = {27, 69, 0};
        byte[] center = {27, 97, 1};
        byte[] left = {27, 97, 0};
        byte[] bigSize = {29, 33, 17};
        byte[] normalSize = {29, 33, 0};

        outputStream.write(center);
        outputStream.write(bigSize);
        writeText(shopName + "\n");
        outputStream.write(normalSize);
        writeText(shopLocation + "\n");
        if (!shopPhone.isEmpty()) writeText("Cel: " + shopPhone + "\n");
        outputStream.write("--------------------------------\n".getBytes());
        
        outputStream.write(left);
        writeText("ORDEN: #" + String.format("%03d", order.id) + "\n");
        writeText("CLIENTE: " + order.customerName + "\n");
        writeText("EQUIPO: " + (order.deviceBrand + " " + order.deviceModel) + "\n");
        
        outputStream.write("--------------------------------\n".getBytes());
        outputStream.write(boldOn);
        writeText("DETALLE DE SERVICIOS:\n");
        outputStream.write(boldOff);
        writeText(order.issue + "\n");
        
        outputStream.write("--------------------------------\n".getBytes());
        outputStream.write(boldOn);
        writeText("TOTAL: " + currency + " " + String.format("%.2f", order.estimatedPrice) + "\n");
        outputStream.write(boldOff);
        writeText("ANTICIPO: " + currency + " " + String.format("%.2f", order.downPayment) + "\n");
        
        if ("Cancelado".equals(order.paymentStatus)) {
            outputStream.write(center);
            outputStream.write(boldOn);
            writeText("ESTADO: !!! PAGADO !!!\n");
            outputStream.write(boldOff);
            outputStream.write(left);
        } else {
            double balance = order.estimatedPrice - order.downPayment;
            outputStream.write(boldOn);
            writeText("SALDO: " + currency + " " + String.format("%.2f", Math.max(0, balance)) + "\n");
            outputStream.write(boldOff);
        }

        outputStream.write("--------------------------------\n".getBytes());

        if (order.customerSignature != null && !order.customerSignature.isEmpty()) {
            outputStream.write(center);
            writeText("FIRMA DEL CLIENTE:\n\n");
            Bitmap signatureBitmap = base64ToBitmap(order.customerSignature);
            if (signatureBitmap != null) {
                printPhoto(signatureBitmap);
            }
            outputStream.write("\n--------------------------------\n".getBytes());
        }
        
        outputStream.write(center);
        writeText("Gracias por su confianza\n\n\n\n");
    }

    private void printSaleReceipt(PhoneSale sale) throws IOException {
        SharedPreferences prefs = context.getSharedPreferences("ShopPrefs", Context.MODE_PRIVATE);
        String shopName = prefs.getString("shopName", "MoiBit Repair Phone");
        String shopLocation = prefs.getString("shopLocation", "");
        String shopPhone = prefs.getString("shopPhone", "");
        String owner = prefs.getString("ownerName", "");
        String currency = PreferenceManager.getCurrencySymbol(context);

        byte[] boldOn = {27, 69, 1};
        byte[] boldOff = {27, 69, 0};
        byte[] center = {27, 97, 1};
        byte[] left = {27, 97, 0};
        byte[] bigSize = {29, 33, 17};
        byte[] normalSize = {29, 33, 0};

        outputStream.write(center);
        outputStream.write(bigSize);
        writeText(shopName + "\n");
        outputStream.write(normalSize);
        if (!owner.isEmpty()) writeText("Atendido por: " + owner + "\n");
        writeText(shopLocation + "\n");
        if (!shopPhone.isEmpty()) writeText("Cel: " + shopPhone + "\n");
        outputStream.write("--------------------------------\n".getBytes());
        
        outputStream.write(boldOn);
        writeText("NOTA DE VENTA\n");
        outputStream.write(boldOff);
        writeText("VENTA: #" + String.format("%03d", sale.id) + "\n");
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        writeText("FECHA: " + sdf.format(new Date(sale.saleDate)) + "\n");
        outputStream.write("--------------------------------\n".getBytes());

        outputStream.write(left);
        writeText("CLIENTE: " + sale.customerName + "\n");
        writeText("TELEFONO: " + sale.customerPhone + "\n");
        outputStream.write("--------------------------------\n".getBytes());
        
        outputStream.write(boldOn);
        writeText("DETALLES DEL EQUIPO:\n");
        outputStream.write(boldOff);
        writeText("MARCA: " + sale.deviceBrand + "\n");
        writeText("MODELO: " + sale.deviceModel + "\n");
        writeText("IMEI: " + sale.imei + "\n");
        writeText("CONDICION: " + sale.condition + "\n");
        outputStream.write("--------------------------------\n".getBytes());

        outputStream.write(boldOn);
        writeText("TOTAL PAGADO: " + currency + " " + String.format("%.2f", sale.price) + "\n");
        writeText("GARANTIA: " + sale.warrantyDays + " DIAS\n");
        outputStream.write(boldOff);
        
        if (sale.notes != null && !sale.notes.isEmpty()) {
            writeText("NOTAS: " + sale.notes + "\n");
        }

        outputStream.write("--------------------------------\n".getBytes());

        if (sale.customerSignature != null && !sale.customerSignature.isEmpty()) {
            outputStream.write(center);
            writeText("FIRMA DEL COMPRADOR:\n\n");
            Bitmap signatureBitmap = base64ToBitmap(sale.customerSignature);
            if (signatureBitmap != null) {
                printPhoto(signatureBitmap);
            }
            outputStream.write("\n--------------------------------\n".getBytes());
        }
        
        outputStream.write(center);
        writeText("Gracias por su compra\n\n\n\n");
    }

    private Bitmap base64ToBitmap(String base64Str) {
        try {
            byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    private void printPhoto(Bitmap bitmap) throws IOException {
        int width = 384;
        int height = (bitmap.getHeight() * width) / bitmap.getWidth();
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        outputStream.write(PrinterGraphics.decodeBitmap(scaledBitmap));
    }
}
