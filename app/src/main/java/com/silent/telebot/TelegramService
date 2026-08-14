package com.silent.telebot;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class TelegramService extends Service {
    private static final String CHANNEL_ID = "TeleBotChannel";
    private static final int NOTIF_ID = 1001;
    private Thread pollerThread;
    private volatile boolean running = true;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIF_ID, buildNotification());
        startPoller();
    }

    private void startPoller() {
        pollerThread = new Thread(() -> {
            while (running) {
                try {
                    new TelegramPoller(TelegramService.this).run();
                    Thread.sleep(60000); // كل 60 ثانية
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        pollerThread.start();
    }

    @Override
    public void onDestroy() {
        running = false;
        if (pollerThread != null) pollerThread.interrupt();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "TeleBot", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("يعمل في الخلفية");
            channel.setSound(null, null);
            channel.enableVibration(false);
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🛠️ TeleBot")
                .setContentText("يعمل في الخلفية...")
                .setSmallIcon(android.R.drawable.ic_menu_settings)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }
}
