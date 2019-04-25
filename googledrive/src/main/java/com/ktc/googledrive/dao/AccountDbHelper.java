package com.ktc.googledrive.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AccountDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "GoogleAccount.db";
    public static final String SQL_CREATE_TABLE="CREATE TABLE "+AccountEntry.TABLE_NAME+" ("+AccountEntry.SUB+" TEXT PRIMARY KEY,"
            +AccountEntry.NAME+" TEXT,"+AccountEntry.PICTURE+" TEXT,"+AccountEntry.REFRESH_TOKEN+" TEXT)";

    private static final String SQL_DELETE_TABLE =
            "DROP TABLE IF EXISTS " + AccountEntry.TABLE_NAME;
    public AccountDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_TABLE);
        onCreate(db);
    }
}
