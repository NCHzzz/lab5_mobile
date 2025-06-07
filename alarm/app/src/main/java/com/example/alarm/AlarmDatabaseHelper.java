package com.example.alarm;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class AlarmDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "alarms.db";
    private static final int DATABASE_VERSION = 2; // Increment version for database update

    private static final String TABLE_ALARMS = "alarms";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_HOUR = "hour";
    private static final String COLUMN_MINUTE = "minute";
    private static final String COLUMN_ENABLED = "enabled";
    private static final String COLUMN_REPEAT_DAYS = "repeat_days";
    private static final String COLUMN_LABEL = "label";

    public AlarmDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE " + TABLE_ALARMS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_HOUR + " INTEGER, " +
                COLUMN_MINUTE + " INTEGER, " +
                COLUMN_ENABLED + " INTEGER, " +
                COLUMN_REPEAT_DAYS + " TEXT, " +
                COLUMN_LABEL + " TEXT)";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add label column to existing database
            db.execSQL("ALTER TABLE " + TABLE_ALARMS + " ADD COLUMN " + COLUMN_LABEL + " TEXT DEFAULT ''");
        }
    }

    public long insertAlarm(Alarm alarm) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_HOUR, alarm.getHour());
        values.put(COLUMN_MINUTE, alarm.getMinute());
        values.put(COLUMN_ENABLED, alarm.isEnabled() ? 1 : 0);
        values.put(COLUMN_REPEAT_DAYS, repeatDaysToString(alarm.getRepeatDays()));
        values.put(COLUMN_LABEL, alarm.getLabel());

        long id = db.insert(TABLE_ALARMS, null, values);
        db.close();
        return id;
    }

    public List<Alarm> getAllAlarms() {
        List<Alarm> alarmList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_ALARMS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Alarm alarm = new Alarm();
                alarm.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                alarm.setHour(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_HOUR)));
                alarm.setMinute(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MINUTE)));
                alarm.setEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENABLED)) == 1);
                alarm.setRepeatDays(stringToRepeatDays(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REPEAT_DAYS))));

                // Get label with fallback
                int labelIndex = cursor.getColumnIndex(COLUMN_LABEL);
                if (labelIndex != -1) {
                    alarm.setLabel(cursor.getString(labelIndex));
                }

                alarmList.add(alarm);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return alarmList;
    }

    public void updateAlarm(Alarm alarm) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_HOUR, alarm.getHour());
        values.put(COLUMN_MINUTE, alarm.getMinute());
        values.put(COLUMN_ENABLED, alarm.isEnabled() ? 1 : 0);
        values.put(COLUMN_REPEAT_DAYS, repeatDaysToString(alarm.getRepeatDays()));
        values.put(COLUMN_LABEL, alarm.getLabel());

        db.update(TABLE_ALARMS, values, COLUMN_ID + " = ?",
                new String[]{String.valueOf(alarm.getId())});
        db.close();
    }

    public void deleteAlarm(long alarmId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ALARMS, COLUMN_ID + " = ?",
                new String[]{String.valueOf(alarmId)});
        db.close();
    }

    private String repeatDaysToString(boolean[] repeatDays) {
        StringBuilder sb = new StringBuilder();
        for (boolean day : repeatDays) {
            sb.append(day ? "1" : "0");
        }
        return sb.toString();
    }

    private boolean[] stringToRepeatDays(String repeatDaysStr) {
        boolean[] repeatDays = new boolean[7];
        if (repeatDaysStr != null && repeatDaysStr.length() == 7) {
            for (int i = 0; i < 7; i++) {
                repeatDays[i] = repeatDaysStr.charAt(i) == '1';
            }
        }
        return repeatDays;
    }
}