package com.demo_screen_recording.pulkit.activities;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.demo_screen_recording.pulkit.R;
import com.demo_screen_recording.pulkit.services.RecorderService;
import com.demo_screen_recording.pulkit.util.AppConstant;
import com.demo_screen_recording.pulkit.util.CommonUtil;

public class ScreenRecordingActivity extends AppCompatActivity {

    private static final String TAG = "ScreenRecordingActivity";

    private FloatingActionButton fab_screen_recorder;

    private MediaProjection mMediaProjection;
    private MediaProjectionManager mProjectionManager;

    private Intent recorderService;
    private Animation loadAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_recording);

        findIds();
        init();

    }

    private void findIds() {

        fab_screen_recorder = (FloatingActionButton) findViewById(R.id.fab_screen_recorder);
    }

    private void init() {

        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);


        fab_screen_recorder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissions();
            }
        });

        fab_screen_recorder.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                Toast.makeText(ScreenRecordingActivity.this, "Recording Stopped", Toast.LENGTH_SHORT).show();
                stopServices();

                return true;
            }
        });
    }

    private void checkPermissions() {

        if (CommonUtil.checkAndRequestPermission(ScreenRecordingActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                AppConstant.MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE)) {

            if (CommonUtil.checkAndRequestPermission(ScreenRecordingActivity.this,
                    Manifest.permission.RECORD_AUDIO,
                    AppConstant.MY_PERMISSIONS_REQUEST_RECORD_AUDIO)) {
                recordScreen();
            }
        }

    }

    private void recordScreen() {

        if (isServiceRunning(RecorderService.class)) {
            Log.d(TAG, "service is running");
        }

        if (mMediaProjection == null && !isServiceRunning(RecorderService.class)) {
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), AppConstant.SCREEN_RECORD_REQUEST_CODE);
        } else if (isServiceRunning(RecorderService.class)) {
            Toast.makeText(ScreenRecordingActivity.this, "Screen already recording", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isServiceRunning(Class<?> serviceClass) {

        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_CANCELED && requestCode == AppConstant.SCREEN_RECORD_REQUEST_CODE) {
            Toast.makeText(this, "Screen Recording Permission Denied", Toast.LENGTH_SHORT).show();
        } else {
            fab_screen_recorder.setImageResource(R.drawable.ic_videocam);

            loadAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blinking_animation);
            // Start the animation (looped playback by default).
            fab_screen_recorder.startAnimation(loadAnimation);

            recorderService = new Intent(this, RecorderService.class);
            recorderService.setAction(AppConstant.SCREEN_RECORDING_START);
            recorderService.putExtra(AppConstant.RECORDER_INTENT_DATA, data);
            recorderService.putExtra(AppConstant.RECORDER_INTENT_RESULT, resultCode);
            startService(recorderService);
//            this.finish();
        }
    }

    protected void stopServices() {

        fab_screen_recorder.setImageResource(R.drawable.ic_videocam_off);

        // Stop the animation.
        fab_screen_recorder.clearAnimation();
        fab_screen_recorder.setAlpha(1.0f);

        stopService(recorderService);
    }

    @Override
    protected void onDestroy() {

        fab_screen_recorder.setImageResource(R.drawable.ic_videocam_off);

        // Stop the animation.
        fab_screen_recorder.clearAnimation();
        fab_screen_recorder.setAlpha(1.0f);

        stopService(recorderService);

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {

            case AppConstant.MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    checkPermissions();
                } else {
                    CommonUtil.checkAndRequestPermission(ScreenRecordingActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            AppConstant.MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                }
                break;

            case AppConstant.MY_PERMISSIONS_REQUEST_RECORD_AUDIO:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    checkPermissions();
                } else {
                    CommonUtil.checkAndRequestPermission(ScreenRecordingActivity.this, Manifest.permission.RECORD_AUDIO,
                            AppConstant.MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
                }
        }
    }


}
