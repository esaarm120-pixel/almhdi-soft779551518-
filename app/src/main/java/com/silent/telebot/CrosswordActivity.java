package com.silent.telebot;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class CrosswordActivity extends AppCompatActivity {

    private GridLayout crosswordGrid;
    private TextView tvClueDisplay, tvLevelTitle;
    private Button btnCheckAnswers;
    
    // حجم شبكة اللعبة (مثلاً 8 أعمدة في 8 صفوف)
    private static final int GRID_SIZE = 8;
    private EditText[][] cellMatrix = new EditText[GRID_SIZE][GRID_SIZE];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crossword);

        crosswordGrid = findViewById(R.id.crosswordGrid);
        tvClueDisplay = findViewById(R.id.tvClueDisplay);
        tvLevelTitle = findViewById(R.id.tvLevelTitle);
        btnCheckAnswers = findViewById(R.id.btnCheckAnswers);

        // بناء الشبكة بصرياً (خلايا إدخال للحروف)
        buildGridUI();

        // زر التحقق
        btnCheckAnswers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkUserSolution();
            }
        });
    }

    private void buildGridUI() {
        if (crosswordGrid == null) return;
        crosswordGrid.removeAllViews();
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                EditText cell = new EditText(this);
                cell.setLayoutParams(new GridLayout.LayoutParams(
                        GridLayout.spec(row),
                        GridLayout.spec(col)
                ));
                cell.setWidth(110);
                cell.setHeight(110);
                cell.setTextSize(18);
                cell.setGravity(android.view.Gravity.CENTER);
                cell.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(1)});
                cell.setBackgroundResource(android.R.drawable.editbox_background_normal);
                
                cellMatrix[row][col] = cell;
                crosswordGrid.addView(cell);
            }
        }
        
        if (tvClueDisplay != null) {
            tvClueDisplay.setText("أفقي: عاصمة عربية عريقة (4 أحرف)");
        }
    }

    private void checkUserSolution() {
        Toast.makeText(this, "جاري التحقق من الحروف...", Toast.LENGTH_SHORT).show();
    }
}
