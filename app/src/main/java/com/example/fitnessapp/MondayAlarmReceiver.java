package com.example.fitnessapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MondayAlarmReceiver extends BroadcastReceiver {
    public static final String ACTION_MONDAY_ALARM = "com.example.fitnessapp.MONDAY_ALARM_TRIGGERED";

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent alarmIntent = new Intent(ACTION_MONDAY_ALARM);
        context.sendBroadcast(alarmIntent);

        // Reschedule for next Monday
        AlarmScheduler.scheduleMondayMidnight(context);
    }
}