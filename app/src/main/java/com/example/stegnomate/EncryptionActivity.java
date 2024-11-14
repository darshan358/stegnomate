package com.example.stegnomate;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class EncryptionActivity extends AppCompatActivity {

    private EditText etSecretMessage, etSecretKey;
    private TextView tvSuccessMessage;
    private Button btnUploadFile, btnEncrypt, btnDownload;
    private Uri coverFileUri;
    private DatabaseHelper databaseHelper;
    private ActivityResultLauncher<Intent> fileChooserLauncher;

    private static final String TAG = "EncryptionActivity";
    private static final int REQUEST_MEDIA_PERMISSIONS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encryption);

        checkPermissions();

        etSecretMessage = findViewById(R.id.etSecretMessage);
        etSecretKey = findViewById(R.id.etSecretKey);
        tvSuccessMessage = findViewById(R.id.tvSuccessMessage);
        btnUploadFile = findViewById(R.id.btnUploadFile);
        btnEncrypt = findViewById(R.id.btnEncrypt);
        btnDownload = findViewById(R.id.btnDownload);

        databaseHelper = new DatabaseHelper(this);

        fileChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        coverFileUri = result.getData().getData();
                        Toast.makeText(this, "File Uploaded", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        btnUploadFile.setOnClickListener(view -> openFileChooser());
        btnEncrypt.setOnClickListener(view -> encryptFile(etSecretMessage.getText().toString(), etSecretKey.getText().toString()));
        btnDownload.setOnClickListener(view -> downloadEncryptedFile());
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_AUDIO,
                        Manifest.permission.READ_MEDIA_VIDEO
                }, REQUEST_MEDIA_PERMISSIONS);
            }
        } else {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_MEDIA_PERMISSIONS);
            }
        }
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        fileChooserLauncher.launch(intent);
    }

    private void downloadEncryptedFile() {
        new Thread(() -> {
            String encryptedFilePath = databaseHelper.getEncryptedFilePath();
            if (encryptedFilePath != null) {
                File encryptedFile = new File(encryptedFilePath);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Encrypted file downloaded: " + encryptedFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    openFile(encryptedFile);
                });
            } else {
                runOnUiThread(() -> Toast.makeText(this, "No encrypted file found", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }


    private void encryptFile(String secretMessage, String secretKey) {
        if (coverFileUri == null || secretMessage.isEmpty() || secretKey.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields and upload a file", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try (InputStream inputStream = getContentResolver().openInputStream(coverFileUri)) {
                if (inputStream == null) {
                    runOnUiThread(() -> Toast.makeText(this, "Error: Unable to open file", Toast.LENGTH_SHORT).show());
                    return;
                }

                File tempFile = createTempFile(inputStream);
                String fileType = getContentResolver().getType(coverFileUri);
                File encryptedData = processEncryption(tempFile, fileType, secretMessage, secretKey);

                // Updated: passing the correct parameters to storeEncryptedFile
                if (encryptedData != null && databaseHelper.storeEncryptedFile(encryptedData.getName(), secretMessage, encryptedData, secretKey)) {
                    runOnUiThread(() -> {
                        tvSuccessMessage.setText(R.string.encryption_successful);
                        tvSuccessMessage.setVisibility(View.VISIBLE);
                        btnDownload.setVisibility(View.VISIBLE);
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, R.string.encryption_failed, Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "Encryption Failed: ", e);
                runOnUiThread(() -> Toast.makeText(this, "Encryption Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }


    private File createTempFile(InputStream inputStream) throws IOException {
        File tempFile = File.createTempFile("cover", null, getCacheDir());
        try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }
        return tempFile;
    }

    private File processEncryption(File tempFile, String fileType, String secretMessage, String secretKey) {
        File encryptedFile = null;
        if (fileType != null) {
            if (fileType.startsWith("image/")) {
                byte[] encryptedData = ImageUtils.encryptImage(tempFile, secretMessage, secretKey);
                encryptedFile = saveToFile(encryptedData, "encrypted_image.png");
            } else if (fileType.startsWith("audio/")) {
                encryptedFile = AudioUtils.encryptAudio(tempFile, secretMessage, secretKey, getExternalFilesDir(null) + "/encrypted_audio.wav");
            } else if (fileType.startsWith("video/")) {
                encryptedFile = VideoUtils.encryptVideo(tempFile, secretMessage, secretKey, getExternalFilesDir(null) + "/encrypted_video.mp4");
            }
        }

        return encryptedFile;
    }


    private File saveToFile(byte[] data, String fileName) {
        File file = new File(getExternalFilesDir(null), fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        } catch (IOException e) {
            Log.e(TAG, "Failed to save file: " + e.getMessage(), e);
        }
        return file;
    }

    private void openFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, getContentResolver().getType(uri));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Open File"));
        } catch (Exception e) {
            Log.e(TAG, "Failed to open file: ", e);
            Toast.makeText(this, "Failed to open file.", Toast.LENGTH_SHORT).show();
        }
    }
}