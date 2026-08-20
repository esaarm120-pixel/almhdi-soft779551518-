package com.example.crossword;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class CrosswordActivity extends AppCompatActivity {

    private GridLayout crosswordGrid;
    private TextView tvStage;
    private TextView tvScore;
    private TextView tvMessage;
    private TextView tvQuestion;

    private Button btnCheck;
    private Button btnHint;
    private Button btnNext;

    private int currentStage = 0;
    private int score = 0;

    private final List<Stage> stages = new ArrayList<>();

    private EditText selectedCell;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crossword);

        initializeViews();
        createStages();
        loadStage(currentStage);
    }

    private void initializeViews() {

        crosswordGrid = findViewById(R.id.crosswordGrid);

        tvStage = findViewById(R.id.tvStage);
        tvScore = findViewById(R.id.tvScore);
        tvMessage = findViewById(R.id.tvMessage);
        tvQuestion = findViewById(R.id.tvQuestion);

        btnCheck = findViewById(R.id.btnCheck);
        btnHint = findViewById(R.id.btnHint);
        btnNext = findViewById(R.id.btnNext);

        btnCheck.setOnClickListener(v -> checkAnswer());

        btnHint.setOnClickListener(v -> showHint());

        btnNext.setOnClickListener(v -> {

            if (currentStage < stages.size() - 1) {
                currentStage++;
                loadStage(currentStage);
            } else {
                showFinishedDialog();
            }
        });
    }

    // =========================================================
    // المراحل
    // =========================================================

    private void createStages() {

        /*
         * المرحلة الأولى
         */
        stages.add(new Stage(
                "المستوى الأول",
                "شيء نستخدمه لمعرفة الوقت",
                "ساعة",
                100,
                "بداية رائعة! ركّز وستصل للإجابة."
        ));

        /*
         * المرحلة الثانية
         */
        stages.add(new Stage(
                "المستوى الثاني",
                "شيء نقرأ فيه الأخبار والقصص",
                "كتاب",
                150,
                "ممتاز! عقلك بدأ يسخن 🔥"
        ));

        /*
         * المرحلة الثالثة
         */
        stages.add(new Stage(
                "المستوى الثالث",
                "شيء يضيء لنا في الظلام",
                "مصباح",
                200,
                "رائع جدًا! أنت تزداد ذكاءً."
        ));

        /*
         * المرحلة الرابعة
         */
        stages.add(new Stage(
                "المستوى الرابع",
                "ماء متجمد",
                "ثلج",
                250,
                "مذهل! لم يبقَ إلا القليل."
        ));

        /*
         * المرحلة الخامسة
         */
        stages.add(new Stage(
                "المستوى الخامس",
                "الكوكب الذي نعيش عليه",
                "الأرض",
                300,
                "بطل الكلمات المتقاطعة! 🏆"
        ));
    }

    // =========================================================
    // تحميل المرحلة
    // =========================================================

    private void loadStage(int stageIndex) {

        Stage stage = stages.get(stageIndex);

        tvStage.setText(
                "المرحلة " + (stageIndex + 1) +
                        " من " + stages.size()
        );

        tvScore.setText("النقاط: " + score);

        tvQuestion.setText("السؤال:\n" + stage.question);

        tvMessage.setText(stage.message);

        btnNext.setVisibility(View.GONE);

        createCrossword(stage.answer);

        updateScore();
    }

    // =========================================================
    // إنشاء شبكة الكلمات
    // =========================================================

    private void createCrossword(String answer) {

        crosswordGrid.removeAllViews();

        answer = answer.trim();

        int cellSize = getResources()
                .getDisplayMetrics().widthPixels;

        cellSize = Math.min(cellSize - 70, 400);

        int width = cellSize / Math.max(answer.length(), 1);

        crosswordGrid.setColumnCount(answer.length());

        for (int i = 0; i < answer.length(); i++) {

            EditText cell = new EditText(this);

            GridLayout.LayoutParams params =
                    new GridLayout.LayoutParams();

            params.width = width;
            params.height = width;

            params.setMargins(2, 2, 2, 2);

            cell.setLayoutParams(params);

            cell.setGravity(Gravity.CENTER);

            cell.setTextSize(22);

            cell.setTypeface(
                    Typeface.DEFAULT,
                    Typeface.BOLD
            );

            cell.setTextColor(Color.rgb(70, 70, 70));

            cell.setBackgroundResource(
                    android.R.drawable.editbox_background
            );

            cell.setSingleLine(true);

            cell.setInputType(
                    android.text.InputType.TYPE_CLASS_TEXT
            );

            final int index = i;

            cell.setOnFocusChangeListener(
                    (v, hasFocus) -> {

                        if (hasFocus) {
                            selectedCell = (EditText) v;
                        }
                    }
            );

            crosswordGrid.addView(cell);
        }
    }

    // =========================================================
    // التحقق من الإجابة
    // =========================================================

    private void checkAnswer() {

        Stage stage = stages.get(currentStage);

        StringBuilder enteredAnswer =
                new StringBuilder();

        for (int i = 0; i < crosswordGrid.getChildCount(); i++) {

            View view = crosswordGrid.getChildAt(i);

            if (view instanceof EditText) {

                EditText cell = (EditText) view;

                String text =
                        cell.getText().toString().trim();

                if (text.length() == 0) {

                    Toast.makeText(
                            this,
                            "أكمل جميع الحروف أولاً ✍️",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                enteredAnswer.append(text);
            }
        }

        String userAnswer =
                normalizeArabic(enteredAnswer.toString());

        String correctAnswer =
                normalizeArabic(stage.answer);

        if (userAnswer.equals(correctAnswer)) {

            score += stage.points;

            tvScore.setText("النقاط: " + score);

            tvMessage.setText(
                    "🎉 أحسنت!\n" +
                    stage.message +
                    "\n\n+" + stage.points + " نقطة"
            );

            colorCells(Color.rgb(198, 239, 206));

            btnNext.setVisibility(View.VISIBLE);

            Toast.makeText(
                    this,
                    "إجابة صحيحة! 👏",
                    Toast.LENGTH_LONG
            ).show();

        } else {

            tvMessage.setText(
                    "❌ ليست الإجابة الصحيحة.\n" +
                    "حاول مرة أخرى، أنت تستطيع!"
            );

            colorCells(Color.rgb(255, 210, 210));

            Toast.makeText(
                    this,
                    "حاول مرة أخرى 💪",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // =========================================================
    // التلميح
    // =========================================================

    private void showHint() {

        Stage stage = stages.get(currentStage);

        String answer = stage.answer;

        int firstEmpty = -1;

        for (int i = 0; i < crosswordGrid.getChildCount(); i++) {

            View view = crosswordGrid.getChildAt(i);

            if (view instanceof EditText) {

                EditText cell = (EditText) view;

                if (cell.getText()
                        .toString()
                        .trim()
                        .isEmpty()) {

                    firstEmpty = i;
                    break;
                }
            }
        }

        if (firstEmpty == -1) {

            Toast.makeText(
                    this,
                    "جميع الخانات ممتلئة.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        EditText cell =
                (EditText) crosswordGrid
                        .getChildAt(firstEmpty);

        String letter =
                String.valueOf(answer.charAt(firstEmpty));

        cell.setText(letter);

        Toast.makeText(
                this,
                "تلميح: الحرف " +
                        (firstEmpty + 1) +
                        " هو " +
                        letter,
                Toast.LENGTH_SHORT
        ).show();
    }

    // =========================================================
    // تلوين الخانات
    // =========================================================

    private void colorCells(int color) {

        for (int i = 0;
             i < crosswordGrid.getChildCount();
             i++) {

            View view =
                    crosswordGrid.getChildAt(i);

            if (view instanceof EditText) {

                view.setBackgroundColor(color);
            }
        }
    }

    // =========================================================
    // تنظيف اللغة العربية
    // =========================================================

    private String normalizeArabic(String text) {

        return text
                .replace("أ", "ا")
                .replace("إ", "ا")
                .replace("آ", "ا")
                .replace("ة", "ه")
                .replace("ى", "ي")
                .replace("ـ", "")
                .replace(" ", "")
                .trim()
                .toLowerCase();
    }

    // =========================================================
    // النقاط
    // =========================================================

    private void updateScore() {

        tvScore.setText(
                "النقاط: " + score
        );
    }

    // =========================================================
    // نهاية اللعبة
    // =========================================================

    private void showFinishedDialog() {

        new AlertDialog.Builder(this)
                .setTitle("🏆 تهانينا يا بطل!")
                .setMessage(
                        "لقد أكملت جميع المراحل بنجاح.\n\n" +
                        "مجموع نقاطك: " + score +
                        "\n\n" +
                        "استمر في التحدي وطوّر معلوماتك كل يوم.\n\n" +
                        "برمجة وتطوير: عصام المهدي"
                )
                .setPositiveButton(
                        "إعادة اللعب",
                        (dialog, which) -> {

                            currentStage = 0;
                            score = 0;

                            loadStage(currentStage);
                        }
                )
                .setNegativeButton(
                        "خروج",
                        null
                )
                .show();
    }

    // =========================================================
    // كلاس المرحلة
    // =========================================================

    private static class Stage {

        String title;
        String question;
        String answer;
        int points;
        String message;

        Stage(
                String title,
                String question,
                String answer,
                int points,
                String message
        ) {

            this.title = title;
            this.question = question;
            this.answer = answer;
            this.points = points;
            this.message = message;
        }
    }
}
