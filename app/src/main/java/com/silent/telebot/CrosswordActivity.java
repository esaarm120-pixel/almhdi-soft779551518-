package com.silent.telebot;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
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

        btnNext.setOnClickListener(v -> nextStage());
    }

    private void createStages() {

        stages.add(new Stage(
                "المستوى الأول",
                "شيء نستخدمه لمعرفة الوقت",
                "ساعة",
                100,
                "بداية رائعة! ركز وستصل للإجابة."
        ));

        stages.add(new Stage(
                "المستوى الثاني",
                "شيء نقرأ فيه الأخبار والقصص",
                "كتاب",
                150,
                "ممتاز! عقلك بدأ يعمل بقوة."
        ));

        stages.add(new Stage(
                "المستوى الثالث",
                "شيء يضيء لنا في الظلام",
                "مصباح",
                200,
                "رائع جداً! أنت تتقدم بسرعة."
        ));

        stages.add(new Stage(
                "المستوى الرابع",
                "ماء متجمد",
                "ثلج",
                250,
                "مذهل! اقتربت من النهاية."
        ));

        stages.add(new Stage(
                "المستوى الخامس",
                "الكوكب الذي نعيش عليه",
                "الأرض",
                300,
                "أحسنت! أنت بطل الكلمات المتقاطعة."
        ));

        stages.add(new Stage(
                "المستوى السادس",
                "النجم الذي يضيء الأرض نهاراً",
                "الشمس",
                350,
                "معلوماتك ممتازة! تابع."
        ));

        stages.add(new Stage(
                "المستوى السابع",
                "حيوان يسمى ملك الغابة",
                "الأسد",
                400,
                "رائع! وصلت إلى مستوى متقدم."
        ));

        stages.add(new Stage(
                "المستوى الثامن",
                "شيء نستخدمه للكتابة",
                "قلم",
                450,
                "ممتاز جداً! لا تتوقف."
        ));

        stages.add(new Stage(
                "المستوى التاسع",
                "عكس كلمة ليل",
                "نهار",
                500,
                "أحسنت! مستوى رائع."
        ));

        stages.add(new Stage(
                "المستوى العاشر",
                "مكان نتعلم فيه",
                "مدرسة",
                600,
                "وصلت إلى المستوى الأخير!"
        ));
    }

    private void loadStage(int stageIndex) {

        if (stageIndex < 0 || stageIndex >= stages.size()) {
            return;
        }

        Stage stage = stages.get(stageIndex);

        tvStage.setText(
                "المرحلة " +
                        (stageIndex + 1) +
                        " / " +
                        stages.size()
        );

        tvScore.setText("النقاط: " + score);

        tvQuestion.setText(
                "السؤال:\n" + stage.question
        );

        tvMessage.setText(stage.message);

        btnNext.setVisibility(View.GONE);

        createCrossword(stage.answer);
    }

    private void createCrossword(String answer) {

        crosswordGrid.removeAllViews();

        String cleanAnswer = normalizeArabic(answer);

        if (cleanAnswer.isEmpty()) {
            return;
        }

        crosswordGrid.setColumnCount(cleanAnswer.length());

        int screenWidth =
                getResources()
                        .getDisplayMetrics()
                        .widthPixels;

        int availableWidth = screenWidth - 70;

        int cellSize =
                availableWidth / cleanAnswer.length();

        cellSize = Math.min(cellSize, 75);

        for (int i = 0; i < cleanAnswer.length(); i++) {

            EditText cell = new EditText(this);

            GridLayout.LayoutParams params =
                    new GridLayout.LayoutParams();

            params.width = cellSize;
            params.height = cellSize;

            params.setMargins(2, 2, 2, 2);

            cell.setLayoutParams(params);

            cell.setGravity(Gravity.CENTER);

            cell.setTextSize(21);

            cell.setTypeface(
                    Typeface.DEFAULT,
                    Typeface.BOLD
            );

            cell.setTextColor(
                    Color.rgb(50, 50, 50)
            );

            cell.setSingleLine(true);

            cell.setMaxLines(1);

            cell.setInputType(
                    InputType.TYPE_CLASS_TEXT
            );

            crosswordGrid.addView(cell);
        }
    }

    private void checkAnswer() {

        Stage stage = stages.get(currentStage);

        StringBuilder userAnswer =
                new StringBuilder();

        for (int i = 0;
             i < crosswordGrid.getChildCount();
             i++) {

            View view =
                    crosswordGrid.getChildAt(i);

            if (!(view instanceof EditText)) {
                continue;
            }

            EditText cell =
                    (EditText) view;

            String letter =
                    cell.getText()
                            .toString()
                            .trim();

            if (letter.isEmpty()) {

                Toast.makeText(
                        this,
                        "أكمل جميع الحروف أولاً",
                        Toast.LENGTH_SHORT
                ).show();

                cell.requestFocus();

                return;
            }

            userAnswer.append(letter);
        }

        String entered =
                normalizeArabic(userAnswer.toString());

        String correct =
                normalizeArabic(stage.answer);

        if (entered.equals(correct)) {

            score += stage.points;

            tvScore.setText(
                    "النقاط: " + score
            );

            tvMessage.setText(
                    "إجابة صحيحة!\n\n" +
                            stage.message +
                            "\n\n+" +
                            stage.points +
                            " نقطة"
            );

            colorCells(
                    Color.rgb(200, 240, 205)
            );

            disableCells();

            btnNext.setVisibility(View.VISIBLE);

            Toast.makeText(
                    this,
                    "أحسنت! إجابة صحيحة",
                    Toast.LENGTH_LONG
            ).show();

        } else {

            tvMessage.setText(
                    "الإجابة غير صحيحة\n\n" +
                            "لا تستسلم، حاول مرة أخرى"
            );

            colorCells(
                    Color.rgb(255, 220, 220)
            );

            Toast.makeText(
                    this,
                    "حاول مرة أخرى",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void showHint() {

        Stage stage = stages.get(currentStage);

        String answer =
                normalizeArabic(stage.answer);

        for (int i = 0;
             i < crosswordGrid.getChildCount();
             i++) {

            View view =
                    crosswordGrid.getChildAt(i);

            if (!(view instanceof EditText)) {
                continue;
            }

            EditText cell =
                    (EditText) view;

            if (cell.getText()
                    .toString()
                    .trim()
                    .isEmpty()) {

                cell.setText(
                        String.valueOf(
                                answer.charAt(i)
                        )
                );

                Toast.makeText(
                        this,
                        "تم كشف أحد الحروف",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }
        }

        Toast.makeText(
                this,
                "جميع الحروف موجودة بالفعل",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void nextStage() {

        if (currentStage < stages.size() - 1) {

            currentStage++;

            loadStage(currentStage);

        } else {

            showFinishedDialog();
        }
    }

    private void disableCells() {

        for (int i = 0;
             i < crosswordGrid.getChildCount();
             i++) {

            View view =
                    crosswordGrid.getChildAt(i);

            if (view instanceof EditText) {
                view.setEnabled(false);
            }
        }
    }

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

    private String normalizeArabic(String text) {

        return text
                .replace("أ", "ا")
                .replace("إ", "ا")
                .replace("آ", "ا")
                .replace("ة", "ه")
                .replace("ى", "ي")
                .replace("ـ", "")
                .replace(" ", "")
                .replace("\n", "")
                .replace("\r", "")
                .trim()
                .toLowerCase();
    }

    private void showFinishedDialog() {

        new AlertDialog.Builder(this)
                .setTitle("تهانينا!")
                .setMessage(
                        "لقد أكملت جميع مراحل الكلمات المتقاطعة بنجاح!\n\n" +
                                "مجموع نقاطك: " +
                                score +
                                "\n\n" +
                                "استمر في التعلم والتحدي كل يوم.\n\n" +
                                "برمجة وتطوير: عصام المهدي"
                )
                .setCancelable(false)
                .setPositiveButton(
                        "إعادة اللعب",
                        (dialog, which) -> {

                            currentStage = 0;
                            score = 0;

                            loadStage(currentStage);
                        }
                )
                .setNegativeButton(
                        "إغلاق",
                        null
                )
                .show();
    }

    private static class Stage {

        final String title;
        final String question;
        final String answer;
        final int points;
        final String message;

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
