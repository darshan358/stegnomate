package com.example.stegnomate;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

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

    // Encrypt message into an audio file and save
    public static File encryptAudio(File coverFile, String secretMessage, String secretKey, String outputPath) {
        try {
            byte[] audioData = readFileToByteArray(coverFile);
            String encryptedMessage = encryptAudioMessage(secretMessage, secretKey); // Use audio-specific encryption
            byte[] encryptedAudioData = embedMessageInAudio(audioData, encryptedMessage);

            // Save encrypted audio data to file
            File encryptedAudioFile = new File(outputPath);
            try (FileOutputStream fos = new FileOutputStream(encryptedAudioFile)) {
                fos.write(encryptedAudioData);
            }

            return encryptedAudioFile;  // Return the file object for further operations
        } catch (Exception e) {
            Log.e(TAG, "Audio Encryption Failed: " + e.getMessage(), e);
            return null;
        }
    }

/*
    // Encrypt message into a video file and save
    public static File encryptVideo(InputStream coverStream, String secretMessage, String secretKey, String outputPath) {
        try {
            // Read the video data from InputStream
            byte[] videoData = readInputStreamToByteArrays(coverStream);

            // Encrypt the message and embed it into the video data
            String encryptedMessage = encryptMessageForVideo(secretMessage, secretKey);
            byte[] encryptedVideoData = embedMessageInVideo(videoData, encryptedMessage);

            // Save the encrypted video data to the output file
            File encryptedVideoFile = new File(outputPath);
            try (FileOutputStream fos = new FileOutputStream(encryptedVideoFile)) {
                fos.write(encryptedVideoData);
            }

            return encryptedVideoFile;
        } catch (Exception e) {
            Log.e(TAG, "Video Encryption Failed: " + e.getMessage(), e);
            return null;
        }
    }
*/

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

    public static String decryptAudio(InputStream inputStream, String secretKey) {
        try {
            byte[] audioData = readInputStreamToByteArray(inputStream);
            String hiddenMessage = extractMessageFromAudio(audioData);
            return decryptAudioMessage(hiddenMessage, secretKey); // Call updated method
        } catch (Exception e) {
            Log.e(TAG, "Audio Decryption Failed: " + e.getMessage(), e);
            return null;
        }
    }



