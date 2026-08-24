package com.silent.telebot;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FirewallVpnService extends VpnService {
    private static final String TAG = "FirewallVpnService";
    private Thread vpnThread;
    private ParcelFileDescriptor vpnInterface = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (vpnInterface == null) {
            startVpn();
        }
        return START_STICKY;
    }

    private void startVpn() {
        try {
            Builder builder = new Builder();
            builder.setSession("جدار الحماية الخاص بي")
                   .addAddress("10.0.0.2", 32)
                   .addRoute("0.0.0.0", 0);

            // جلب قائمة التطبيقات المحظورة من SharedPreferences
            List<String> blockedPackages = getBlockedPackages();

            for (String packageName : blockedPackages) {
                try {
                    builder.addDisallowedApplication(packageName);
                    Log.d(TAG, "تم حظر: " + packageName);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "التطبيق غير موجود: " + packageName);
                }
            }

            vpnInterface = builder.establish();
            Log.d(TAG, "VPN started successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error starting VPN", e);
        }
    }

    private List<String> getBlockedPackages() {
        // هنا ستقرأ من SharedPreferences
        // مؤقتاً نعيد قائمة فارغة
        return new ArrayList<>();
    }

    @Override
    public void onDestroy() {
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
                      }
