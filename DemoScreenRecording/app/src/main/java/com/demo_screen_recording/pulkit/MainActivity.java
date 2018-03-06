package com.demo_screen_recording.pulkit;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.demo_screen_recording.pulkit.activities.DirectScreenRecordingActivity;
import com.demo_screen_recording.pulkit.activities.ScreenRecordingActivity;

public class MainActivity extends AppCompatActivity {

    private Button btn_direct_screen_recording, btn_screen_recording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findIds();
        init();

    }

    private void findIds() {

        btn_direct_screen_recording = (Button) findViewById(R.id.btn_direct_screen_recording);
        btn_screen_recording = (Button) findViewById(R.id.btn_screen_recording);

    }

    private void init() {

        /*Without Services*/
        btn_direct_screen_recording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), DirectScreenRecordingActivity.class);
                startActivity(intent);
            }
        });

        /*With Services*/
        btn_screen_recording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ScreenRecordingActivity.class);
                startActivity(intent);
            }
        });

    }

}
