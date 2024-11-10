package com.example.stegnomate;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

public class ImageUtils {

    private static final String TAG = "ImageUtils";
    private static final String END_MARKER = "111000111"; // Define an end marker for the message

    // Encrypt message into an image file
    public static byte[] encryptImage(File coverFile, String secretMessage, String secretKey) {
        try {
            Bitmap coverBitmap = BitmapFactory.decodeFile(coverFile.getAbsolutePath());
            String encryptedMessage = encryptMessage(secretMessage, secretKey) + END_MARKER; // Append end marker
            Bitmap modifiedBitmap = hideMessageInImage(coverBitmap, encryptedMessage);
            return bitmapToByteArray(modifiedBitmap);
        } catch (Exception e) {
            Log.e(TAG, "Image Encryption Failed: " + e.getMessage(), e);
            return null;
        }
    }


    // Decrypt message from an image file
    public static String decryptImage(InputStream inputStream, String secretKey) {
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            String hiddenMessage = extractMessageFromImage(bitmap);
            if (hiddenMessage.contains(END_MARKER)) {
                hiddenMessage = hiddenMessage.split(END_MARKER)[0]; // Stop at the end marker
                return decryptMessage(hiddenMessage, secretKey);
            } else {
                Log.e(TAG, "End marker not found, incomplete message");
                return null; // Return null if the end marker is not found
            }
        } catch (Exception e) {
            Log.e(TAG, "Image Decryption Failed: " + e.getMessage(), e);
            return null;
        }
    }


    // Placeholder for encrypting a message (replace with actual AES encryption if needed)
    private static String encryptMessage(String message, String key) {
        // Convert to binary string (simulate encryption for demonstration)
        StringBuilder binary = new StringBuilder();
        for (char c : message.toCharArray()) {
            binary.append(String.format("%8s", Integer.toBinaryString(c)).replace(' ', '0'));
        }
        return binary.toString();
    }



    // Placeholder for decrypting a message (replace with actual AES decryption if needed)
    private static String decryptMessage(String binaryMessage, String key) {
        // Convert from binary string to text (simulate decryption for demonstration)
        StringBuilder message = new StringBuilder();
        for (int i = 0; i < binaryMessage.length(); i += 8) {
            String byteStr = binaryMessage.substring(i, i + 8);
            message.append((char) Integer.parseInt(byteStr, 2));
        }
        return message.toString();
    }


    // Method to hide a message in an image using LSB
    private static Bitmap hideMessageInImage(Bitmap coverBitmap, String message) {
        int messageIndex = 0;
        int messageLength = message.length();
        Bitmap encodedBitmap = coverBitmap.copy(Bitmap.Config.ARGB_8888, true);

        outerLoop:
        for (int x = 0; x < encodedBitmap.getWidth(); x++) {
            for (int y = 0; y < encodedBitmap.getHeight(); y++) {
                if (messageIndex < messageLength) {
                    int pixel = encodedBitmap.getPixel(x, y);
                    int bit = message.charAt(messageIndex) == '1' ? 1 : 0;
                    int newPixel = (pixel & 0xFFFFFFFE) | bit;
                    encodedBitmap.setPixel(x, y, newPixel);
                    messageIndex++;
                } else {
                    break outerLoop;
                }
            }
        }
        return encodedBitmap;
    }

    // Extract a hidden message from an image using LSB
    private static String extractMessageFromImage(Bitmap bitmap) {
        StringBuilder message = new StringBuilder();
        for (int x = 0; x < bitmap.getWidth(); x++) {
            for (int y = 0; y < bitmap.getHeight(); y++) {
                int pixel = bitmap.getPixel(x, y);
                message.append((pixel & 1) == 1 ? '1' : '0');
                // Stop if the end marker is detected
                if (message.length() >= END_MARKER.length() && message.substring(message.length() - END_MARKER.length()).equals(END_MARKER)) {
                    return message.toString();
                }
            }
        }
        return message.toString();
    }

    // Convert Bitmap to byte array
    private static byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

}
