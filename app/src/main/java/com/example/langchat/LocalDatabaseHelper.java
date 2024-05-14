package com.example.langchat;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.Context;
import android.util.Log;


import android.content.ContentValues;

import java.util.ArrayList;


public class LocalDatabaseHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;

    private static LocalDatabaseHelper instance;

    private static final String DATABASE_NAME = "LangChat";

    public LocalDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized LocalDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new LocalDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE_TRANSLATIONS = "CREATE TABLE IF NOT EXISTS translations (id INTEGER PRIMARY KEY AUTOINCREMENT, messageId INTEGER, language VARCHAR(32), message VARCHAR(255))";
        db.execSQL(CREATE_TABLE_TRANSLATIONS);
        Log.d("DatabaseHelper", "Database created successfully");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS translations");

        // Create tables again
        onCreate(db);
    }


    @SuppressLint("Range")
    public String retrieveTranslatedMessage(int message_id, String language) {
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM translations WHERE messageId=? AND language=? LIMIT 1", new String[]{String.valueOf(message_id), language});
        String message = null;
        if (cursor != null && cursor.moveToFirst()) {
            message = cursor.getString(cursor.getColumnIndex("message"));
        }

        if (cursor != null) {
            cursor.close();
        }
        return message;
    }

    public Boolean saveTranslation(int message_id, String language, String message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put("messageId", message_id);
        contentValues.put("language", language);
        contentValues.put("message", message);

        long result = db.insert("translations", null, contentValues);

        if (result != -1) {
            Log.d("DatabaseHelper", "Added translation successfully");
        }

        return result != -1;
    }


}