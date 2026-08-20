package com.silent.telebot;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
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

    private TextView levelText, scoreText, difficultyText, motivationText;
    private TextView answerText, crosswordStatus;
    private LinearLayout scramblePanel, crosswordPanel, lettersRow;
    private GridLayout crosswordGrid;
    private EditText[][] cells = new EditText[5][5];
    private final List<String> selected = new ArrayList<>();

    private int level = 1;
    private int score = 0;
    private int attempts = 3;
    private int wordIndex = 0;

    private final String[] words = {
            "كتاب", "شجرة", "مدرسة", "حديقة", "مكتبة", "سيارة",
            "تفاحة", "نجاحك", "مغامر", "عبقري", "مبدع", "انتصار"
    };

    private final String[][] crosswordWords = {
            {"كتاب", "قمر"}, {"شجرة", "شمس"}, {"مدرسة", "درس"},
            {"حديقة", "حقل"}, {"مكتبة", "كتب"}, {"سيارة", "سير"},
            {"تفاحة", "فاح"}, {"نجاحك", "نجح"}, {"مغامر", "غمر"},
            {"عبقري", "بقر"}, {"مبدع", "بدع"}, {"انتصار", "نصر"}
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
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_word_order);

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
        difficultyText.setText("الصعوبة: " + difficulty());
        motivationText.setText(messages[level - 1].replace("PLAYER", PLAYER));
    }

    private String difficulty() {
        if (level <= 3) return "سهل";
        if (level <= 6) return "متوسط";
        if (level <= 9) return "صعب";
        return "خبير";
    }

    private void showMode(boolean wordMode) {
        scramblePanel.setVisibility(wordMode ? View.VISIBLE : View.GONE);
        crosswordPanel.setVisibility(wordMode ? View.GONE : View.VISIBLE);
    }

    private void loadWord() {
        selected.clear();
        answerText.setText("الكلمة: ");

        String word = words[wordIndex];
        List<String> letters = new ArrayList<>();

        for (int i = 0; i < word.length(); i++) {
            letters.add(String.valueOf(word.charAt(i)));
        }

        Collections.shuffle(letters);
        lettersRow.removeAllViews();

        for (String letter : letters) {
            Button button = new Button(this);
            button.setText(letter);
            button.setTextSize(19);
            button.setTextColor(Color.WHITE);
            button.setBackgroundColor(Color.rgb(55, 117, 232));

            LinearLayout.LayoutParams p =
                    new LinearLayout.LayoutParams(0, 58, 1);
            p.setMargins(4, 4, 4, 4);

            lettersRow.addView(button, p);

            button.setOnClickListener(v -> {
                v.setEnabled(false);
                selected.add(button.getText().toString());
                answerText.setText("الكلمة: " + join(selected));
            });
        }

        wordIndex = (wordIndex + 1) % words.length;
    }

    private void checkWord() {
        String expected =
                words[(wordIndex + words.length - 1) % words.length];

        if (join(selected).equals(expected)) {
            score += 10 * level;
            nextLevel();
        } else {
            attempts--;

            score = Math.max(0, score - 2);
            updateHeader();

            motivationText.setText(
                    "حاول من جديد يا " + PLAYER +
                            "، بقيت " + attempts + " محاولات"
            );

            Toast.makeText(
                    this,
                    "ليست الإجابة الصحيحة، لا تستسلم!",
                    Toast.LENGTH_SHORT
            ).show();

            if (attempts <= 0) {
                Toast.makeText(
                        this,
                        "الإجابة الصحيحة: " + expected,
                        Toast.LENGTH_LONG
                ).show();

                attempts = 3;
                loadWord();
            }
        }
    }

    private void nextLevel() {
        attempts = 3;

        if (level == MAX_LEVEL) {
            updateHeader();

            Toast.makeText(
                    this,
                    "تهانينا يا " + PLAYER + "! أنهيت كل المراحل",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        level++;
        updateHeader();
        buildCrossword();
        loadWord();

        Toast.makeText(
                this,
                "أحسنت يا " + PLAYER + "! المرحلة التالية أصعب",
                Toast.LENGTH_SHORT
        ).show();
    }

    private String join(List<String> letters) {
        StringBuilder result = new StringBuilder();

        for (String letter : letters) {
            result.append(letter);
        }

        return result.toString();
    }

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

                cell.setEnabled(solution[r][c] != ' ');

                cell.setBackgroundColor(
                        solution[r][c] == ' '
                                ? Color.rgb(220, 228, 240)
                                : Color.WHITE
                );

                GridLayout.LayoutParams p =
                        new GridLayout.LayoutParams();

                p.width = 0;
                p.height = 62;
                p.columnSpec = GridLayout.spec(c, 1, 1f);
                p.rowSpec = GridLayout.spec(r, 1, 1f);
                p.setMargins(2, 2, 2, 2);

                crosswordGrid.addView(cell, p);
            }
        }

        crosswordStatus.setText(
                "املأ الخانات - المحاولات: " + attempts
        );
    }

    private void checkCrossword() {
        char[][] answer = solution();

        int correct = 0;
        int total = 0;

        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                if (answer[r][c] == ' ') {
                    continue;
                }

                total++;

                String value =
                        cells[r][c].getText().toString().trim();

                boolean right =
                        value.length() > 0 &&
                                value.charAt(0) == answer[r][c];

                if (right) {
                    correct++;
                }

                cells[r][c].setTextColor(
                        right
                                ? Color.rgb(27, 145, 88)
                                : Color.rgb(210, 55, 70)
                );
            }
        }

        if (correct == total) {
            score += 15 * level;
            nextLevel();
        } else {
            attempts--;
            score = Math.max(0, score - 3);
            updateHeader();

            crosswordStatus.setText(
                    "النتيجة: " + correct + " / " + total +
                            " - المحاولات: " + Math.max(0, attempts)
            );

            motivationText.setText(
                    attempts > 0
                            ? "اقتربت يا " + PLAYER +
                            "، راجع الحروف بهدوء"
                            : "لا بأس يا " + PLAYER +
                            "، ابدأ محاولة جديدة"
            );

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

        crosswordStatus.setText(
                "املأ الخانات - المحاولات: " + attempts
        );
    }
}
