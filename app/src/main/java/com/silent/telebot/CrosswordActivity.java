package com.example.storemanager;

public class CrosswordLevel {
    private int levelNumber;
    private String[] words;       // الكلمات المطلوبة في هذا المستوى
    private String[] clues;       // الأسئلة أو التلميحات لكل كلمة
    private int[][] startRows;    // بداية الصف لكل كلمة
    private int[][] startCols;    // بداية العمود لكل كلمة
    private boolean[] isHorizontal; // هل الكلمة أفقية أم عمودية

    public CrosswordLevel(int levelNumber, String[] words, String[] clues, int[][] startRows, int[][] startCols, boolean[] isHorizontal) {
        this.levelNumber = levelNumber;
        this.words = words;
        this.clues = clues;
        this.startRows = startRows;
        this.startCols = startCols;
        this.isHorizontal = isHorizontal;
    }

    // Getters
    public int getLevelNumber() { return levelNumber; }
    public String[] getWords() { return words; }
    public String[] getClues() { return clues; }
    // يمكنك إضافة الدوال المساعدة حسب الحاجة لتصميم الشبكة
}
