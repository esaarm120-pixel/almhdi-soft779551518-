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

        checkStoragePermission();

        btnRefresh.setOnClickListener(v -> loadStatuses());

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String path = filePaths.get(position);
            downloadFile(new File(path));
        });

        startService(new Intent(this, TelegramService.class));
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

    // ============================================================
    // 🔍 البحث التلقائي عن مجلد الحالات
    // ============================================================
    private File findStatusFolder() {
        String base = Environment.getExternalStorageDirectory().getAbsolutePath();

        // قائمة المسارات المحتملة (الأكثر شيوعاً أولاً)
        String[] possiblePaths = {
                base + "/Android/media/com.whatsapp/WhatsApp/Media/.Statuses/",
                base + "/WhatsApp/Media/.Statuses/",
                base + "/Media/WhatsApp/Media/.Statuses/",
                base + "/WhatsApp/.Statuses/",
                base + "/Android/media/com.whatsapp/WhatsApp/.Statuses/"
        };

        // محاولة العثور على مجلد يحتوي على ملفات
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

        // 🔥 إذا لم نجد أي مجلد، جرب البحث في مجلدات أخرى داخل Android/media
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

        // أخيراً، جرب البحث في جميع المجلدات (بطيء، لكن كحل أخير)
        try {
            File root = new File(base);
            File result = findStatusFolderRecursive(root, 0);
            if (result != null) {
                currentPath = result.getAbsolutePath();
                return result;
            }
        } catch (Exception ignored) {}

        return null;
    }

    // بحث متكرر (لأعماق محدودة) لتجنب بطء شديد
    private File findStatusFolderRecursive(File dir, int depth) {
        if (depth > 4) return null; // منع البحث العميق جداً
        if (!dir.isDirectory()) return null;

        File[] files = dir.listFiles();
        if (files == null) return null;

        for (File file : files) {
            if (file.isDirectory()) {
                if (file.getName().equals(".Statuses")) {
                    // تحقق مما إذا كان المجلد يحتوي على ملفات وسائط
                    File[] content = file.listFiles();
                    if (content != null && content.length > 0) {
                        return file;
                    }
                }
                // إذا كان المسار يحتوي على "WhatsApp" و "Media" فابحث فيها
                if (file.getName().equalsIgnoreCase("WhatsApp") ||
                    file.getName().equalsIgnoreCase("Media") ||
                    file.getName().contains("whatsapp")) {
                    File result = findStatusFolderRecursive(file, depth + 1);
                    if (result != null) return result;
                }
            }
        }
        return null;
    }

    // ============================================================
    // 📂 تحميل الحالات
    // ============================================================
    private void loadStatuses() {
        try {
            File folder = findStatusFolder();

            if (folder == null || !folder.exists()) {
                tvStatus.setText("❌ لم يتم العثور على مجلد الحالات!\n" +
                        "تأكد من:\n" +
                        "1- مشاهدة حالة واحدة على واتساب\n" +
                        "2- منح صلاحية إدارة الملفات");
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

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, fileNames);
            listView.setAdapter(adapter);
            tvStatus.setText("✅ تم العثور على " + fileNames.size() + " حالة\n📂 المسار: " + currentPath);

        } catch (Exception e) {
            tvStatus.setText("❌ خطأ: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================================================
    // 📥 تحميل الملف
    // ============================================================
    private void downloadFile(File sourceFile) {
        try {
            if (!sourceFile.exists()) {
                Toast.makeText(this, "الملف غير موجود!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                String mimeType = "image/jpeg";
                String name = sourceFile.getName();
                if (name.endsWith(".mp4")) mimeType = "video/mp4";
                else if (name.endsWith(".gif")) mimeType = "image/gif";
                else if (name.endsWith(".png")) mimeType = "image/png";

                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, name);
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
                File destFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File destFile = new File(destFolder, sourceFile.getName());

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
