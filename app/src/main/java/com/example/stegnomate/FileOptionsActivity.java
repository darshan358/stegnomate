// FileOptionsActivity.java
package com.example.stegnomate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class FileOptionsActivity extends AppCompatActivity {

    private Button encryptButton;
    private Button decryptButton;
    private String fileType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_options);

        fileType = getIntent().getStringExtra("fileType");

        encryptButton = findViewById(R.id.btnEncrypt);
        decryptButton = findViewById(R.id.btnDecrypt);

        encryptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEncryptionActivity();
            }
        });

        decryptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDecryptionActivity();
            }
        });
    }

    private void openEncryptionActivity() {
        Intent intent = new Intent(FileOptionsActivity.this, EncryptionActivity.class);
        intent.putExtra("fileType", fileType);
        startActivity(intent);
    }

    private void openDecryptionActivity() {
        Intent intent = new Intent(FileOptionsActivity.this, DecryptionActivity.class);
        intent.putExtra("fileType", fileType);
        startActivity(intent);

    }
}
