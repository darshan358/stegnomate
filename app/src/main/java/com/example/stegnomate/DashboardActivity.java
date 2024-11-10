// DashboardActivity.java
package com.example.stegnomate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class DashboardActivity extends AppCompatActivity {

    private Button audioButton;
    private Button videoButton;
    private Button imageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        audioButton = findViewById(R.id.btnAudio);
        videoButton = findViewById(R.id.btnVideo);
        imageButton = findViewById(R.id.btnImage);

        audioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileOptions("audio");
            }
        });

        videoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileOptions("video");
            }
        });

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileOptions("image");
            }
        });
    }

    private void openFileOptions(String fileType) {
        Intent intent = new Intent(DashboardActivity.this, FileOptionsActivity.class);
        intent.putExtra("fileType", fileType);
        startActivity(intent);
    }
}
