package com.demo_screen_recording.pulkit.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.demo_screen_recording.pulkit.R;
import com.demo_screen_recording.pulkit.util.AppConstant;
import com.demo_screen_recording.pulkit.util.CommonUtil;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class DirectScreenRecordingActivity extends AppCompatActivity {

    ToggleButton toggleButton;

    private static final String TAG = "DirectScreenRecording";

    private int mScreenDensity, width, height;
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private MediaProjectionCallBack mediaProjectionCallBack;
    private MediaRecorder mediaRecorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direct_screen_recording);

        findIds();
        init();

    }

    private void findIds() {

        toggleButton = (ToggleButton) findViewById(R.id.toggle);

    }

    private void init() {

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mScreenDensity = displayMetrics.densityDpi;

        //Initialize MediaRecorder class and initialize it with preferred configuration
        mediaRecorder = new MediaRecorder();
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        setListner();
    }

    private void setListner() {

        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissions();

            }
        });
    }

    private void checkPermissions() {

        if (CommonUtil.checkAndRequestPermission(DirectScreenRecordingActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                AppConstant.MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE)) {

            if (CommonUtil.checkAndRequestPermission(DirectScreenRecordingActivity.this,
                    Manifest.permission.RECORD_AUDIO,
                    AppConstant.MY_PERMISSIONS_REQUEST_RECORD_AUDIO)) {
                onToggleScreenRecord();
            }
        }
    }

    private void onToggleScreenRecord() {

        if (toggleButton.isChecked()) {
            Log.v(TAG, "Start Recording");
            initRecorder();
            recordScreen();
        } else {
            mediaRecorder.stop();
            mediaRecorder.reset();
            Log.v(TAG, "Stop Recording");
            stopScreenRecording();
        }
    }

    private void initRecorder() {

        try {
            String fileLocation = Environment.getExternalStorageDirectory() + File.separator + AppConstant.APP_DIR;
            File file = new File(fileLocation);
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && !file.isDirectory()) {
                file.mkdirs();
            }
            String saveFileName = getFileSaveName();
            String filePath = fileLocation + File.separator + saveFileName + ".mp4";

            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFile(filePath);
            mediaRecorder.setVideoSize(AppConstant.DISPLAY_WIDTH, AppConstant.DISPLAY_HEIGHT);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setVideoEncodingBitRate(512 * 1000);
            mediaRecorder.setVideoFrameRate(30);
            mediaRecorder.prepare();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private String getFileSaveName() {
        Date date = new Date();
        CharSequence sequence = DateFormat.format("yyMMdd_HHmmss", date.getTime());
        String dateFormat = sequence.toString();
        return dateFormat;
    }

    private void recordScreen() {

        if (mediaProjection == null) {
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), AppConstant.REQUEST_CODE);
            return;
        }
        /* Create a new virtual display with the actual default display
                 * and pass it on to MediaRecorder to start recording */
        virtualDisplay = createVirtualDisplay();
        mediaRecorder.start();
    }

    private void stopScreenRecording() {
        if (virtualDisplay == null) {
            return;
        } else {
            virtualDisplay.release();
            destroyMediaProjection();
        }
    }

    private void destroyMediaProjection() {

        if (mediaProjection != null) {
            mediaProjection.unregisterCallback(mediaProjectionCallBack);
            mediaProjection.stop();
            mediaProjection = null;
        }
        Log.i(TAG, "destroyMediaProjection: Media Projection Stopped");
    }

    @Override
    public void onBackPressed() {

        Log.v(TAG, "Stop Recording");
        stopScreenRecording();
        super.onBackPressed();
    }

//    @Override
//    protected void onPause() {
//
//        mediaRecorder.stop();
//        mediaRecorder.reset();
//        Log.v(TAG, "Stop Recording");
//        stopScreenRecording();
//        super.onPause();
//    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        destroyMediaProjection();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode != AppConstant.REQUEST_CODE) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            return;
        }
        if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Screen Recording Permission Denied", Toast.LENGTH_SHORT).show();
            toggleButton.setChecked(false);
            return;
        }

        //Set Callback for MediaProjection
        mediaProjectionCallBack = new MediaProjectionCallBack();

        //Initialize MediaProjection using data received from Intent
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);

        //Register MediaProjection
        mediaProjection.registerCallback(mediaProjectionCallBack, null);

        /* Create a new virtual display with the actual default display
                 * and pass it on to MediaRecorder to start recording */
        virtualDisplay = createVirtualDisplay();

        //Start Recorder
        mediaRecorder.start();
    }

    //Virtual display created by mirroring the actual physical display
    private VirtualDisplay createVirtualDisplay() {

        return mediaProjection.createVirtualDisplay(TAG, AppConstant.DISPLAY_WIDTH, AppConstant.DISPLAY_HEIGHT, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder.getSurface(), null /*Callbacks*/, null /*Handler*/);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {

            case AppConstant.MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    checkPermissions();
                } else {
                    CommonUtil.checkAndRequestPermission(DirectScreenRecordingActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            AppConstant.MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                }
                break;

            case AppConstant.MY_PERMISSIONS_REQUEST_RECORD_AUDIO:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    checkPermissions();
                } else {
                    CommonUtil.checkAndRequestPermission(DirectScreenRecordingActivity.this, Manifest.permission.RECORD_AUDIO,
                            AppConstant.MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
                }
        }
    }

    private class MediaProjectionCallBack extends MediaProjection.Callback {

        @Override
        public void onStop() {

            if (toggleButton.isChecked()) {
                toggleButton.setChecked(false);
                mediaRecorder.stop();
                mediaRecorder.reset();
                Log.v(TAG, "Recording Stopped");
            }
            mediaProjection = null;
            stopScreenRecording();
        }
    }

}