/*
    // Decrypt message from a video file
    public static String decryptVideo(InputStream videoStream, String secretKey) {
        try {
            // Read the video data from InputStream into byte[]
            byte[] videoData = readInputStreamToByteArray(videoStream);

            // Extract and decrypt the hidden message from the video data
            byte[] hiddenMessage = extractMessageFromVideo(videoData);
            if (hiddenMessage == null) {
                Log.e(TAG, "No hidden message found in video.");
                return null;
            }

            // Decrypt the extracted message
            return decryptMessageFromVideo(hiddenMessage, secretKey);
        } catch (Exception e) {
            Log.e(TAG, "Video Decryption Failed: " + e.getMessage(), e);
            return null;
        }
    }
*/

    // Placeholder for encrypting a message (replace with actual AES encryption if needed)
    private static String encryptMessage(String message, String key) {
        // Convert to binary string (simulate encryption for demonstration)
        StringBuilder binary = new StringBuilder();
        for (char c : message.toCharArray()) {
            binary.append(String.format("%8s", Integer.toBinaryString(c)).replace(' ', '0'));
        }
        return binary.toString();
    }

    @SuppressLint("DefaultLocale")
    private static String encryptAudioMessage(String secretMessage, String secretKey) {
        StringBuilder binaryMessage = new StringBuilder();

        // Convert each character of the message to an 8-bit binary representation
        for (char c : secretMessage.toCharArray()) {
            binaryMessage.append(String.format("%08d", Integer.parseInt(Integer.toBinaryString(c))));
        }

        // Prepend the length of the message (in bytes) to help with accurate extraction
        int messageLength = secretMessage.length();
        String lengthPrefix = String.format("%08d", Integer.parseInt(Integer.toBinaryString(messageLength)));

        String completeBinaryMessage = lengthPrefix + binaryMessage.toString();

        // Encrypt the binary message if encryption is needed
        String encryptedBinaryMessage = encryptMessage(completeBinaryMessage, secretKey);

        return encryptedBinaryMessage != null ? encryptedBinaryMessage : "";
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

    // Placeholder for decrypting a message from audio (replace with actual AES decryption if needed)
    private static String decryptAudioMessage(String binaryMessage, String key) {
        StringBuilder message = new StringBuilder();

        // Assuming the binary message starts with a length prefix (in bits)
        if (binaryMessage.length() > 0) {
            // Example: The first 8 bits represent the length of the actual message
            int messageLength = Integer.parseInt(binaryMessage.substring(0, 8), 2);

            // Extract only the relevant part of the binary message based on the length
            if (binaryMessage.length() > 8) {
                String relevantBinary = binaryMessage.substring(8, 8 + messageLength * 8);
                // Convert from binary string to text
                for (int i = 0; i < relevantBinary.length(); i += 8) {
                    // Ensure we don't exceed the bounds of the relevant binary string
                    if (i + 8 <= relevantBinary.length()) {
                        String byteStr = relevantBinary.substring(i, i + 8);
                        message.append((char) Integer.parseInt(byteStr, 2));
                    }
                }
            }
        }

        return message.toString().trim(); // Trim to remove any unwanted whitespace
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

    // Read file into byte array
    private static byte[] readFileToByteArray(File file) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        return data;
    }

    // Read input stream into byte array
    private static byte[] readInputStreamToByteArray(InputStream inputStream) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, length);
        }
        return byteArrayOutputStream.toByteArray();
    }

    // Convert Bitmap to byte array
    private static byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    // Embed message into audio data using LSB
    private static byte[] embedMessageInAudio(byte[] audioData, String message) {
        byte[] messageBytes = message.getBytes();
        for (int i = 0; i < messageBytes.length && i < audioData.length; i++) {
            audioData[i] = (byte)((audioData[i] & 0xFE) | (messageBytes[i] & 1));
        }
        return audioData;
    }


    private static String extractMessageFromAudio(byte[] audioData) {
        StringBuilder message = new StringBuilder();
        for (byte b : audioData) {
            message.append((b & 1) == 1 ? '1' : '0');
        }
        // Implement a termination marker check here, if necessary
        return message.toString();
    }

    //----------------------------SOme Video Methods---------------------------------------------------

    // Encrypt message into a video file and save
    public static File encryptVideo(InputStream coverFileInputStream, String secretMessage, String secretKey, String outputPath) {
        try {
            // Read the video stream into a buffer
            byte[] videoData = inputStreamToByteArray(coverFileInputStream);

            // Encrypt the message using AES with CBC mode (more secure than ECB)
            byte[] encryptedMessage = encryptMessageForVideo(secretMessage, secretKey);

            // Embed the encrypted message into the video
            byte[] encryptedVideoData = embedMessageInVideo(videoData, encryptedMessage);

            // Write the final encrypted video data to file
            File encryptedVideoFile = new File(outputPath);
            try (FileOutputStream fos = new FileOutputStream(encryptedVideoFile)) {
                fos.write(encryptedVideoData);
            }

            return encryptedVideoFile;  // Return the encrypted video file for further operations
        } catch (Exception e) {
            Log.e(TAG, "Video Encryption Failed: " + e.getMessage(), e);
            return null;
        }
    }

    // Encrypt the message for video
    private static byte[] encryptMessageForVideo(String message, String secretKey) {
        try {
            // Generate a secure random IV
            SecureRandom secureRandom = new SecureRandom();
            byte[] iv = new byte[16]; // AES block size is 16 bytes
            secureRandom.nextBytes(iv);

            // AES encryption with CBC mode
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(iv));

            byte[] encryptedMessageBytes = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));

            // Combine IV and encrypted message for storage
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byteArrayOutputStream.write(iv);  // Prepend the IV
            byteArrayOutputStream.write(encryptedMessageBytes);  // Append the encrypted message
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            Log.e(TAG, "Message Encryption Failed: " + e.getMessage(), e);
            return null;
        }
    }

    // Decrypt message from a video file
    public static String decryptVideo(InputStream videoFileInputStream, String secretKey) {
        try {
            // Read the video stream into a buffer
            byte[] videoData = inputStreamToByteArray(videoFileInputStream);

            // Extract the embedded message from the video data
            byte[] extractedMessage = extractMessageFromVideo(videoData);

            if (extractedMessage == null) {
                Log.e(TAG, "No hidden message found in video.");
                return null;
            }

            // Decrypt the extracted message
            return decryptMessageFromVideo(extractedMessage, secretKey);
        } catch (Exception e) {
            Log.e(TAG, "Video Decryption Failed: " + e.getMessage(), e);
            return null;
        }
    }

    // Decrypt the message extracted from the video
    private static String decryptMessageFromVideo(byte[] encryptedMessageBytes, String secretKey) {
        try {
            // Extract the IV from the first 16 bytes
            byte[] iv = Arrays.copyOfRange(encryptedMessageBytes, 0, 16);
            byte[] encryptedMessage = Arrays.copyOfRange(encryptedMessageBytes, 16, encryptedMessageBytes.length);

            // AES decryption with CBC mode
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(iv));

            byte[] decryptedMessageBytes = cipher.doFinal(encryptedMessage);
            return new String(decryptedMessageBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "Message Decryption Failed: " + e.getMessage(), e);
            return null;
        }
    }

    // Helper method to read an InputStream into a byte array
    private static byte[] inputStreamToByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }
        return byteArrayOutputStream.toByteArray();
    }

    // Embed the message into video data
    private static byte[] embedMessageInVideo(byte[] videoData, byte[] message) {
        // We simply append the message at the end of the video file
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            byteArrayOutputStream.write(videoData);
            byteArrayOutputStream.write(message);  // Embed the message at the end
        } catch (IOException e) {
            Log.e(TAG, "Error embedding message in video: " + e.getMessage(), e);
        }
        return byteArrayOutputStream.toByteArray();
    }

    // Extract the embedded message from video data
    private static byte[] extractMessageFromVideo(byte[] videoData) {
        // Extract the message by reading the last bytes of the video data
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(videoData);
            byteArrayInputStream.skip(videoData.length - 16);  // Skip to the message position

            byte[] messageBytes = new byte[16];  // Assuming the message is of fixed length (adjust accordingly)
            byteArrayInputStream.read(messageBytes);
            return messageBytes;
        } catch (IOException e) {
            Log.e(TAG, "Error extracting message from video: " + e.getMessage(), e);
            return null;
        }
    }

}
