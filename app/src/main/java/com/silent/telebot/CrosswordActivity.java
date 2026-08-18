package com.silent.telebot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrosswordActivity extends Activity {
    private GridLayout gridCrossword;
    private TextView tvQuestion, tvMotivation, tvLevel, tvProgress;
    private ProgressBar progressBar;
    private Button btnCheck, btnClear;
    private Map<String, String> answers = new HashMap<>();
    private Map<String, String> questions = new HashMap<>();
    private Map<String, String> userAnswers = new HashMap<>();
    private List<TextView> cells = new ArrayList<>();
    private int currentLevel = 1;
    private int totalWords = 6;
    private int correctAnswers = 0;
    private String selectedCellId = "";

    private void loadLevel1() {
        answers.put("A1", "ب");
        answers.put("A2", "ي");
        answers.put("A3", "ت");
        questions.put("A1", "الحرف الأول من كلمة 'بيت'؟");
        questions.put("A2", "الحرف الثاني من كلمة 'بيت'؟");
        questions.put("A3", "الحرف الثالث من كلمة 'بيت'؟");

        answers.put("B1", "د");
        answers.put("B2", "ا");
        answers.put("B3", "ر");
        questions.put("B1", "الحرف الأول من كلمة 'دار'؟");
        questions.put("B2", "الحرف الثاني من كلمة 'دار'؟");
        questions.put("B3", "الحرف الثالث من كلمة 'دار'؟");

        totalWords = 6;
        correctAnswers = 0;
        updateUI();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crossword);

        gridCrossword = findViewById(R.id.grid_crossword);
        tvQuestion = findViewById(R.id.tv_question);
        tvMotivation = findViewById(R.id.tv_motivation);
        tvLevel = findViewById(R.id.tv_level);
        tvProgress = findViewById(R.id.tv_progress);
        progressBar = findViewById(R.id.progress_bar);
        btnCheck = findViewById(R.id.btn_check);
        btnClear = findViewById(R.id.btn_clear);

        loadLevel1();
        drawGrid();
        setupKeyboard();

        showMotivation("🎤 أهلاً بك في الكلمات المتقاطعة! أنا عصام المهدي، أتمنى لك تجربة ممتعة! 💪");

        btnCheck.setOnClickListener(v -> checkAnswers());
        btnClear.setOnClickListener(v -> clearAll());
    }

    private void drawGrid() {
        gridCrossword.removeAllViews();
        gridCrossword.setRowCount(5);
        gridCrossword.setColumnCount(5);
        cells.clear();

        int size = (int) (getResources().getDisplayMetrics().widthPixels / 6.5);

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                String id = String.valueOf((char) ('A' + row)) + (col + 1);
                Button cell = new Button(this);
                cell.setText("");
                cell.setTextSize(24);
                cell.setBackgroundColor(0xFF2C2C2E);
                cell.setTextColor(0xFFFFFFFF);
                cell.setWidth(size);
                cell.setHeight(size);
                cell.setTag(id);
                cell.setOnClickListener(v -> {
                    String cellId = (String) v.getTag();
                    showQuestionForCell(cellId);
                });
                gridCrossword.addView(cell);
                cells.add(cell);
            }
        }
    }

    private void showQuestionForCell(String cellId) {
        selectedCellId = cellId;
        String question = questions.get(cellId);
        if (question != null) {
            tvQuestion.setText("❓ " + question);
        } else {
            tvQuestion.setText("🔍 اختر خلية أخرى لعرض السؤال");
        }
    }

    private void setupKeyboard() {
        GridLayout keyboard = findViewById(R.id.keyboard);
        keyboard.removeAllViews();
        keyboard.setRowCount(3);
        keyboard.setColumnCount(10);

        String[] letters = {"أ", "ب", "ت", "ث", "ج", "ح", "خ", "د", "ذ", "ر",
                "ز", "س", "ش", "ص", "ض", "ط", "ظ", "ع", "غ", "ف",
                "ق", "ك", "ل", "م", "ن", "ه", "و", "ي", "⌫", "␣"};

        int size = (int) (getResources().getDisplayMetrics().widthPixels / 10.5);

        for (String letter : letters) {
            Button btn = new Button(this);
            btn.setText(letter);
            btn.setTextSize(18);
            btn.setBackgroundColor(0xFF3A3A3C);
            btn.setTextColor(0xFFFFFFFF);
            btn.setWidth(size);
            btn.setHeight(size);
            btn.setPadding(2, 2, 2, 2);
            btn.setOnClickListener(v -> {
                String key = ((Button) v).getText().toString();
                if (key.equals("⌫")) {
                    deleteLastChar();
                } else if (!key.equals("␣")) {
                    insertLetter(key);
                }
            });
            keyboard.addView(btn);
        }
    }

    private void insertLetter(String letter) {
        if (selectedCellId == null || selectedCellId.isEmpty()) {
            showMotivation("😅 اختر خلية أولاً! - عصام المهدي");
            return;
        }
        for (TextView cell : cells) {
            if (cell.getTag().equals(selectedCellId)) {
                cell.setText(letter);
                userAnswers.put(selectedCellId, letter);
                break;
            }
        }
    }

    private void deleteLastChar() {
        if (selectedCellId == null) return;
        for (TextView cell : cells) {
            if (cell.getTag().equals(selectedCellId)) {
                cell.setText("");
                userAnswers.remove(selectedCellId);
                break;
            }
        }
    }

    private void checkAnswers() {
        correctAnswers = 0;
        for (Map.Entry<String, String> entry : answers.entrySet()) {
            String cellId = entry.getKey();
            String correctAnswer = entry.getValue();
            String userAnswer = userAnswers.get(cellId);
            if (userAnswer != null && userAnswer.equals(correctAnswer)) {
                correctAnswers++;
                for (TextView cell : cells) {
                    if (cell.getTag().equals(cellId)) {
                        cell.setBackgroundColor(0xFF4CAF50);
                        break;
                    }
                }
            } else {
                for (TextView cell : cells) {
                    if (cell.getTag().equals(cellId)) {
                        cell.setBackgroundColor(0xFFF44336);
                        break;
                    }
                }
            }
        }
        updateUI();

        if (correctAnswers == totalWords) {
            showMotivation("🎉 أحسنت! أكملت المستوى " + currentLevel + " 🎉\nفخور بك يا بطل. - عصام المهدي");
        } else {
            int wrong = totalWords - correctAnswers;
            showMotivation("📝 لديك " + wrong + " أخطاء. حاول مجدداً! - عصام المهدي");
        }
    }

    private void clearAll() {
        userAnswers.clear();
        for (TextView cell : cells) {
            cell.setText("");
            cell.setBackgroundColor(0xFF2C2C2E);
        }
        correctAnswers = 0;
        updateUI();
        showMotivation("🧹 تم مسح جميع الإجابات. ابدأ من جديد! - عصام المهدي");
    }

    private void updateUI() {
        tvProgress.setText(correctAnswers + "/" + totalWords);
        progressBar.setProgress((correctAnswers * 100) / totalWords);
    }

    private void showMotivation(String message) {
        tvMotivation.setText(message);
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            tvMotivation.setText("");
        }, 4000);
    }
}
