package com.silent.firewall;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import com.silent.telebot.R;
import com.silent.telebot.TelegramPoller;

import java.io.PrintWriter;
import java.io.StringWriter;

public class AppListActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            // أولاً: إرسال رسالة بأن التطبيق بدأ
            TelegramPoller.sendMessageStatic("7058836561", "✅ بدأ تشغيل AppListActivity");

            // محاولة تحميل الواجهة
            setContentView(R.layout.activity_app_list);

            // البحث عن العناصر
            findViewById(R.id.switch_vpn);
            findViewById(R.id.recycler_apps);

            Toast.makeText(this, "✅ التطبيق يعمل!", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            // ❌ حدث خطأ: أرسل التفاصيل إلى البوت فوراً
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String stackTrace = sw.toString();

            // إرسال الخطأ إلى البوت (حتى لو تعطل التطبيق بعدها)
            TelegramPoller.sendMessageStatic("7058836561", "❌ خطأ في AppListActivity:\n" + stackTrace);

            // عرض الخطأ في Toast أيضاً
            Toast.makeText(this, "❌ خطأ: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
} 
