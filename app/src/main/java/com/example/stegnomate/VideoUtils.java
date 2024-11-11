package com.example.stegnomate;

import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class VideoUtils {

    private static final String TAG = "VideoUtils";
    private static final String KEY_MARKER = "VIDKEYMARKER";  // Used to mark the beginning of the key in encrypted data

    // Encrypts the secret message into the video file
    public static File encryptVideo(File inputFile, String secretMessage, String secretKey, String outputFilePath) {
        File encryptedFile = new File(outputFilePath);
        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(encryptedFile)) {

            byte[] buffer = new byte[1024];
            int length;

            // Encrypt the key marker and secret message
            byte[] keyBytes = xorWithKey(KEY_MARKER.getBytes(StandardCharsets.UTF_8), secretKey.getBytes());
            byte[] secretMessageBytes = secretMessage.getBytes(StandardCharsets.UTF_8);
            int messageLengthBits = secretMessageBytes.length * 8;

            // Write header with key, key length, message length, and encrypted message
            fos.write(ByteBuffer.allocate(4).putInt(keyBytes.length).array());  // Key length
            fos.write(keyBytes);  // Encrypted key marker
            fos.write(ByteBuffer.allocate(4).putInt(messageLengthBits).array());  // Message length

            int messageBitIndex = 0;  // Track which bit of the message we're encoding
            while ((length = fis.read(buffer)) > 0) {
                for (int i = 0; i < length; i++) {
                    if (messageBitIndex < messageLengthBits) {
                        int byteIndex = messageBitIndex / 8;
                        int bitIndex = messageBitIndex % 8;

                        int secretBit = (secretMessageBytes[byteIndex] >> (7 - bitIndex)) & 1;
                        buffer[i] = (byte) ((buffer[i] & 0xFE) | secretBit);

                        messageBitIndex++;
                    }
                }
                fos.write(buffer, 0, length);
            }

            Log.i(TAG, "Video encryption completed successfully.");
        } catch (IOException e) {
            Log.e(TAG, "Error during video encryption: " + e.getMessage(), e);
        }
        return encryptedFile;
    }

    // Decrypts the secret message from the video file
    public static String decryptVideo(InputStream encryptedStream, String secretKey) {
        StringBuilder secretMessage = new StringBuilder();
        try {
            byte[] lengthBuffer = new byte[4];

            // Read and validate the key
            encryptedStream.read(lengthBuffer);  // Read the key length
            int keyLength = ByteBuffer.wrap(lengthBuffer).getInt();
            byte[] keyBytes = new byte[keyLength];
            encryptedStream.read(keyBytes);

            String extractedKey = new String(xorWithKey(keyBytes, secretKey.getBytes()), StandardCharsets.UTF_8);
            if (!extractedKey.equals(KEY_MARKER)) {
                Log.e(TAG, "Decryption failed: Key does not match marker");
                return null;
            }

            // Read the length of the message in bits
            encryptedStream.read(lengthBuffer);  // Message length in bits
            int messageLengthBits = ByteBuffer.wrap(lengthBuffer).getInt();
            int messageLengthBytes = (messageLengthBits + 7) / 8;

            byte[] messageBytes = new byte[messageLengthBytes];
            int messageBitIndex = 0;

            byte[] buffer = new byte[1024];
            int length;

            while ((length = encryptedStream.read(buffer)) > 0) {
                for (int i = 0; i < length; i++) {
                    if (messageBitIndex < messageLengthBits) {
                        int byteIndex = messageBitIndex / 8;
                        int bitIndex = messageBitIndex % 8;

                        int extractedBit = buffer[i] & 1;
                        messageBytes[byteIndex] = (byte) (messageBytes[byteIndex] | (extractedBit << (7 - bitIndex)));

                        messageBitIndex++;
                    }
                }
            }

            secretMessage.append(new String(messageBytes, StandardCharsets.UTF_8));
            Log.i(TAG, "Video decryption completed successfully.");
        } catch (IOException e) {
            Log.e(TAG, "Error during video decryption: " + e.getMessage(), e);
        }
        return secretMessage.toString();
    }

    // XOR encryption/decryption helper method
    private static byte[] xorWithKey(byte[] data, byte[] key) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ key[i % key.length]);
        }
        return result;
    }
}
