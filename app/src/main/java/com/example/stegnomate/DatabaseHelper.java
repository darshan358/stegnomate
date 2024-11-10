package com.example.stegnomate;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "stegno.db";
    private static final int DATABASE_VERSION = 1;

    // User table constants
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PASSWORD = "password";

    // Encrypted files table constants
    private static final String TABLE_ENCRYPTED_FILES = "encrypted_files";
    private static final String COLUMN_FILE_ID = "file_id";
    private static final String COLUMN_ENCRYPTED_DATA = "encrypted_data";
    private static final String COLUMN_SECRET_KEY = "secret_key";



    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Users table
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USERNAME + " TEXT, " +
                COLUMN_PASSWORD + " TEXT)";
        db.execSQL(createUsersTable);

        // Create Encrypted Files table
        String createEncryptedFilesTable = "CREATE TABLE " + TABLE_ENCRYPTED_FILES + "(" +
                COLUMN_FILE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_ENCRYPTED_DATA + " BLOB, " +
                COLUMN_SECRET_KEY + " TEXT)";
        db.execSQL(createEncryptedFilesTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older tables if they exist
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ENCRYPTED_FILES);
        onCreate(db);
    }

    public boolean insertUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_PASSWORD, password);
        long result = db.insert(TABLE_USERS, null, values);
        return result != -1; // Returns true if insert was successful
    }

    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_USERS + " WHERE " +
                COLUMN_USERNAME + "=? AND " + COLUMN_PASSWORD + "=?";
        Cursor cursor = db.rawQuery(query, new String[]{username, password});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists; // Returns true if user exists
    }

    public boolean storeEncryptedFile(File encryptedData, String secretKey) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_ENCRYPTED_DATA, encryptedData.getAbsolutePath()); // Store file path as a String
        values.put(COLUMN_SECRET_KEY, secretKey); // Store encryption key for reference

        long result = db.insert(TABLE_ENCRYPTED_FILES, null, values);

        return result != -1;
    }


    public byte[] getEncryptedFile() {
        SQLiteDatabase db = this.getReadableDatabase();

        // Query to retrieve the most recent encrypted file
        String query = "SELECT " + COLUMN_ENCRYPTED_DATA + " FROM " + TABLE_ENCRYPTED_FILES +
                " ORDER BY " + COLUMN_FILE_ID + " DESC LIMIT 1";
        Cursor cursor = db.rawQuery(query, null);

        byte[] encryptedData = null;

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(COLUMN_ENCRYPTED_DATA);

                    if (columnIndex != -1) { // Check if the column index is valid
                        encryptedData = cursor.getBlob(columnIndex);
                    } else {
                        Log.e("DatabaseHelper", "Column index for " + COLUMN_ENCRYPTED_DATA + " is invalid.");
                    }
                }
            } catch (Exception e) {
                Log.e("DatabaseHelper", "Error retrieving encrypted file: ", e);
            } finally {
                cursor.close();
            }
        } else {
            Log.e("DatabaseHelper", "Cursor is null. No data retrieved.");
        }

        return encryptedData; // Return the encrypted data or null if not found
    }

}
