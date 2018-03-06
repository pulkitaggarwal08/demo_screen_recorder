package com.demo_screen_recording.pulkit.services;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.FileProvider;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import com.demo_screen_recording.pulkit.R;
import com.demo_screen_recording.pulkit.activities.ScreenRecordingActivity;
import com.demo_screen_recording.pulkit.util.AppConstant;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by pulkit on 18/12/17.
 */

public class RecorderService extends Service {

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private static int WIDTH, HEIGHT, FPS, DENSITY_DPI;
    private static int BITRATE;
    private static boolean mustRecAudio;
    private static String SAVEPATH;
    private boolean isRecording;
    private Intent data;
    private int result;
    private long startTime, elapsedTime = 0;

    private static final String TAG = "RecorderService";
    private WindowManager window;

    private int mScreenDensity;
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private MediaProjectionCallback mediaProjectionCallBack;
    private MediaRecorder mediaRecorder;
    private Surface surface;
    private NotificationManager mNotificationManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        switch (intent.getAction()) {
            case AppConstant.SCREEN_RECORDING_START:

                if (!isRecording) {
                    getValues();

                    data = intent.getParcelableExtra(AppConstant.RECORDER_INTENT_DATA);
                    result = intent.getIntExtra(AppConstant.RECORDER_INTENT_RESULT, Activity.RESULT_OK);

                    startRecording();

                    /*Show recording in Notification bar */
                    startNotificationForeGround(createRecordingNotification(null).build(), AppConstant.SCREEN_RECORDER_NOTIFICATION_ID);
                }
                break;
            case AppConstant.SCREEN_RECORDING_PAUSE:
                pauseScreenRecording();
                break;
            case AppConstant.SCREEN_RECORDING_RESUME:
                resumeScreenRecording();
                break;
            case AppConstant.SCREEN_RECORDING_STOP:

                stopScreenRecording();
                break;
        }
        return START_STICKY;
    }

    //Start service as a foreground service. We dont want the service to be killed in case of low memory
    private void startNotificationForeGround(Notification notification, int ID) {
        startForeground(ID, notification);
    }

    private void initRecorder() {

        try {
            if (mustRecAudio) {
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mediaRecorder.setOutputFile(SAVEPATH);
                mediaRecorder.setVideoSize(WIDTH, HEIGHT);
                mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                mediaRecorder.setVideoEncodingBitRate(BITRATE);
                mediaRecorder.setVideoFrameRate(FPS);

                int rotation = window.getDefaultDisplay().getRotation();
                int orientation = ORIENTATIONS.get(rotation + 90);

                mediaRecorder.setOrientationHint(orientation);
                mediaRecorder.prepare();

                //To set the surface for video
                surface = mediaRecorder.getSurface();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //Virtual display created by mirroring the actual physical display
    private VirtualDisplay createVirtualDisplay() {
        return mediaProjection.createVirtualDisplay(TAG, WIDTH, HEIGHT, DENSITY_DPI,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface, null /*Callbacks*/, null /*Handler*/);
    }

    private void startRecording() {

        //Initialize MediaRecorder class and initialize it with preferred configuration
        mediaRecorder = new MediaRecorder();
        initRecorder();

        //Set Callback for MediaProjection
        mediaProjectionCallBack = new MediaProjectionCallback();
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        //Initialize MediaProjection using data received from Intent
        mediaProjection = mediaProjectionManager.getMediaProjection(result, data);

        //Register MediaProjection
        mediaProjection.registerCallback(mediaProjectionCallBack, null);

        /* Create a new virtual display with the actual default display
                 * and pass it on to MediaRecorder to start recording */
        virtualDisplay = createVirtualDisplay();

        //Start Recorder
        mediaRecorder.start();
        Toast.makeText(this, "Rec start", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onDestroy() {

        if (mediaProjection != null) {
            Log.v(TAG, "Recording Stopped");

            mediaProjection.unregisterCallback(mediaProjectionCallBack);
            mediaProjection.stop();
            mediaProjection = null;
        } else {
            super.onDestroy();
        }
    }

    private void getValues() {
        String screenSize = getResolution();
        setWidthHeight(screenSize);

        FPS = 30;
        BITRATE = 7130317;

        //To use for audio disable/enable
        mustRecAudio = true;

        String fileLocation = Environment.getExternalStorageDirectory() + File.separator + AppConstant.APP_DIR;
        File file = new File(fileLocation);
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && !file.isDirectory()) {
            file.mkdirs();
        }
        String saveFileName = getFileSaveName();
        SAVEPATH = fileLocation + File.separator + saveFileName + ".mp4";

//        useFloatingControls = false;
//        showTouches = false;

    }

    //Get the device resolution in pixels
    private String getResolution() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        window = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        window.getDefaultDisplay().getMetrics(displayMetrics);

        DENSITY_DPI = displayMetrics.densityDpi;
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
        return width + "x" + height;
    }

    private void setWidthHeight(String screenSize) {

        String[] widthHeight = screenSize.split("x");
        WIDTH = Integer.parseInt(widthHeight[0]);
        HEIGHT = Integer.parseInt(widthHeight[1]);
    }

    private String getFileSaveName() {
        Date date = new Date();
        CharSequence sequence = DateFormat.format("yyMMdd_HHmmss", date.getTime());
        String dateFormat = sequence.toString();
        return dateFormat;
    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            Log.v(TAG, "Recording Stopped");
            stopScreenRecording();
        }
    }

    private void stopScreenRecording() {
        if (virtualDisplay == null) {
            return;
        } else {
            destroyMediaProjection();
        }
    }

    private void destroyMediaProjection() {

        try {
            mediaRecorder.stop();
            indexFile();
            Log.i(TAG, "MediaProjection Stopped");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Fatal exception! Destroying media projection failed." + "\n" + e.getMessage());
            if (new File(SAVEPATH).delete()) {
                Log.d(TAG, "Corrupted file delete successful");
            }
        } finally {
            mediaRecorder.reset();
            virtualDisplay.release();
            mediaRecorder.release();

            if (mediaProjection != null) {
                mediaProjection.unregisterCallback(mediaProjectionCallBack);
                mediaProjection.stop();
                mediaProjection = null;
            }
        }
        isRecording = false;
    }

    /* Its weird that android does not index the files immediately once its created and that causes
     * trouble for user in finding the video in gallery. Let's explicitly announce the file creation
     * to android and index it */
    private void indexFile() {
        //Create a new ArrayList and add the newly created video file path to it
        ArrayList<String> toBeScanned = new ArrayList<>();
        toBeScanned.add(SAVEPATH);
        String[] toBeScannedStr = new String[toBeScanned.size()];
        toBeScannedStr = toBeScanned.toArray(toBeScannedStr);

        //Request MediaScannerConnection to scan the new file and index it
        MediaScannerConnection.scanFile(this, toBeScannedStr, null, new MediaScannerConnection.OnScanCompletedListener() {

            @Override
            public void onScanCompleted(String path, Uri uri) {
                Log.i(TAG, "SCAN COMPLETED: " + path);
                //Show toast on main thread
                Message message = mHandler.obtainMessage();
                message.sendToTarget();
                stopSelf();
            }
        });
    }

    Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            Toast.makeText(RecorderService.this, R.string.screen_recording_stopped_toast, Toast.LENGTH_SHORT).show();

            /*For Notificaiton*/
            stopRecordNotification();
        }
    };

    /*After stopping the video, show the notification in notification area*/
    private void stopRecordNotification() {

        //To set the icon
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

        Uri uri = FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName() + ".provider", new File(SAVEPATH));

        //To share the video
        Intent shareIntent = new Intent()
                .setAction(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_STREAM, uri)
                .setType("video/mp4");
        PendingIntent sharePendingIntent = PendingIntent.getActivity(this, 0,
                Intent.createChooser(shareIntent, "Share Recorded"), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder shareNotification = new NotificationCompat.Builder(this)
                .setContentTitle("Share Recorded Video")
                .setContentText("Click to share the recorded Video")
                .setSmallIcon(R.mipmap.ic_notification)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setAutoCancel(true)
                .setContentIntent(sharePendingIntent)
                .addAction(android.R.drawable.ic_menu_share, getString(R.string.share_intent), sharePendingIntent);

        updateNotification(shareNotification.build(), AppConstant.SCREEN_RECORDER_SHARE_NOTIFICATION_ID);
    }

    /*To pause the Screen Recording*/
    private void pauseScreenRecording() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaRecorder.pause();
        }
        //calculate total elapsed time until pause
        elapsedTime = elapsedTime + (System.currentTimeMillis() - startTime);

        //Set Resume action to Notification and update the current notification
        Intent recordResumeIntent = new Intent(this, RecorderService.class);
        recordResumeIntent.setAction(AppConstant.SCREEN_RECORDING_RESUME);

        PendingIntent precordResumeIntent = PendingIntent.getService(this, 0, recordResumeIntent, 0);
        NotificationCompat.Action action = new
                NotificationCompat.Action(android.R.drawable.ic_media_play, getString(R.string.resume), precordResumeIntent);

        updateNotification(createRecordingNotification(action)
                .setUsesChronometer(false)
                .build(), AppConstant.SCREEN_RECORDER_NOTIFICATION_ID);

        Toast.makeText(this, R.string.pause, Toast.LENGTH_SHORT).show();

    }

    private void resumeScreenRecording() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaRecorder.resume();
        }

        //Reset startTime to current time again
        startTime = System.currentTimeMillis();

        //set Pause action to Notification and update current Notification
        Intent recordPauseIntent = new Intent(this, RecorderService.class);
        recordPauseIntent.setAction(AppConstant.SCREEN_RECORDING_PAUSE);

        PendingIntent precordPauseIntent = PendingIntent.getService(this, 0, recordPauseIntent, 0);
        NotificationCompat.Action action = new NotificationCompat.Action(android.R.drawable.ic_media_pause,
                getString(R.string.pause), precordPauseIntent);

        updateNotification(createRecordingNotification(action)
                .setUsesChronometer(true)
                .setWhen((System.currentTimeMillis() - elapsedTime))
                .build(), AppConstant.SCREEN_RECORDER_NOTIFICATION_ID);

        Toast.makeText(this, R.string.resume, Toast.LENGTH_SHORT).show();

    }

    //Update existing notification with its ID and new Notification data
    private void updateNotification(Notification notification, int ID) {
        getManager().notify(ID, notification);
    }

    private NotificationManager getManager() {
        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return mNotificationManager;
    }

    /* Create Notification.Builder with action passed in case user's android version is greater than API-24 */
    private NotificationCompat.Builder createRecordingNotification(NotificationCompat.Action action) {

        //To set the icon
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

        Intent stopIntent = new Intent(this, RecorderService.class);
        stopIntent.setAction(AppConstant.SCREEN_RECORDING_STOP);
        PendingIntent stoppendingIntent = PendingIntent.getService(this, 0, stopIntent, 0);

        //To pause the video
        Intent pauseIntent = new Intent(this, RecorderService.class);
        pauseIntent.setAction(AppConstant.SCREEN_RECORDING_PAUSE);
        PendingIntent pausePendingIntent = PendingIntent.getService(this, 0, pauseIntent, 0);

        Intent UIIntent = new Intent(this, ScreenRecordingActivity.class);
        PendingIntent notificationContentIntent = PendingIntent.getActivity(this, 0, UIIntent, 0);

        NotificationCompat.Builder builder = new
                NotificationCompat.Builder(this)
                .setContentTitle("Screen recording")
                .setTicker("Screen recording")
                .setSmallIcon(R.mipmap.ic_notification)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setUsesChronometer(true)
                .setOngoing(true)
//                .setContentIntent(notificationContentIntent)
                .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
                .addAction(android.R.drawable.ic_media_pause, getString(R.string.pause_intent), pausePendingIntent)
                .addAction(R.mipmap.ic_notification_stop, getResources().getString(R.string.stop), stoppendingIntent);

        if (action != null) {
            builder.addAction(action);
        }
        return builder;
    }


}









