package com.example;

import android.graphics.Bitmap;
import android.graphics.Color;

public class PrinterGraphics {

    public static byte[] decodeBitmap(Bitmap bmp) {
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        int horizontalBytes = (width + 7) / 8;
        
        // ESC/POS GS v 0 command header
        byte[] command = new byte[8 + horizontalBytes * height];
        command[0] = 29; // GS
        command[1] = 118; // v
        command[2] = 48; // 0
        command[3] = 0; // m (normal mode)
        command[4] = (byte) (horizontalBytes % 256); // xL
        command[5] = (byte) (horizontalBytes / 256); // xH
        command[6] = (byte) (height % 256); // yL
        command[7] = (byte) (height / 256); // yH

        int pos = 8;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < horizontalBytes; x++) {
                byte b = 0;
                for (int bit = 0; x * 8 + bit < width && bit < 8; bit++) {
                    int pixel = bmp.getPixel(x * 8 + bit, y);
                    int gray = (int) (0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel));
                    if (gray < 128) { // Black pixel
                        b |= (byte) (1 << (7 - bit));
                    }
                }
                command[pos++] = b;
            }
        }
        return command;
    }
}
