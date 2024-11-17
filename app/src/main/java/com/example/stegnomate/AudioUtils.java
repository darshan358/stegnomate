package com.example.stegnomate;

import android.util.Log;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class AudioUtils {

    private static final String TAG = "AudioUtils";
    private static final String KEY_MARKER = "KEYMARKER";  // Used to identify the start of key in encrypted data

    // Encrypt audio and embed secret message
    public static File encryptAudio(File inputFile, String secretMessage, String secretKey, String outputFilePath) {
        File encryptedFile = new File(outputFilePath);

        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(encryptedFile)) {

            byte[] buffer = new byte[1024];
            int length;

            // Encrypt key and message
            byte[] keyBytes = xorWithKey(KEY_MARKER.getBytes(StandardCharsets.UTF_8), secretKey.getBytes());
            byte[] secretMessageBytes = xorWithKey(secretMessage.getBytes(StandardCharsets.UTF_8), secretKey.getBytes());

            // Write header with encrypted key and message
            fos.write(ByteBuffer.allocate(4).putInt(keyBytes.length).array()); // Key length
            fos.write(keyBytes); // Encrypted key
            fos.write(ByteBuffer.allocate(4).putInt(secretMessageBytes.length).array()); // Message length
            fos.write(secretMessageBytes); // Encrypted message

            // Copy raw audio data
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }

            Log.i(TAG, "Audio encryption completed successfully.");
        } catch (IOException e) {
            Log.e(TAG, "Error during audio encryption: " + e.getMessage(), e);
        }

        return encryptedFile;
    }

    // Decrypt audio and extract secret message
    public static String decryptAudio(InputStream encryptedStream, String secretKey) {
        String secretMessage = "";

        try {
            byte[] lengthBuffer = new byte[4];

            // Read and validate key
            if (encryptedStream.read(lengthBuffer) != 4) {
                throw new IOException("Failed to read key length");
            }
            int keyLength = ByteBuffer.wrap(lengthBuffer).getInt();

            byte[] keyBytes = new byte[keyLength];
            if (encryptedStream.read(keyBytes) != keyLength) {
                throw new IOException("Failed to read key bytes");
            }

            String extractedKey = new String(xorWithKey(keyBytes, secretKey.getBytes()), StandardCharsets.UTF_8);
            if (!extractedKey.equals(KEY_MARKER)) {
                throw new IOException("Key does not match marker");
            }

            // Read and decrypt the message
            if (encryptedStream.read(lengthBuffer) != 4) {
                throw new IOException("Failed to read message length");
            }
            int messageLength = ByteBuffer.wrap(lengthBuffer).getInt();

            if (messageLength < 0 || messageLength > 1_000_000) { // Safeguard against invalid lengths
                throw new IOException("Invalid message length");
            }

            byte[] secretMessageBytes = new byte[messageLength];
            if (encryptedStream.read(secretMessageBytes) != messageLength) {
                throw new IOException("Failed to read message bytes");
            }

            // Decrypt the secret message
            byte[] decryptedBytes = xorWithKey(secretMessageBytes, secretKey.getBytes());
            secretMessage = new String(decryptedBytes, StandardCharsets.UTF_8);

            Log.i(TAG, "Audio decryption completed successfully.");
        } catch (IOException e) {
            Log.e(TAG, "Decryption failed: " + e.getMessage(), e);
        }

        return secretMessage;
    }

    // XOR encryption/decryption utility
    private static byte[] xorWithKey(byte[] data, byte[] key) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ key[i % key.length]);
        }
        return result;
    }
}
