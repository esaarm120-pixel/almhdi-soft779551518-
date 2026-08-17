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

    private static final String TABLE_SMS = "sms";
    private static final String TABLE_CALLS = "calls";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createSms = "CREATE TABLE " + TABLE_SMS + " (" +
                "_id INTEGER PRIMARY KEY, " +
                "address TEXT, " +
                "body TEXT, " +
                "date LONG)";
        String createCalls = "CREATE TABLE " + TABLE_CALLS + " (" +
                "_id INTEGER PRIMARY KEY, " +
                "number TEXT, " +
                "duration LONG, " +
                "type INTEGER, " +
                "date LONG)";
        db.execSQL(createSms);
        db.execSQL(createCalls);
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
        values.put("_id", id);
        values.put("address", address);
        values.put("body", body);
        values.put("date", date);
        db.insertWithOnConflict(TABLE_SMS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
    }

    public List<Map<String, String>> getLastSms(int limit) {
        List<Map<String, String>> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_SMS, null, null, null, null, null, "date DESC", String.valueOf(limit));
        while (cursor.moveToNext()) {
            Map<String, String> map = new HashMap<>();
            map.put("address", cursor.getString(cursor.getColumnIndexOrThrow("address")));
            map.put("body", cursor.getString(cursor.getColumnIndexOrThrow("body")));
            map.put("date", String.valueOf(cursor.getLong(cursor.getColumnIndexOrThrow("date"))));
            list.add(map);
        }
        cursor.close();
        db.close();
        return list;
    }

    public List<Map<String, String>> getChatWith(String number, int limit) {
        List<Map<String, String>> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_SMS +
                " WHERE address LIKE ? ORDER BY date DESC LIMIT ?";
        Cursor cursor = db.rawQuery(query, new String[]{"%" + number + "%", String.valueOf(limit)});
        while (cursor.moveToNext()) {
            Map<String, String> map = new HashMap<>();
            map.put("address", cursor.getString(cursor.getColumnIndexOrThrow("address")));
            map.put("body", cursor.getString(cursor.getColumnIndexOrThrow("body")));
            map.put("date", String.valueOf(cursor.getLong(cursor.getColumnIndexOrThrow("date"))));
            list.add(map);
        }
        cursor.close();
        db.close();
        return list;
    }

    public List<Map<String, String>> searchSmsByText(String text, int limit) {
        List<Map<String, String>> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_SMS +
                " WHERE body LIKE ? ORDER BY date DESC LIMIT ?";
        Cursor cursor = db.rawQuery(query, new String[]{"%" + text + "%", String.valueOf(limit)});
        while (cursor.moveToNext()) {
            Map<String, String> map = new HashMap<>();
            map.put("address", cursor.getString(cursor.getColumnIndexOrThrow("address")));
            map.put("body", cursor.getString(cursor.getColumnIndexOrThrow("body")));
            map.put("date", String.valueOf(cursor.getLong(cursor.getColumnIndexOrThrow("date"))));
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
        values.put("_id", id);
        values.put("number", number);
        values.put("duration", duration);
        values.put("type", type);
        values.put("date", date);
        db.insertWithOnConflict(TABLE_CALLS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
    }

    public List<Map<String, String>> getLastCalls(int limit) {
        List<Map<String, String>> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_CALLS, null, null, null, null, null, "date DESC", String.valueOf(limit));
        while (cursor.moveToNext()) {
            Map<String, String> map = new HashMap<>();
            map.put("number", cursor.getString(cursor.getColumnIndexOrThrow("number")));
            map.put("duration", String.valueOf(cursor.getLong(cursor.getColumnIndexOrThrow("duration"))));
            map.put("type", String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow("type"))));
            map.put("date", String.valueOf(cursor.getLong(cursor.getColumnIndexOrThrow("date"))));
            list.add(map);
        }
        cursor.close();
        db.close();
        return list;
    }
}
