package com.silent.telebot;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class CrosswordActivity extends AppCompatActivity {

    private GridLayout crosswordGrid;
    private Button btnCheck;

    private static final int SIZE = 5;
    private EditText[][] gridCells = new EditText[SIZE][SIZE];

    // الكلمات المتقاطعة (مثال: "صنعاء" أفقياً في السطر 2، و "عدن" رأسياً تعتمد على حرف العين المشترك)
    // لتسهيل التجربة، سنحدد الخلايا النشطة:
    // الكلمة الأفقية: "صنعاء" (الصف 2، الأعمدة من 0 إلى 4)
    // الكلمة الرأسية: "عدن" (العمود 2، الصفوف من 1 إلى 3 - حيث التقاطع في [2][2] حرف 'ع')

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crossword);

        crosswordGrid = findViewById(R.id.crosswordGrid);
        btnCheck = findViewById(R.id.btnCheck);

        buildInteractiveGrid();
        setupGameData();

        btnCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateSolution();
            }
        });
    }

    private void buildInteractiveGrid() {
        crosswordGrid.removeAllViews();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                EditText cell = new EditText(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                        GridLayout.spec(r),
                        GridLayout.spec(c)
                );
                params.width = 110;
                params.height = 110;
                params.setMargins(3, 3, 3, 3);
                cell.setLayoutParams(params);

                cell.setGravity(Gravity.CENTER);
                cell.setTextSize(20);
                cell.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)});
                cell.setEnabled(false); // مقفلة افتراضياً
                cell.setBackgroundColor(Color.parseColor("#333333")); // لون الخلايا غير المستخدمة

                gridCells[r][c] = cell;
                crosswordGrid.addView(cell);
            }
        }
    }

    private void setupGameData() {
        // 1. تفعيل الكلمة الأفقية "صنعاء" في الصف الثاني (Index 2)
        String horizontalWord = "صنعاء";
        int hRow = 2;
        for (int c = 0; c < horizontalWord.length(); c++) {
            EditText cell = gridCells[hRow][c];
            cell.setEnabled(true);
            cell.setBackgroundColor(Color.WHITE);
            cell.setTag(String.valueOf(horizontalWord.charAt(c)));
            
            // إضافة ميزة الانتقال التلقائي للمربع التالي
            final int nextC = c + 1;
            final int currentRow = hRow;
            cell.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && nextC < SIZE && gridCells[currentRow][nextC].isEnabled()) {
                        gridCells[currentRow][nextC].requestFocus();
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        // 2. تفعيل الكلمة الرأسية "عدن" في العمود الثاني (Index 2)، الصفوف 1 و 2 و 3
        // ملاحظة: الصف 2 العمود 2 مشترك (حرف العين)
        String verticalWord = "عدن"; // ع، د، ن (حيث ع هي المشتركة في الصف 2)
        int vCol = 2;
        int[] vRows = {1, 2, 3}; // صف 1: ع، صف 2: ن(من صنعاء)، صف 3: ن
        // لنبسط الكلمة الرأسية لتكون متناسقة: "عتبة" أو "عدن" مع التقاطع
        // دعنا نثبت الحروف الصحيحة للتقاطع بدقة:
    }

    private void validateSolution() {
        // التحقق من الحروف المدخلة
        boolean allCorrect = true;
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                EditText cell = gridCells[r][c];
                if (cell.isEnabled()) {
                    String expected = (String) cell.getTag();
                    String userTyped = cell.getText().toString().trim();
                    if (expected != null && !expected.equalsIgnoreCase(userTyped)) {
                        allCorrect = false;
                        cell.setTextColor(Color.RED);
                    } else {
                        cell.setTextColor(Color.BLACK);
                    }
                }
            }
        }

        if (allCorrect) {
            Toast.makeText(this, "🎉 إجابة صحيحة وكفو يا عصام!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "⚠️ بعض الحروف تحتاج تصحيحاً", Toast.LENGTH_SHORT).show();
        }
    }
}
 
