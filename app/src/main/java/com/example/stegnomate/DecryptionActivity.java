package com.example.stegnomate;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;

public class DecryptionActivity extends AppCompatActivity {

    private EditText etSecretKey;
    private TextView tvSecretMessage, tvSuccessMessage;
    private Button btnUploadFile, btnDecrypt;
    private Uri encryptedFileUri;

    private ActivityResultLauncher<Intent> fileChooserLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decryption);

        etSecretKey = findViewById(R.id.etSecretKey);
        tvSecretMessage = findViewById(R.id.tvExtractedMessage);
        tvSuccessMessage = findViewById(R.id.tvSuccessMessage);
        btnUploadFile = findViewById(R.id.btnUploadFile);
        btnDecrypt = findViewById(R.id.btnDecrypt);

        // Initialize the file chooser launcher
        fileChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        encryptedFileUri = result.getData().getData();
                        Toast.makeText(this, "File Uploaded", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Handle file upload
        btnUploadFile.setOnClickListener(view -> openFileChooser());

        // Handle decryption
        btnDecrypt.setOnClickListener(view -> {
            String secretKey = etSecretKey.getText().toString();

            if (encryptedFileUri != null && !secretKey.isEmpty()) {
                decryptFile(secretKey);
            } else {
                Toast.makeText(this, "Please fill in the secret key and upload a file", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        fileChooserLauncher.launch(intent);
    }

    private void decryptFile(String secretKey) {
        new Thread(() -> {
            InputStream inputStream = null;
            String secretMessage = null; // Declare it here
            try {
                inputStream = getContentResolver().openInputStream(encryptedFileUri);
                if (inputStream == null) {
                    runOnUiThread(() -> Toast.makeText(this, "Error: Unable to open the selected file.", Toast.LENGTH_LONG).show());
                    return;
                }

                // Perform decryption based on the file type
                String fileType = getContentResolver().getType(encryptedFileUri);
                if (fileType != null) {
                    if (fileType.startsWith("image/")) {
                        secretMessage = ImageUtils.decryptImage(inputStream, secretKey);
                    } else if (fileType.startsWith("audio/")) {
                        secretMessage = AudioUtils.decryptAudio(inputStream, secretKey);
                    } else if (fileType.startsWith("video/")) {
                        secretMessage = VideoUtils.decryptVideo(inputStream, secretKey);
                    }
                }

                // Show success message with the extracted secret message
                if (secretMessage != null) {
                    final String extractedMessage = secretMessage; // Make it final
                    runOnUiThread(() -> {
                        tvSuccessMessage.setText(R.string.decryption_successful);
                        tvSuccessMessage.setVisibility(View.VISIBLE);
                        tvSecretMessage.setText(getString(R.string.secret_message, extractedMessage));
                        tvSecretMessage.setVisibility(View.VISIBLE);
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, R.string.decryption_failed, Toast.LENGTH_LONG).show());
                }

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Decryption Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
