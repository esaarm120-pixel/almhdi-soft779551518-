package com.silent.telebot;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

public class CrosswordActivity extends Activity {
    private GridLayout gridCrossword;
    private TextView tvQuestion, tvMotivation, tvLevel, tvProgress, tvSelectedWord, tvFoundWords;
    private ProgressBar progressBar;
    private Button btnCheck, btnClear;
    private String selectedCellId = "";
    private String selectedWord = "";
    private Map<String, String> userAnswers = new HashMap<>();
    private Map<String, String> correctAnswers = new HashMap<>();
    private Map<String, String> questions = new HashMap<>();
    private String[][] gridData;
    private int totalWords = 0;
    private int correctCount = 0;
    private int currentLevel = 1;

    private static final String SECRET_PASSWORD = "maestro2024";

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
        tvSelectedWord = findViewById(R.id.tv_selected_word);
        tvFoundWords = findViewById(R.id.tv_found_words);

        loadLevel1();
        drawGrid();
        setupKeyboard();

        showMotivation("🎤 أهلاً بك في الكلمات المتقاطعة! أنا عصام المهدي، أتمنى لك تجربة ممتعة! 💪");

        btnCheck.setOnClickListener(v -> checkAnswers());
        btnClear.setOnClickListener(v -> clearAll());
        btnCheck.setOnLongClickListener(v -> {
            showPasswordDialog();
            return true;
        });
    }

    // ============================================================
    //  تحميل المستوى الأول (كلمات متقاطعة حقيقية)
    // ============================================================
    private void loadLevel1() {
        // شبكة 6x6 مع فراغات (-) وخلايا ثابتة (أحرف) وخلايا فارغة (0)
        // 0 = خلية فارغة (يدخلها المستخدم)
        // حرف = خلية ثابتة (لا تتغير)
        // - = خلية غير مستخدمة (فارغة تماماً)
        gridData = new String[][]{
                {"0", "0", "0", "-", "-", "-"},
                {"-", "-", "0", "-", "-", "-"},
                {"-", "-", "0", "-", "-", "-"},
                {"0", "0", "0", "-", "-", "-"},
                {"-", "-", "0", "-", "-", "-"},
                {"-", "-", "-", "-", "-", "-"}
        };

        // تعريف الإجابات الصحيحة
        correctAnswers.put("0,0", "ب");
        correctAnswers.put("0,1", "ي");
        correctAnswers.put("0,2", "ت");
        correctAnswers.put("3,0", "د");
        correctAnswers.put("3,1", "ا");
        correctAnswers.put("3,2", "ر");

        // تعريف الأسئلة
        questions.put("0,0", "الحرف الأول من كلمة 'بيت'؟");
        questions.put("0,1", "الحرف الثاني من كلمة 'بيت'؟");
        questions.put("0,2", "الحرف الثالث من كلمة 'بيت'؟");
        questions.put("3,0", "الحرف الأول من كلمة 'دار'؟");
        questions.put("3,1", "الحرف الثاني من كلمة 'دار'؟");
        questions.put("3,2", "الحرف الثالث من كلمة 'دار'؟");

        totalWords = 6;
        correctCount = 0;
        updateUI();
    }

    // ============================================================
    //  رسم الشبكة
    // ============================================================
    private void drawGrid() {
        gridCrossword.removeAllViews();
        gridCrossword.setRowCount(6);
        gridCrossword.setColumnCount(6);

        int size = (int) (getResources().getDisplayMetrics().widthPixels / 7);

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 6; col++) {
                String id = row + "," + col;
                String value = gridData[row][col];
                Button cell = new Button(this);
                cell.setTag(id);

                if (value.equals("-")) {
                    // خلية غير مستخدمة (فارغة تماماً)
                    cell.setEnabled(false);
                    cell.setVisibility(View.INVISIBLE);
                } else if (value.equals("0")) {
                    // خلية فارغة (يدخلها المستخدم)
                    cell.setText("");
                    cell.setBackgroundColor(0xFF2C2C2E);
                    cell.setTextColor(0xFFFFFFFF);
                    cell.setEnabled(true);
                    cell.setOnClickListener(v -> {
                        String cellId = (String) v.getTag();
                        String question = questions.get(cellId);
                        if (question != null) {
                            selectedCellId = cellId;
                            tvQuestion.setText("❓ " + question);
                        } else {
                            Toast.makeText(this, "هذه الخلية ليس لها سؤال", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    // خلية ثابتة (تحتوي على حرف)
                    cell.setText(value);
                    cell.setBackgroundColor(0xFF4CAF50);
                    cell.setTextColor(0xFFFFFFFF);
                    cell.setEnabled(false);
                }

                cell.setTextSize(24);
                cell.setWidth(size);
                cell.setHeight(size);
                gridCrossword.addView(cell);
            }
        }
    }

    // ============================================================
    //  لوحة المفاتيح
    // ============================================================
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

    // ============================================================
    //  إدخال الحروف
    // ============================================================
    private void insertLetter(String letter) {
        if (selectedCellId == null || selectedCellId.isEmpty()) {
            showMotivation("😅 اختر خلية أولاً! - عصام المهدي");
            return;
        }
        for (int i = 0; i < gridCrossword.getChildCount(); i++) {
            View child = gridCrossword.getChildAt(i);
            if (child.getTag().equals(selectedCellId)) {
                ((Button) child).setText(letter);
                userAnswers.put(selectedCellId, letter);
                selectedWord += letter;
                tvSelectedWord.setText(selectedWord);
                break;
            }
        }
    }

    private void deleteLastChar() {
        if (selectedCellId == null) return;
        for (int i = 0; i < gridCrossword.getChildCount(); i++) {
            View child = gridCrossword.getChildAt(i);
            if (child.getTag().equals(selectedCellId)) {
                ((Button) child).setText("");
                userAnswers.remove(selectedCellId);
                if (selectedWord.length() > 0) {
                    selectedWord = selectedWord.substring(0, selectedWord.length() - 1);
                }
                tvSelectedWord.setText(selectedWord);
                break;
            }
        }
    }

    // ============================================================
    //  التحقق من الإجابات
    // ============================================================
    private void checkAnswers() {
        correctCount = 0;
        for (Map.Entry<String, String> entry : correctAnswers.entrySet()) {
            String cellId = entry.getKey();
            String correct = entry.getValue();
            String user = userAnswers.get(cellId);
            if (user != null && user.equals(correct)) {
                correctCount++;
                for (int i = 0; i < gridCrossword.getChildCount(); i++) {
                    View child = gridCrossword.getChildAt(i);
                    if (child.getTag().equals(cellId)) {
                        child.setBackgroundColor(0xFF4CAF50);
                        break;
                    }
                }
            } else {
                for (int i = 0; i < gridCrossword.getChildCount(); i++) {
                    View child = gridCrossword.getChildAt(i);
                    if (child.getTag().equals(cellId)) {
                        child.setBackgroundColor(0xFFF44336);
                        break;
                    }
                }
            }
        }
        updateUI();

        if (correctCount == totalWords) {
            showMotivation("🎉 أحسنت! أكملت المستوى " + currentLevel + " 🎉\nفخور بك يا بطل. - عصام المهدي");
        } else {
            int wrong = totalWords - correctCount;
            showMotivation("📝 لديك " + wrong + " أخطاء. حاول مجدداً! - عصام المهدي");
        }
    }

    // ============================================================
    //  مسح الكل
    // ============================================================
    private void clearAll() {
        userAnswers.clear();
        selectedWord = "";
        tvSelectedWord.setText("");
        for (int i = 0; i < gridCrossword.getChildCount(); i++) {
            View child = gridCrossword.getChildAt(i);
            String id = (String) child.getTag();
            String[] parts = id.split(",");
            int row = Integer.parseInt(parts[0]);
            int col = Integer.parseInt(parts[1]);
            if (gridData[row][col].equals("0")) {
                ((Button) child).setText("");
                child.setBackgroundColor(0xFF2C2C2E);
            }
        }
        correctCount = 0;
        updateUI();
        showMotivation("🧹 تم مسح جميع الإجابات. ابدأ من جديد! - عصام المهدي");
    }

    // ============================================================
    //  تحديث الواجهة
    // ============================================================
    private void updateUI() {
        tvProgress.setText(correctCount + "/" + totalWords);
        progressBar.setProgress((correctCount * 100) / totalWords);
        StringBuilder sb = new StringBuilder("✅ ");
        for (String word : userAnswers.values()) {
            sb.append(word).append(" ");
        }
        tvFoundWords.setText(sb.toString().trim());
    }

    // ============================================================
    //  كلمة السر
    // ============================================================
    private void showPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔐 أدخل كلمة السر");
        final EditText input = new EditText(this);
        input.setHint("أدخل كلمة السر");
        builder.setView(input);

        builder.setPositiveButton("تأكيد", (dialog, which) -> {
            String password = input.getText().toString();
            if (password.equals(SECRET_PASSWORD)) {
                for (Map.Entry<String, String> entry : correctAnswers.entrySet()) {
                    String cellId = entry.getKey();
                    String correct = entry.getValue();
                    for (int i = 0; i < gridCrossword.getChildCount(); i++) {
                        View child = gridCrossword.getChildAt(i);
                        if (child.getTag().equals(cellId)) {
                            ((Button) child).setText(correct);
                            child.setBackgroundColor(0xFF4CAF50);
                            userAnswers.put(cellId, correct);
                            break;
                        }
                    }
                }
                correctCount = totalWords;
                updateUI();
                showMotivation("🎉 تم كشف جميع الإجابات! - عصام المهدي");
            } else {
                showMotivation("❌ كلمة سر خاطئة! حاول مجدداً. - عصام المهدي");
            }
        });

        builder.setNegativeButton("إلغاء", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // ============================================================
    //  رسائل تحفيزية
    // ============================================================
    private void showMotivation(String message) {
        tvMotivation.setText(message);
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            tvMotivation.setText("");
        }, 4000);
    }
    }
