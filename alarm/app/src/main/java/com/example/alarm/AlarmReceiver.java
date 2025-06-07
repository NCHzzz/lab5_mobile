package com.example.alarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";
    public static final String EXTRA_ALARM_ID = "ALARM_ID";
    private static MediaPlayer mediaPlayer;
    private static PowerManager.WakeLock wakeLock;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm triggered!");

        // Acquire wake lock to keep device awake
        acquireWakeLock(context);

        long alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1);
        Log.d(TAG, "Alarm ID: " + alarmId);

        // Create notification channel for newer Android versions
        createNotificationChannel(context);

        // Play the alarm sound
        playAlarmSound(context);

        // Start AlarmActivity
        Intent alarmIntent = new Intent(context, AlarmActivity.class);
        alarmIntent.putExtra(EXTRA_ALARM_ID, alarmId);
        alarmIntent.putExtra("MEDIA_PLAYER", true);
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(alarmIntent);
    }

    private void acquireWakeLock(Context context) {
        if (wakeLock != null && wakeLock.isHeld()) {
            return;
        }

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.ON_AFTER_RELEASE,
                "AlarmApp:AlarmWakeLock");
        wakeLock.acquire(10*60*1000L); // 10 minutes
        Log.d(TAG, "WakeLock acquired");
    }

    private void playAlarmSound(Context context) {
        // Clean up any existing player
        stopSound();

        try {
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmSound == null) {
                alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(context, alarmSound);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes attributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                mediaPlayer.setAudioAttributes(attributes);
            } else {
                mediaPlayer.setAudioStreamType(android.media.AudioManager.STREAM_ALARM);
            }

            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
            Log.d(TAG, "MediaPlayer started successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error playing alarm sound", e);
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannel channel = new NotificationChannel(
                    "alarm_channel",
                    "Alarm Notifications",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{1000, 1000, 1000, 1000, 1000});
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static void stopSound() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
                Log.d(TAG, "MediaPlayer stopped and released");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping sound", e);
        }

        releaseWakeLock();
    }

    public static void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
            Log.d(TAG, "WakeLock released");
        }
    }
}