package com.silent.telebot;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WordOrderActivity extends Activity {

    private static final String PLAYER = "عصام المهدي";
    private static final int MAX_LEVEL = 12;

    // عناصر الواجهة
    private TextView levelText, scoreText, difficultyText, motivationText;
    private TextView answerText, crosswordStatus;
    private TextView starsText, coinsText;

    private LinearLayout scramblePanel, crosswordPanel, lettersRow;
    private GridLayout crosswordGrid;

    private EditText[][] cells = new EditText[5][5];
    private final List<String> selected = new ArrayList<>();

    // حالة اللعبة
    private int level = 1;
    private int score = 0;
    private int stars = 0;
    private int coins = 0;
    private int attempts = 3;
    private int wordIndex = 0;

    // قوائم الكلمات (12 مستوى)
    private final String[] words = {
            "كتاب", "شجرة", "مدرسة", "حديقة", "مكتبة",
            "سيارة", "تفاحة", "نجاحك", "مغامر", "عبقري", "مبدع", "انتصار"
    };

    private final String[][] crosswordWords = {
            {"كتاب", "قمر"},
            {"شجرة", "شمس"},
            {"مدرسة", "درس"},
            {"حديقة", "حقل"},
            {"مكتبة", "كتب"},
            {"سيارة", "سير"},
            {"تفاحة", "فاح"},
            {"نجاحك", "نجح"},
            {"مغامر", "غمر"},
            {"عبقري", "بقر"},
            {"مبدع", "بدع"},
            {"انتصار", "نصر"}
    };

    private final String[] messages = {
            "بداية ممتازة يا PLAYER، كل بطل يبدأ بخطوة!",
            "أحسنت يا PLAYER، تركيزك رائع!",
            "استمر يا PLAYER، أنت تتقدم بسرعة!",
            "مذهل يا PLAYER، المرحلة القادمة أصعب!",
            "عقلك يعمل كالأبطال يا PLAYER!",
            "اقتربت من القمة يا PLAYER!",
            "لا تستسلم يا PLAYER، الحل أمامك!",
            "أداء قوي يا PLAYER، أنت في مستوى متقدم!",
            "رائع يا PLAYER، لم يبقَ إلا القليل!",
            "هذه روح المحترفين يا PLAYER!",
            "استثنائي يا PLAYER، أنت قريب من النهاية!",
            "أسطورة يا PLAYER، أكملت جميع المراحل!"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crossword);

        // ربط العناصر
        scramblePanel = findViewById(R.id.scramblePanel);
        crosswordPanel = findViewById(R.id.crosswordPanel);
        lettersRow = findViewById(R.id.lettersRow);
        crosswordGrid = findViewById(R.id.crosswordGrid);

        answerText = findViewById(R.id.scrambleAnswer);
        crosswordStatus = findViewById(R.id.crosswordStatus);

        levelText = findViewById(R.id.levelText);
        scoreText = findViewById(R.id.scoreText);
        difficultyText = findViewById(R.id.difficultyText);
        motivationText = findViewById(R.id.motivationText);

        starsText = findViewById(R.id.starsText);
        coinsText = findViewById(R.id.coinsText);

        // الأزرار
        findViewById(R.id.scrambleModeButton).setOnClickListener(v -> showMode(true));
        findViewById(R.id.crosswordModeButton).setOnClickListener(v -> showMode(false));
        findViewById(R.id.scrambleCheckButton).setOnClickListener(v -> checkWord());
        findViewById(R.id.scrambleNewButton).setOnClickListener(v -> loadWord());
        findViewById(R.id.crosswordCheckButton).setOnClickListener(v -> checkCrossword());
        findViewById(R.id.crosswordClearButton).setOnClickListener(v -> clearCrossword());

        updateHeader();
        loadWord();
        buildCrossword();
        showMode(true);
    }

    private void updateHeader() {
        levelText.setText("المرحلة " + level + " من " + MAX_LEVEL);
        scoreText.setText("النقاط: " + score);
        difficultyText.setText("الصعوبة: " + getDifficulty());

        if (starsText != null) starsText.setText("🌟 " + stars);
        if (coinsText != null) coinsText.setText("💰 " + coins);

        String message = messages[level - 1].replace("PLAYER", PLAYER);
        motivationText.setText(message);
    }

    private String getDifficulty() {
        if (level <= 3) return "سهل";
        if (level <= 6) return "متوسط";
        if (level <= 9) return "صعب";
        return "خبير";
    }

    private void showMode(boolean wordMode) {
        scramblePanel.setVisibility(wordMode ? View.VISIBLE : View.GONE);
        crosswordPanel.setVisibility(wordMode ? View.GONE : View.VISIBLE);
    }

    // ============= وضع ترتيب الحروف =============
    private void loadWord() {
        selected.clear();
        answerText.setText("اختر الحروف لتكوين الكلمة");
        lettersRow.removeAllViews();

        String word = words[wordIndex];
        List<String> letters = new ArrayList<>();
        for (int i = 0; i < word.length(); i++) {
            letters.add(String.valueOf(word.charAt(i)));
        }
        Collections.shuffle(letters);

        for (String letter : letters) {
            Button button = new Button(this);
            button.setText(letter);
            button.setTextSize(19);
            button.setAllCaps(false);
            button.setGravity(Gravity.CENTER);
            button.setPadding(0, 0, 0, 0);
            button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            button.setTextColor(Color.WHITE);
            button.setBackground(createBackground(
                    Color.rgb(48, 112, 224),
                    Color.rgb(28, 65, 132), 3, 100
            ));
            button.setElevation(dp(4));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(54), dp(54));
            params.setMargins(dp(3), dp(3), dp(3), dp(3));
            lettersRow.addView(button, params);

            button.setOnClickListener(v -> {
                v.setEnabled(false);
                selected.add(button.getText().toString());
                answerText.setText(join(selected));
            });
        }

        wordIndex = (wordIndex + 1) % words.length;
    }

    private void checkWord() {
        String expected = words[(wordIndex + words.length - 1) % words.length];

        if (join(selected).equals(expected)) {
            int earnedStars = (attempts >= 3) ? 3 : (attempts >= 2 ? 2 : 1);
            stars += earnedStars;
            coins += 10;
            score += 10 * level;
            nextLevel();
        } else {
            attempts--;
            score = Math.max(0, score - 2);
            updateHeader();

            motivationText.setText("حاول من جديد يا " + PLAYER + "، بقيت " + attempts + " محاولات");
            Toast.makeText(this, "ليست الإجابة الصحيحة، لا تستسلم!", Toast.LENGTH_SHORT).show();

            if (attempts <= 0) {
                Toast.makeText(this, "الإجابة الصحيحة: " + expected, Toast.LENGTH_LONG).show();
                attempts = 3;
                loadWord();
            }
        }
    }

    // ============= وضع الكلمات المتقاطعة =============
    private char[][] solution() {
        char[][] grid = new char[5][5];
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                grid[r][c] = ' ';
            }
        }

        String horizontal = crosswordWords[level - 1][0];
        String vertical = crosswordWords[level - 1][1];

        for (int c = 0; c < Math.min(5, horizontal.length()); c++) {
            grid[2][c] = horizontal.charAt(c);
        }
        for (int r = 0; r < Math.min(5, vertical.length()); r++) {
            grid[r][4] = vertical.charAt(r);
        }

        return grid;
    }

    private void buildCrossword() {
        char[][] solution = solution();
        crosswordGrid.removeAllViews();
        crosswordGrid.setColumnCount(5);
        crosswordGrid.setRowCount(5);

        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                EditText cell = new EditText(this);
                cells[r][c] = cell;

                cell.setGravity(Gravity.CENTER);
                cell.setTextSize(21);
                cell.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                cell.setSingleLine(true);
                cell.setPadding(0, 0, 0, 0);
                cell.setTextColor(Color.rgb(25, 45, 80));

                boolean enabled = solution[r][c] != ' ';
                cell.setEnabled(enabled);

                cell.setBackground(createBackground(
                        enabled ? Color.WHITE : Color.rgb(218, 227, 240),
                        enabled ? Color.rgb(56, 102, 168) : Color.rgb(183, 198, 219),
                        2, 8
                ));

                cell.setElevation(enabled ? dp(2) : 0);

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = 62;
                params.columnSpec = GridLayout.spec(c, 1, 1f);
                params.rowSpec = GridLayout.spec(r, 1, 1f);
                params.setMargins(2, 2, 2, 2);

                crosswordGrid.addView(cell, params);
            }
        }

        crosswordStatus.setText("املأ الخانات - المحاولات: " + attempts);
    }

    private void checkCrossword() {
        char[][] answer = solution();
        int correct = 0;
        int total = 0;

        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                if (answer[r][c] == ' ') continue;
                total++;

                String value = cells[r][c].getText().toString().trim();
                boolean right = value.length() > 0 && value.charAt(0) == answer[r][c];
                if (right) correct++;

                cells[r][c].setTextColor(right ? Color.rgb(27, 145, 88) : Color.rgb(210, 55, 70));
            }
        }

        if (correct == total) {
            int earnedStars = (attempts >= 3) ? 3 : (attempts >= 2 ? 2 : 1);
            stars += earnedStars;
            coins += 15;
            score += 15 * level;
            nextLevel();
        } else {
            attempts--;
            score = Math.max(0, score - 3);
            updateHeader();

            crosswordStatus.setText("النتيجة: " + correct + " / " + total + " - المحاولات: " + Math.max(0, attempts));
            motivationText.setText(attempts > 0
                    ? "اقتربت يا " + PLAYER + "، راجع الحروف بهدوء"
                    : "لا بأس يا " + PLAYER + "، ابدأ محاولة جديدة");

            if (attempts <= 0) {
                attempts = 3;
                clearCrossword();
            }
        }
    }

    private void clearCrossword() {
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                if (cells[r][c].isEnabled()) {
                    cells[r][c].setText("");
                    cells[r][c].setTextColor(Color.rgb(25, 45, 80));
                }
            }
        }
        crosswordStatus.setText("املأ الخانات - المحاولات: " + attempts);
    }

    // ============= دوال مساعدة =============
    private void nextLevel() {
        attempts = 3;
        if (level == MAX_LEVEL) {
            updateHeader();
            Toast.makeText(this, "تهانينا يا " + PLAYER + "! أنهيت جميع المراحل", Toast.LENGTH_LONG).show();
            return;
        }
        level++;
        updateHeader();
        buildCrossword();
        loadWord();
        Toast.makeText(this, "أحسنت يا " + PLAYER + "! المرحلة التالية أصعب", Toast.LENGTH_SHORT).show();
    }

    private String join(List<String> letters) {
        StringBuilder sb = new StringBuilder();
        for (String l : letters) sb.append(l);
        return sb.toString();
    }

    private GradientDrawable createBackground(int fill, int stroke, int width, int radius) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(fill);
        gd.setStroke(dp(width), stroke);
        gd.setCornerRadius(dp(radius));
        return gd;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
    }
