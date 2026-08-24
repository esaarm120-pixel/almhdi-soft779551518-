package com.silent.firewall;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;   // ← هذا الاستيراد الجديد
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FirewallVpnService extends VpnService {
    private static final String TAG = "FirewallVpnService";
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
            builder.setSession("جدار الحماية")
                   .addAddress("10.0.0.2", 32)
                   .addRoute("0.0.0.0", 0);

            List<String> blockedPackages = getBlockedPackages();
            for (String pkg : blockedPackages) {
                try {
                    builder.addDisallowedApplication(pkg);
                    Log.d(TAG, "تم حظر: " + pkg);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "التطبيق غير موجود: " + pkg);
                }
            }

            vpnInterface = builder.establish();
            Log.d(TAG, "VPN started");

        } catch (Exception e) {
            Log.e(TAG, "Error starting VPN", e);
        }
    }

    private List<String> getBlockedPackages() {
        List<String> blocked = new ArrayList<>();
        SharedPreferences prefs = getSharedPreferences("firewall", MODE_PRIVATE);
        Map<String, ?> all = prefs.getAll();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            if (entry.getValue() instanceof Boolean && (Boolean) entry.getValue()) {
                blocked.add(entry.getKey());
            }
        }
        return blocked;
    }

    public static boolean isRunning() {
        return false;
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
