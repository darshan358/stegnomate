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

    public static File encryptAudio(File inputFile, String secretMessage, String secretKey, String outputFilePath) {
        File encryptedFile = new File(outputFilePath);

        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(encryptedFile)) {

            byte[] buffer = new byte[1024];
            int length;

            // Convert the secret message to bytes and encrypt it using XOR
            byte[] secretMessageBytes = xorWithKey(secretMessage.getBytes(StandardCharsets.UTF_8), secretKey.getBytes());
            int messageLength = secretMessageBytes.length;

            // Write the length and the secret message to the file header
            fos.write(ByteBuffer.allocate(4).putInt(messageLength).array());  // Store length of secret message
            fos.write(secretMessageBytes);  // Store the encrypted message

            // Write the original audio data to the output file without modification
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
            encryptedStream.read(lengthBuffer);  // Read the length of the hidden message
            int messageLength = ByteBuffer.wrap(lengthBuffer).getInt();

            byte[] secretMessageBytes = new byte[messageLength];
            encryptedStream.read(secretMessageBytes);  // Read the encrypted message bytes

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
