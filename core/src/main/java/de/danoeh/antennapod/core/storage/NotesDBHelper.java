package de.danoeh.antennapod.core.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class NotesDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "notesDatabase";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_NAME = "notes";
    private static final String COLUMN_ID = "id";

    private static final String MEDIA_ID = "media_id";
    private static final String COLUMN_WORD = "word";
    private static final String COLUMN_TRANSLATION = "translation";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_UPDATED_AT = "updated_at";
    private static final String COLUMN_REVIEWED_AT = "reviewed_at";
    private static final String COLUMN_DIFFICULTY_LEVEL = "difficulty_level";

    public NotesDBHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableStatement = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                MEDIA_ID + " INTEGER, " +
                COLUMN_WORD + " TEXT UNIQUE, " +
                COLUMN_TRANSLATION + " TEXT, " +
                COLUMN_CREATED_AT + " INTEGER, " +
                COLUMN_UPDATED_AT + " INTEGER, " +
                COLUMN_REVIEWED_AT + " INTEGER, " +
                COLUMN_DIFFICULTY_LEVEL + " INTEGER)";

        db.execSQL(createTableStatement);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public WordNote addWord(int mediaId, String word, String translation, int difficultyLevel) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        long now = (long) System.currentTimeMillis()/1000;
        cv.put(MEDIA_ID, mediaId);
        cv.put(COLUMN_WORD, word);
        cv.put(COLUMN_TRANSLATION, translation);
        cv.put(COLUMN_CREATED_AT, now);
        cv.put(COLUMN_UPDATED_AT, now);
        cv.put(COLUMN_REVIEWED_AT, now);
        cv.put(COLUMN_DIFFICULTY_LEVEL, difficultyLevel);

        long insert = db.insert(TABLE_NAME, null, cv);
        if(insert != -1){
            WordNote newNote = new WordNote();
            newNote.note_id = (int)insert;
            newNote.media_id = mediaId;
            newNote.word = word;
            newNote.translation = translation;
            newNote.created_at = now;
            newNote.updated_at = now;
            newNote.reviewed_at = now;
            newNote.difficulty_level = difficultyLevel;
            return newNote;
        }else{
            return null;
        }
    }

    public boolean deleteWord(int noteId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String queryString = "DELETE FROM " + TABLE_NAME + " WHERE " + COLUMN_ID + " = ?";
        Cursor cursor = db.rawQuery(queryString, new String[]{noteId + ""});
        if (cursor.moveToFirst()) {
            cursor.close();
            return true;
        } else {
            cursor.close();
            return false;
        }
    }

    public boolean deleteWord(String word) {
        SQLiteDatabase db = this.getWritableDatabase();
        String queryString = "DELETE FROM " + TABLE_NAME + " WHERE " + COLUMN_WORD + " = ?";
        Cursor cursor = db.rawQuery(queryString, new String[]{word});

        if (cursor.moveToFirst()) {
            cursor.close();
            return true;
        } else {
            cursor.close();
            return false;
        }
    }

    public boolean updateWordReviewTime(String word) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        long reviewedAt = (long) System.currentTimeMillis()/1000;
        cv.put(COLUMN_REVIEWED_AT, reviewedAt);

        int update = db.update(TABLE_NAME, cv, COLUMN_WORD + " = ?", new String[]{word});
        Log.d("TranscriptionFragment", "NotesDBHelper to updateWordReviewTime " + word + ", reviewed at " + reviewedAt);
        return update != -1;
    }

    public boolean setWordDifficultyLevel(String word, int difficultyLevel) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_DIFFICULTY_LEVEL, difficultyLevel);

        int update = db.update(TABLE_NAME, cv, COLUMN_WORD + " = ?", new String[]{word});
        return update != -1;
    }

    public ArrayList<WordNote> getWordList(String orderColumn, String sortOrder, int startPosition, int length) {
        ArrayList<WordNote> wordList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String queryString = "SELECT * FROM " + TABLE_NAME +
                " ORDER BY " + orderColumn + " " + sortOrder +
                " LIMIT ? OFFSET ?";

        Cursor cursor = db.rawQuery(queryString, new String[] { String.valueOf(length), String.valueOf(startPosition) });

        if (cursor.moveToFirst()) {
            do {

                WordNote note = new WordNote();
/*
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                MEDIA_ID + " INTEGER, " +
                COLUMN_WORD + " TEXT UNIQUE, " +
                COLUMN_TRANSLATION + " TEXT, " +
                COLUMN_CREATED_AT + " INTEGER, " +
                COLUMN_UPDATED_AT + " INTEGER, " +
                COLUMN_REVIEWED_AT + " INTEGER, " +
                COLUMN_DIFFICULTY_LEVEL + " INTEGER)";
 */
                note.note_id = cursor.getInt(0);
                note.media_id = cursor.getInt(1);
                note.word = cursor.getString(2);
                note.translation = cursor.getString(3);
                note.created_at = cursor.getLong(4);
                note.updated_at = cursor.getLong(5);
                note.reviewed_at = cursor.getLong(6);
                note.difficulty_level = cursor.getInt(7);
                wordList.add(note); // index 1 corresponds to COLUMN_WORD
            } while (cursor.moveToNext());
        }

        cursor.close();
        return wordList;
    }
}
