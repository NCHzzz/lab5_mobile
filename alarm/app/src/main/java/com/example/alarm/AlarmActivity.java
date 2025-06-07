package com.example.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AlarmActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private static final int SNOOZE_TIME_IN_MINUTES = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        // Set the window to be displayed even when the device is locked
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        // Play alarm sound only if it wasn't started by the receiver
        boolean isMediaPlayerStarted = getIntent().getBooleanExtra("MEDIA_PLAYER", false);

        if (!isMediaPlayerStarted) {
            // Get default alarm sound
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmSound == null) {
                alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }

            // Play alarm sound
            mediaPlayer = MediaPlayer.create(this, alarmSound);
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        }

        // Set up dismiss button
        Button dismissButton = findViewById(R.id.dismissButton);
        dismissButton.setOnClickListener(v -> {
            stopAlarm();
            finish();
        });

        // Set up snooze button
        Button snoozeButton = findViewById(R.id.snoozeButton);
        snoozeButton.setOnClickListener(v -> {
            snoozeAlarm();
            stopAlarm();
            finish();
        });
    }

    private void snoozeAlarm() {
        // Create intent for AlarmReceiver
        Intent intent = new Intent(this, AlarmReceiver.class);
        long alarmId = getIntent().getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, 0);
        intent.putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                (int) alarmId,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Get alarm manager and set it for 5 minutes from now
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        long snoozeTimeInMillis = System.currentTimeMillis() + (SNOOZE_TIME_IN_MINUTES * 60 * 1000);

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            snoozeTimeInMillis,
                            pendingIntent
                    );
                } else {
                    Toast.makeText(this, "Please allow exact alarms permission", Toast.LENGTH_SHORT).show();
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        snoozeTimeInMillis,
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        snoozeTimeInMillis,
                        pendingIntent
                );
            }

            Toast.makeText(this, "Alarm snoozed for " + SNOOZE_TIME_IN_MINUTES + " minutes",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void stopAlarm() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        // Stop any alarm sound started by the receiver
        // This will also release the WakeLock
        AlarmReceiver.stopSound();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlarm();
    }
}