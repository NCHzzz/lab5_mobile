package com.example.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Calendar;
import java.util.List;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder> {
    private Context context;
    private List<Alarm> alarmList;
    private AlarmDatabaseHelper dbHelper;
    private AlarmManager alarmManager;

    public AlarmAdapter(Context context, List<Alarm> alarmList) {
        this.context = context;
        this.alarmList = alarmList;
        this.dbHelper = new AlarmDatabaseHelper(context);
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.alarm_item, parent, false);
        return new AlarmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        Alarm alarm = alarmList.get(position);

        // Set time text
        holder.timeTextView.setText(alarm.getTimeString());

        // Set label text
        holder.labelTextView.setText(alarm.getLabel().isEmpty() ? "Alarm" : alarm.getLabel());

        // Set repeat days text
        holder.daysTextView.setText(alarm.getRepeatDaysString());

        // Set switch without triggering the listener
        holder.enableSwitch.setOnCheckedChangeListener(null);
        holder.enableSwitch.setChecked(alarm.isEnabled());

        // Set switch listener
        holder.enableSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            alarm.setEnabled(isChecked);
            dbHelper.updateAlarm(alarm);
            if (isChecked) {
                scheduleAlarm(alarm);
            } else {
                cancelAlarm(alarm);
            }
        });

        // Set item click listener for editing
        holder.itemView.setOnClickListener(v -> {
            // Call edit alarm method from MainActivity
            if (context instanceof MainActivity) {
                ((MainActivity) context).showEditAlarmDialog(alarm);
            }
        });

        // Set delete button
        holder.deleteButton.setOnClickListener(v -> {
            cancelAlarm(alarm);
            dbHelper.deleteAlarm(alarm.getId());
            alarmList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, alarmList.size());
        });
    }

    @Override
    public int getItemCount() {
        return alarmList.size();
    }

    public void updateAlarmList(List<Alarm> newAlarmList) {
        this.alarmList = newAlarmList;
        notifyDataSetChanged();
    }

    public void scheduleAlarm(Alarm alarm) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, alarm.getHour());
        calendar.set(Calendar.MINUTE, alarm.getMinute());
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.getId());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) alarm.getId(),
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                Toast.makeText(context, "Please allow exact alarms permission", Toast.LENGTH_SHORT).show();
                Intent alarmPermissionIntent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                context.startActivity(alarmPermissionIntent);
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
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.getId());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) alarm.getId(),
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    static class AlarmViewHolder extends RecyclerView.ViewHolder {
        TextView timeTextView;
        TextView labelTextView;
        TextView daysTextView;
        Switch enableSwitch;
        View deleteButton;

        public AlarmViewHolder(@NonNull View itemView) {
            super(itemView);
            timeTextView = itemView.findViewById(R.id.alarmTimeTextView);
            labelTextView = itemView.findViewById(R.id.alarmLabelTextView);
            daysTextView = itemView.findViewById(R.id.alarmDaysTextView);
            enableSwitch = itemView.findViewById(R.id.alarmEnableSwitch);
            deleteButton = itemView.findViewById(R.id.deleteAlarmButton);
        }
    }
}