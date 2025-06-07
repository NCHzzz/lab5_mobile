package com.example.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView alarmsRecyclerView;
    private AlarmAdapter alarmAdapter;
    private AlarmDatabaseHelper dbHelper;
    private FloatingActionButton addAlarmFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new AlarmDatabaseHelper(this);

        // Initialize RecyclerView
        alarmsRecyclerView = findViewById(R.id.alarmsRecyclerView);
        alarmsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Load alarms from database
        loadAlarms();

        // Set up FAB for adding new alarms
        addAlarmFab = findViewById(R.id.addAlarmFab);
        addAlarmFab.setOnClickListener(v -> showTimePickerDialog());

        // Check for notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        // Check for schedule exact alarms permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!((android.app.AlarmManager) getSystemService(ALARM_SERVICE)).canScheduleExactAlarms()) {
                new AlertDialog.Builder(this)
                        .setTitle("Permission Required")
                        .setMessage("This app needs permission to schedule exact alarms")
                        .setPositiveButton("Grant", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAlarms();
    }

    private void loadAlarms() {
        List<Alarm> alarmList = dbHelper.getAllAlarms();

        if (alarmAdapter == null) {
            alarmAdapter = new AlarmAdapter(this, alarmList);
            alarmsRecyclerView.setAdapter(alarmAdapter);
        } else {
            alarmAdapter.updateAlarmList(alarmList);
        }
    }

    private void showTimePickerDialog() {
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(7)
                .setMinute(0)
                .setTitleText("Set alarm time")
                .build();

        timePicker.addOnPositiveButtonClickListener(v -> {
            // Create new alarm
            Alarm alarm = new Alarm();
            alarm.setHour(timePicker.getHour());
            alarm.setMinute(timePicker.getMinute());
            alarm.setEnabled(true);

            // Save to database
            long id = dbHelper.insertAlarm(alarm);
            alarm.setId(id);

            // Schedule the alarm directly
            scheduleAlarm(alarm);

            // Load alarms to update UI
            loadAlarms();

            Toast.makeText(MainActivity.this,
                    "Alarm set for " + String.format("%02d:%02d", alarm.getHour(), alarm.getMinute()),
                    Toast.LENGTH_SHORT).show();
        });

        timePicker.show(getSupportFragmentManager(), "time_picker");
    }

    public void showEditAlarmDialog(Alarm alarm) {
        // Inflate the dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.alarm_edit_dialog, null);

        // Get references to dialog views
        TextInputEditText labelEditText = dialogView.findViewById(R.id.alarmLabelEditText);

        // Set up day checkboxes
        CheckBox[] dayCheckboxes = new CheckBox[7];
        dayCheckboxes[0] = dialogView.findViewById(R.id.cbSunday);
        dayCheckboxes[1] = dialogView.findViewById(R.id.cbMonday);
        dayCheckboxes[2] = dialogView.findViewById(R.id.cbTuesday);
        dayCheckboxes[3] = dialogView.findViewById(R.id.cbWednesday);
        dayCheckboxes[4] = dialogView.findViewById(R.id.cbThursday);
        dayCheckboxes[5] = dialogView.findViewById(R.id.cbFriday);
        dayCheckboxes[6] = dialogView.findViewById(R.id.cbSaturday);

        // Fill dialog with alarm data
        labelEditText.setText(alarm.getLabel());
        boolean[] repeatDays = alarm.getRepeatDays();
        for (int i = 0; i < 7; i++) {
            dayCheckboxes[i].setChecked(repeatDays[i]);
        }

        // Create time picker for editing time
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(alarm.getHour())
                .setMinute(alarm.getMinute())
                .setTitleText("Edit alarm time")
                .build();

        // Create and show dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit Alarm")
                .setView(dialogView)
                .setPositiveButton("Save", (dialogInterface, i) -> {
                    // Update alarm label and repeat days
                    alarm.setLabel(labelEditText.getText().toString());

                    boolean[] newRepeatDays = new boolean[7];
                    for (int j = 0; j < 7; j++) {
                        newRepeatDays[j] = dayCheckboxes[j].isChecked();
                    }
                    alarm.setRepeatDays(newRepeatDays);

                    // Update database
                    dbHelper.updateAlarm(alarm);

                    // Cancel and reschedule alarm if enabled
                    if (alarm.isEnabled()) {
                        // Cancel previous pending intent
                        cancelAlarm(alarm);
                        // Reschedule the alarm
                        scheduleAlarm(alarm);
                    }

                    // Refresh display
                    loadAlarms();
                })
                .setNeutralButton("Edit Time", (dialogInterface, i) -> {
                    // Show time picker when user clicks edit time
                    timePicker.show(getSupportFragmentManager(), "time_edit_picker");
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();

        // Set time picker listener
        timePicker.addOnPositiveButtonClickListener(v -> {
            // Update alarm time
            alarm.setHour(timePicker.getHour());
            alarm.setMinute(timePicker.getMinute());

            // Reshow the edit dialog after time was changed
            dialog.dismiss();
            showEditAlarmDialog(alarm);
        });
    }

    private void scheduleAlarm(Alarm alarm) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, alarm.getHour());
        calendar.set(Calendar.MINUTE, alarm.getMinute());
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.getId());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                (int) alarm.getId(),
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                Toast.makeText(this, "Please allow exact alarms permission", Toast.LENGTH_SHORT).show();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        } else {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        }
    }

    private void cancelAlarm(Alarm alarm) {
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.getId());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                (int) alarm.getId(),
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }
}