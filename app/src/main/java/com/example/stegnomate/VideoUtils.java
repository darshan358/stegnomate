package com.example.stegnomate;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class VideoUtils {

    private static final String TAG = "VideoUtils";

    // Encrypts the secret message into the video file
    public static File encryptVideo(File inputFile, String secretMessage, String secretKey, String outputFilePath) {
        File encryptedFile = new File(outputFilePath);
        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(encryptedFile)) {

            byte[] buffer = new byte[1024];
            int length;

            // Convert secret message to bytes and determine message length in bits
            byte[] secretMessageBytes = secretMessage.getBytes();
            int messageLengthBits = secretMessageBytes.length * 8;

            // Write message length (in bits) to the beginning of the file
            fos.write(ByteBuffer.allocate(4).putInt(messageLengthBits).array());

            int messageBitIndex = 0;  // Track which bit of the message we're encoding
            while ((length = fis.read(buffer)) > 0) {
                for (int i = 0; i < length; i++) {
                    if (messageBitIndex < messageLengthBits) {
                        // Get the byte and bit position within the secret message
                        int byteIndex = messageBitIndex / 8;
                        int bitIndex = messageBitIndex % 8;

                        // Extract the current bit from the secret message byte
                        int secretBit = (secretMessageBytes[byteIndex] >> (7 - bitIndex)) & 1;

                        // Modify the LSB of the current byte in the video data
                        buffer[i] = (byte) ((buffer[i] & 0xFE) | secretBit);

                        // Move to the next bit of the message
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
            byte[] buffer = new byte[1024];
            int length;

            // Read the message length from the header (first 4 bytes)
            byte[] lengthBuffer = new byte[4];
            encryptedStream.read(lengthBuffer);
            int messageLengthBits = ByteBuffer.wrap(lengthBuffer).getInt();
            int messageLengthBytes = (messageLengthBits + 7) / 8;  // Round up to full bytes

            byte[] messageBytes = new byte[messageLengthBytes];
            int messageBitIndex = 0;

            while ((length = encryptedStream.read(buffer)) > 0) {
                for (int i = 0; i < length; i++) {
                    if (messageBitIndex < messageLengthBits) {
                        // Determine byte and bit index in messageBytes
                        int byteIndex = messageBitIndex / 8;
                        int bitIndex = messageBitIndex % 8;

                        // Extract the LSB of the current byte in video data
                        int extractedBit = buffer[i] & 1;

                        // Set the extracted bit in the message byte
                        messageBytes[byteIndex] = (byte) (messageBytes[byteIndex] | (extractedBit << (7 - bitIndex)));

                        // Move to the next bit of the message
                        messageBitIndex++;
                    }
                }
            }

            // Convert the message bytes to a string
            secretMessage.append(new String(messageBytes));
            Log.i(TAG, "Video decryption completed successfully.");
        } catch (IOException e) {
            Log.e(TAG, "Error during video decryption: " + e.getMessage(), e);
        }
        return secretMessage.toString();
    }

    // Helper method for XOR encryption of data
    private static byte[] encryptData(byte[] data, String secretMessage, String secretKey) {
        byte[] encryptedData = new byte[data.length];
        int keyLength = secretKey.length();
        byte[] secretMessageBytes = secretMessage.getBytes();
        int messageIndex = 0;

        for (int i = 0; i < data.length; i++) {
            encryptedData[i] = (byte) (data[i] ^ secretKey.charAt(i % keyLength));
            if (messageIndex < secretMessageBytes.length) {
                encryptedData[i] = (byte) ((encryptedData[i] & 0xFE) | (secretMessageBytes[messageIndex] & 0x01));
                messageIndex++;
            }
        }
        return encryptedData;
    }

    // Helper method for XOR decryption of data
    private static byte[] decryptData(byte[] data, String secretKey) {
        byte[] decryptedData = new byte[data.length];
        int keyLength = secretKey.length();

        for (int i = 0; i < data.length; i++) {
            decryptedData[i] = (byte) (data[i] ^ secretKey.charAt(i % keyLength));
        }

        return decryptedData;
    }
}
