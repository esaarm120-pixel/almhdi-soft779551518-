package com.silent.telebot;

import android.database.Cursor;
import android.os.Bundle;
import android.view.Gravity;
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

    private static final int GRID_SIZE = 5; // شبكة 5 في 5 متناسقة تماماً
    private EditText[][] cellMatrix = new EditText[GRID_SIZE][GRID_SIZE];

    // بيانات تجريبية حقيقية للمستوى الأول (يمكنك ربطها بقاعدة الـ SQLite الخاصة بك لاحقاً)
    // الكلمة: "صنعاء" (5 حروف)، تبدأ من السطر 1، العمود 0، أفقياً
    private String currentWord = "صنعاء";
    private int startRow = 1;
    private int startCol = 0;
    private boolean isHorizontal = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crossword);

        crosswordGrid = findViewById(R.id.crosswordGrid);
        tvClueDisplay = findViewById(R.id.tvClueDisplay);
        tvLevelTitle = findViewById(R.id.tvLevelTitle);
        btnCheckAnswers = findViewById(R.id.btnCheckAnswers);

        buildGridUI();
        loadLevelData();

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
                GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                        GridLayout.spec(row),
                        GridLayout.spec(col)
                );
                params.width = 120;
                params.height = 120;
                params.setMargins(4, 4, 4, 4);
                cell.setLayoutParams(params);
                
                cell.setTextSize(20);
                cell.setGravity(Gravity.CENTER);
                cell.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(1)});
                cell.setEnabled(false); // مقفلة افتراضياً حتى تنشطها الكلمة المطلوبة
                cell.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));

                cellMatrix[row][col] = cell;
                crosswordGrid.addView(cell);
            }
        }
    }

    private void loadLevelData() {
        // عرض التلميح الحقيقي للكلمة
        tvClueDisplay.setText("أفقي: عاصمة اليمن التاريخية (5 أحرف)");

        // تفعيل مربعات الكلمة المحددة فقط
        for (int i = 0; i < currentWord.length(); i++) {
            int r = startRow;
            int c = startCol;

            if (isHorizontal) {
                c += i;
            } else {
                r += i;
            }

            if (r < GRID_SIZE && c < GRID_SIZE) {
                EditText activeCell = cellMatrix[r][c];
                activeCell.setEnabled(true);
                activeCell.setBackgroundColor(getResources().getColor(android.R.color.white));
                // تخزين الحرف الصحيح خفية للتحقق منه لاحقاً
                activeCell.setTag(String.valueOf(currentWord.charAt(i)));
            }
        }
    }

    private void checkUserSolution() {
        boolean isAllCorrect = true;
        int activeCount = 0;

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                EditText cell = cellMatrix[r][c];
                if (cell.isEnabled()) {
                    activeCount++;
                    String expectedChar = (String) cell.getTag();
                    String userChar = cell.getText().toString().trim();

                    if (expectedChar != null && expectedChar.equalsIgnoreCase(userChar)) {
                        cell.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    } else {
                        isAllCorrect = false;
                        cell.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    }
                }
            }
        }

        if (isAllCorrect && activeCount > 0) {
            Toast.makeText(this, "🎉 إجابة صحيحة 100%! كفو يا عصام", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "⚠️ بعض الحروف غير صحيحة، حاول مجدداً", Toast.LENGTH_SHORT).show();
        }
    }
}
 
