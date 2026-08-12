package com.silent.telebot;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AutoStartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        startTelegramAlarm(context);
    }

    private void startTelegramAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(context, TelegramReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long interval = 60 * 1000;
        long triggerTime = System.currentTimeMillis() + interval;

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else {
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerTime, interval, pendingIntent);
            }
        }
    }
  }
