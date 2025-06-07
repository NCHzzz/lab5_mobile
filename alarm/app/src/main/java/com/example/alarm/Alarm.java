package com.example.alarm;

public class Alarm {
    private long id;
    private int hour;
    private int minute;
    private boolean isEnabled;
    private boolean[] repeatDays; // Sunday to Saturday
    private String label;

    public Alarm() {
        this.repeatDays = new boolean[7];
        this.label = "";
    }

    public Alarm(long id, int hour, int minute, boolean isEnabled) {
        this.id = id;
        this.hour = hour;
        this.minute = minute;
        this.isEnabled = isEnabled;
        this.repeatDays = new boolean[7];
        this.label = "";
    }

    // Getters and setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public int getHour() { return hour; }
    public void setHour(int hour) { this.hour = hour; }

    public int getMinute() { return minute; }
    public void setMinute(int minute) { this.minute = minute; }

    public boolean isEnabled() { return isEnabled; }
    public void setEnabled(boolean enabled) { isEnabled = enabled; }

    public boolean[] getRepeatDays() { return repeatDays; }
    public void setRepeatDays(boolean[] repeatDays) { this.repeatDays = repeatDays; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getTimeString() {
        return String.format("%02d:%02d", hour, minute);
    }

    public String getRepeatDaysString() {
        if (isRepeatNone()) {
            return "Once";
        }

        if (isRepeatEveryday()) {
            return "Every day";
        }

        StringBuilder days = new StringBuilder();
        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

        for (int i = 0; i < 7; i++) {
            if (repeatDays[i]) {
                if (days.length() > 0) {
                    days.append(", ");
                }
                days.append(dayNames[i]);
            }
        }

        return days.toString();
    }

    private boolean isRepeatNone() {
        for (boolean day : repeatDays) {
            if (day) return false;
        }
        return true;
    }

    private boolean isRepeatEveryday() {
        for (boolean day : repeatDays) {
            if (!day) return false;
        }
        return true;
    }
}