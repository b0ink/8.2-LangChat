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
    private static final int DATABASE_VERSION = 2;

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
        String CREATE_TABLE_MESSAGE_RECEIPTS = "CREATE TABLE IF NOT EXISTS message_receipts (id INTEGER PRIMARY KEY AUTOINCREMENT, conversation_id INTEGER, last_message_read_id INTEGER)";
        db.execSQL(CREATE_TABLE_MESSAGE_RECEIPTS);
        Log.d("DatabaseHelper", "Database created successfully");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS message_receipts");

        // Create tables again
        onCreate(db);
    }


    @SuppressLint("Range")
    public int getLastReadMessage(int conversation_id) {
        SQLiteDatabase db = this.getWritableDatabase();

        int last_message_read_id = -1;
        Cursor cursor = db.rawQuery("SELECT * FROM message_receipts WHERE conversation_id=? LIMIT 1", new String[]{String.valueOf(conversation_id)});
        if (cursor != null && cursor.moveToFirst()) {
            last_message_read_id = cursor.getInt(cursor.getColumnIndex("last_message_read_id"));
        }

        if (cursor != null) {
            cursor.close();
        }
        return last_message_read_id;
    }

    public Boolean saveLastReadMessage(int conversation_id, int message_id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put("last_message_read_id", message_id);

        long result = db.update("message_receipts", contentValues, "conversation_id=?", new String[]{String.valueOf(conversation_id)});

        if (result != -1) {
            Log.d("DatabaseHelper", "Added translation successfully");
        }

        return result != -1;
    }



}