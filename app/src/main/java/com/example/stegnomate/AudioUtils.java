package com.example.stegnomate;

import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class AudioUtils {

    private static final String TAG = "AudioUtils";
    private static final String KEY_MARKER = "KEYMARKER";  // Used to mark the beginning of the key in encrypted data

    public static File encryptAudio(File inputFile, String secretMessage, String secretKey, String outputFilePath) {
        File encryptedFile = new File(outputFilePath);

        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(encryptedFile)) {

            byte[] buffer = new byte[1024];
            int length;

            // Convert the key and message to bytes, and encrypt them
            byte[] keyBytes = xorWithKey(KEY_MARKER.getBytes(StandardCharsets.UTF_8), secretKey.getBytes());
            byte[] secretMessageBytes = xorWithKey(secretMessage.getBytes(StandardCharsets.UTF_8), secretKey.getBytes());

            // Write lengths and contents to the file header
            fos.write(ByteBuffer.allocate(4).putInt(keyBytes.length).array());  // Key length
            fos.write(keyBytes);  // Write the encrypted key
            fos.write(ByteBuffer.allocate(4).putInt(secretMessageBytes.length).array());  // Message length
            fos.write(secretMessageBytes);  // Write the encrypted message

            // Write the audio data to the output file without modification
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }

            Log.i(TAG, "Audio encryption completed successfully.");
        } catch (IOException e) {
            Log.e(TAG, "Error during audio encryption: " + e.getMessage(), e);
        }
        return encryptedFile;
    }

    public static String decryptAudio(InputStream encryptedStream, String secretKey) {
        String secretMessage = "";

        try {
            byte[] lengthBuffer = new byte[4];

            // Read and validate key
            encryptedStream.read(lengthBuffer);  // Read the key length
            int keyLength = ByteBuffer.wrap(lengthBuffer).getInt();
            byte[] keyBytes = new byte[keyLength];
            encryptedStream.read(keyBytes);  // Read the encrypted key bytes

            // Decrypt and check key validity
            String extractedKey = new String(xorWithKey(keyBytes, secretKey.getBytes()), StandardCharsets.UTF_8);
            if (!extractedKey.equals(KEY_MARKER)) {
                Log.e(TAG, "Decryption failed: Key does not match marker");
                return null;
            }

            // Read and decrypt the hidden message
            encryptedStream.read(lengthBuffer);  // Read message length
            int messageLength = ByteBuffer.wrap(lengthBuffer).getInt();
            byte[] secretMessageBytes = new byte[messageLength];
            encryptedStream.read(secretMessageBytes);  // Read encrypted message bytes

            // Decrypt the message using XOR
            byte[] decryptedBytes = xorWithKey(secretMessageBytes, secretKey.getBytes());
            secretMessage = new String(decryptedBytes, StandardCharsets.UTF_8);

            Log.i(TAG, "Audio decryption completed successfully.");
        } catch (IOException e) {
            Log.e(TAG, "Error during audio decryption: " + e.getMessage(), e);
        }

        return secretMessage;
    }

    private static byte[] xorWithKey(byte[] data, byte[] key) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ key[i % key.length]);
        }
        return result;
    }
}
