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
    private static final String KEY_MARKER = "000111000"; // Define a marker to separate the key and message

    // Encrypt message and key into an image file
    public static byte[] encryptImage(File coverFile, String secretMessage, String secretKey) {
        try {
            Bitmap coverBitmap = BitmapFactory.decodeFile(coverFile.getAbsolutePath());

            // Append the encrypted key with a key marker and then the encrypted message with an end marker
            String encryptedKey = encryptMessage(secretKey, secretKey) + KEY_MARKER;
            String encryptedMessage = encryptMessage(secretMessage, secretKey) + END_MARKER;
            String combinedMessage = encryptedKey + encryptedMessage;

            Bitmap modifiedBitmap = hideMessageInImage(coverBitmap, combinedMessage);
            return bitmapToByteArray(modifiedBitmap);
        } catch (Exception e) {
            Log.e(TAG, "Image Encryption Failed: " + e.getMessage(), e);
            return null;
        }
    }

    // Decrypt message from an image file, checking the key
    public static String decryptImage(InputStream inputStream, String secretKey) {
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            String combinedData = extractMessageFromImage(bitmap);

            // Split the extracted data by the key marker
            if (combinedData.contains(KEY_MARKER) && combinedData.contains(END_MARKER)) {
                String[] parts = combinedData.split(KEY_MARKER, 2);
                String extractedKeyBinary = parts[0];

                // Safely check if the message is properly split
                if (parts.length < 2) {
                    Log.e(TAG, "Decryption failed: Incomplete message or key");
                    return null;
                }

                String extractedMessageBinary = parts[1].split(END_MARKER, 2)[0];

                // Convert binary data back to text and validate the key
                String extractedKey = decryptMessage(extractedKeyBinary, secretKey);
                if (!extractedKey.equals(secretKey)) {
                    Log.e(TAG, "Decryption failed: Key does not match");
                    return null; // Key mismatch
                }

                // If key matches, decrypt and return the message
                return decryptMessage(extractedMessageBinary, secretKey);
            } else {
                Log.e(TAG, "Key or end marker not found, incomplete message");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Image Decryption Failed: " + e.getMessage(), e);
            return null;
        }
    }

    // Encrypting message (binary conversion as placeholder)
    private static String encryptMessage(String message, String key) {
        StringBuilder binary = new StringBuilder();
        for (char c : message.toCharArray()) {
            binary.append(String.format("%8s", Integer.toBinaryString(c)).replace(' ', '0'));
        }
        return binary.toString();
    }

    // Decrypting message (binary to text conversion)
    private static String decryptMessage(String binaryMessage, String key) {
        // Check that binaryMessage length is a multiple of 8
        if (binaryMessage.length() % 8 != 0) {
            Log.e(TAG, "Decryption failed: Binary message length is not a multiple of 8");
            return null;
        }

        StringBuilder message = new StringBuilder();
        for (int i = 0; i < binaryMessage.length(); i += 8) {
            String byteStr = binaryMessage.substring(i, Math.min(i + 8, binaryMessage.length()));
            try {
                message.append((char) Integer.parseInt(byteStr, 2));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid binary sequence: " + byteStr);
                return null;
            }
        }
        return message.toString();
    }

    // Hide message in image using LSB
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

    // Extract hidden message from image
    private static String extractMessageFromImage(Bitmap bitmap) {
        StringBuilder message = new StringBuilder();
        for (int x = 0; x < bitmap.getWidth(); x++) {
            for (int y = 0; y < bitmap.getHeight(); y++) {
                int pixel = bitmap.getPixel(x, y);
                message.append((pixel & 1) == 1 ? '1' : '0');

                // Check for end marker or key marker in extracted message
                if (message.length() >= END_MARKER.length() &&
                        message.substring(message.length() - END_MARKER.length()).equals(END_MARKER)) {
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
