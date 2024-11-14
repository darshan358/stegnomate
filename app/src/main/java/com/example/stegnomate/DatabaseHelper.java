package com.example.stegnomate;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "stego.db";
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
    private static final String COLUMN_FILE_NAME = "file_name"; // New column
    private static final String COLUMN_SECRET_MESSAGE = "secret_message"; // New column


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.d("Database Path", context.getDatabasePath(DATABASE_NAME).getAbsolutePath());
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            // Create Users table
            String createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_USERNAME + " TEXT, " +
                    COLUMN_PASSWORD + " TEXT)";
            db.execSQL(createUsersTable);

            // Create Encrypted Files table
            String createEncryptedFilesTable = "CREATE TABLE " + TABLE_ENCRYPTED_FILES + " (" +
                    COLUMN_FILE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_FILE_NAME + " TEXT, " +  // New column
                    COLUMN_SECRET_MESSAGE + " TEXT, " +  // New column
                    COLUMN_ENCRYPTED_DATA + " TEXT, " +
                    COLUMN_SECRET_KEY + " TEXT)";
            db.execSQL(createEncryptedFilesTable);

            Log.d("DatabaseHelper", "Tables created successfully.");
        } catch (SQLException e) {
            Log.e("DatabaseHelper", "Error creating tables: " + e.getMessage());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_ENCRYPTED_FILES);
            onCreate(db);
            Log.d("DatabaseHelper", "Database upgraded successfully.");
        } catch (SQLException e) {
            Log.e("DatabaseHelper", "Error upgrading database: " + e.getMessage());
        }
    }

    public boolean insertUser(String username, String password) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_USERNAME, username);
            values.put(COLUMN_PASSWORD, password);
            long result = db.insert(TABLE_USERS, null, values);
            if (result == -1) {
                Log.e("DatabaseHelper", "Failed to insert user.");
                return false;
            } else {
                Log.d("DatabaseHelper", "User inserted successfully.");
                return true;
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error inserting user: " + e.getMessage());
            return false;
        } finally {
            if (db != null && db.isOpen()) {
                db.close();  // Ensure the database connection is closed
                Log.d("DatabaseHelper", "Database connection closed after insert.");
            }
        }
    }

    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_USERS + " WHERE " +
                COLUMN_USERNAME + "=? AND " + COLUMN_PASSWORD + "=?";
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{username, password});
            return cursor != null && cursor.getCount() > 0;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error checking user: " + e.getMessage());
            return false;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
                Log.d("DatabaseHelper", "Cursor closed after query.");
            }
            if (db != null && db.isOpen()) {
                db.close();  // Ensure the database connection is closed
                Log.d("DatabaseHelper", "Database connection closed after query.");
            }
        }
    }

    public boolean storeEncryptedFile(String fileName, String secretMessage, File encryptedData, String secretKey) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_FILE_NAME, fileName); // Store file name
            values.put(COLUMN_SECRET_MESSAGE, secretMessage); // Store secret message
            values.put(COLUMN_ENCRYPTED_DATA, encryptedData.getAbsolutePath()); // Store file path
            values.put(COLUMN_SECRET_KEY, secretKey);
            long result = db.insert(TABLE_ENCRYPTED_FILES, null, values);
            if (result == -1) {
                Log.e("DatabaseHelper", "Failed to store encrypted file.");
                return false;
            } else {
                Log.d("DatabaseHelper", "Encrypted file stored successfully.");
                return true;
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error storing encrypted file: " + e.getMessage());
            return false;
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
                Log.d("DatabaseHelper", "Database connection closed after storing encrypted file.");
            }
        }
    }


    @SuppressLint("Range")
    public String getEncryptedFilePath() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        String encryptedFilePath = null;
        try {
            db = this.getReadableDatabase();
            String query = "SELECT " + COLUMN_ENCRYPTED_DATA + " FROM " + TABLE_ENCRYPTED_FILES +
                    " ORDER BY " + COLUMN_FILE_ID + " DESC LIMIT 1";
            cursor = db.rawQuery(query, null);
            if (cursor != null && cursor.moveToFirst()) {
                encryptedFilePath = cursor.getString(cursor.getColumnIndex(COLUMN_ENCRYPTED_DATA));
            }
            return encryptedFilePath;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting encrypted file path: " + e.getMessage());
            return null;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
                Log.d("DatabaseHelper", "Cursor closed after query.");
            }
            if (db != null && db.isOpen()) {
                db.close();  // Ensure the database connection is closed
                Log.d("DatabaseHelper", "Database connection closed after query.");
            }
        }
    }
}
