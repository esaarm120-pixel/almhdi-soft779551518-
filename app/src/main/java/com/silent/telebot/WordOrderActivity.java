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

    private TextView levelText;
    private TextView scoreText;
    private TextView difficultyText;
    private TextView motivationText;
    private TextView answerText;
    private TextView crosswordStatus;

    private LinearLayout scramblePanel;
    private LinearLayout crosswordPanel;
    private LinearLayout lettersRow;
    private GridLayout crosswordGrid;

    private EditText[][] cells = new EditText[5][5];
    private final List<String> selectedLetters = new ArrayList<>();

    private int level = 1;
    private int score = 0;
    private int attempts = 3;
    private int wordIndex = 0;

    private final String[] words = {
            "كتاب",
            "شجرة",
            "مدرسة",
            "حديقة",
            "مكتبة",
            "سيارة",
            "تفاحة",
            "نجاحك",
            "مغامر",
            "عبقري",
            "مبدع",
            "انتصار"
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
            "بداية ممتازة يا PLAYER!",
            "أحسنت يا PLAYER، تركيزك رائع!",
            "استمر يا PLAYER، أنت تتقدم بسرعة!",
            "مذهل يا PLAYER، المرحلة القادمة أصعب!",
            "عقلك يعمل كالأبطال يا PLAYER!",
            "اقتربت من القمة يا PLAYER!",
            "لا تستسلم يا PLAYER!",
            "أداء قوي يا PLAYER!",
            "رائع يا PLAYER، لم يبقَ إلا القليل!",
            "هذه روح المحترفين يا PLAYER!",
            "استثنائي يا PLAYER!",
            "أسطورة يا PLAYER، أكملت جميع المراحل!"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_crossword);

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

        findViewById(R.id.scrambleModeButton)
                .setOnClickListener(v -> showMode(true));

        findViewById(R.id.crosswordModeButton)
                .setOnClickListener(v -> showMode(false));

        findViewById(R.id.scrambleCheckButton)
                .setOnClickListener(v -> checkWord());

        findViewById(R.id.scrambleNewButton)
                .setOnClickListener(v -> loadWord());

        findViewById(R.id.crosswordCheckButton)
                .setOnClickListener(v -> checkCrossword());

        findViewById(R.id.crosswordClearButton)
                .setOnClickListener(v -> clearCrossword());

        updateHeader();
        loadWord();
        buildCrossword();
        showMode(true);
    }

    private void updateHeader() {
        levelText.setText("المرحلة " + level + " من " + MAX_LEVEL);
        scoreText.setText("النقاط: " + score);
        difficultyText.setText("الصعوبة: " + getDifficulty());

        String message = messages[level - 1]
                .replace("PLAYER", PLAYER);

        motivationText.setText(message);
    }

    private String 
