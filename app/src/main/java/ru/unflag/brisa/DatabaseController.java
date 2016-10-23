package ru.unflag.brisa;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseController {
    // database constants
    // schema constants
    private static final String SCHEMA_NAME = "brisa";
    private static final int SCHEMA_VERSION = 1;
    private static final String TBL_MESSAGES = "messages";
    private static final String TBL_MSG_STATUSES = "msg_statuses";

    // messages table column names
    static final String COL_MSG_ID = "id";
    static final String COL_MSG_TIMESTAMP = "timestamp";
    static final String COL_MSG_SUBJECT = "subject";
    static final String COL_MSG_TEXT = "text";
    static final String COL_MSG_LEVEL = "level";
    private static final String COL_MSG_CREATED_AT = "created_at";

    // msg_statuses column names
    private static final String COL_MSTS_ID = "id";
    private static final String COL_MSTS_MSG_ID = "msg_id";
    static final String COL_MSTS_STATUS = "status";
    private static final String COL_MSTS_STATUS_AT = "status_at";

    private static final int MSTS_STATUS_DEFAULT = 0;
    private static final int MSTS_STATUS_READ = 1;

    private static final String TBL_MSGS_CREATE = "CREATE TABLE " + TBL_MESSAGES + "(" +
            COL_MSG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_MSG_TIMESTAMP + " TEXT, " +
            COL_MSG_SUBJECT + " TEXT, " +
            COL_MSG_TEXT + " TEXT, " +
            COL_MSG_LEVEL + " TEXT, " +
            COL_MSG_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP" + ");";

    private static final String TBL_MSG_STS_CREATE = "CREATE TABLE " + TBL_MSG_STATUSES + "(" +
            COL_MSTS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_MSTS_MSG_ID + " INTEGER, " +
            COL_MSTS_STATUS + " INTEGER DEFAULT " + MSTS_STATUS_DEFAULT + ", " +
            COL_MSTS_STATUS_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY" + "(" + COL_MSTS_MSG_ID + ")" + " REFERENCES " + TBL_MESSAGES + "(" + COL_MSG_ID + "));";

    private final Context context;
    private DatabaseAccessor databaseAccessor;
    private SQLiteDatabase sqLiteDatabase;

    DatabaseController(Context context) {
        this.context = context;
    }

    public void open() {
        databaseAccessor = new DatabaseAccessor(context, SCHEMA_NAME, null, SCHEMA_VERSION);
        sqLiteDatabase = databaseAccessor.getWritableDatabase();
    }

    void close() {
        if (databaseAccessor != null) {
            databaseAccessor.close();
        }
    }

    Cursor getMessages() {
        String query = "SELECT " + "MSG.ROWID AS _id, " +
                "MSG." + COL_MSG_ID + ", " +
                "MSG." + COL_MSG_TIMESTAMP + ", " +
                "MSG." + COL_MSG_SUBJECT + ", " +
                "MSG." + COL_MSG_TEXT + ", " +
                "MSG." + COL_MSG_LEVEL + ", " +
                "MAX(" + "MST." + COL_MSTS_STATUS + ") AS " + COL_MSTS_STATUS +
                " FROM " + TBL_MESSAGES + " AS MSG " +
                "INNER JOIN " + TBL_MSG_STATUSES + " AS MST ON " + "MSG." + COL_MSG_ID + " = " + "MST." + COL_MSTS_MSG_ID +
                " GROUP BY " + "MSG." + COL_MSG_ID + ", " +
                "MSG." + COL_MSG_TIMESTAMP + ", " +
                "MSG." + COL_MSG_SUBJECT + ", " +
                "MSG." + COL_MSG_TEXT + ", " +
                "MSG." + COL_MSG_LEVEL +
                " ORDER BY " + "MSG." + COL_MSG_ID + " DESC;";
        return sqLiteDatabase.rawQuery(query, null);
    }

    Cursor getFilteredMessages(String filter) {
        String query = "SELECT " + "MSG.ROWID AS _id, " +
                "MSG." + COL_MSG_ID + ", " +
                "MSG." + COL_MSG_TIMESTAMP + ", " +
                "MSG." + COL_MSG_SUBJECT + ", " +
                "MSG." + COL_MSG_TEXT + ", " +
                "MSG." + COL_MSG_LEVEL + ", " +
                "MAX(" + "MST." + COL_MSTS_STATUS + ") AS " + COL_MSTS_STATUS +
                " FROM " + TBL_MESSAGES + " AS MSG " +
                "INNER JOIN " + TBL_MSG_STATUSES + " AS MST ON " + "MSG." + COL_MSG_ID + " = " + "MST." + COL_MSTS_MSG_ID +
                " WHERE (" + "MSG." + COL_MSG_SUBJECT + " like '%" + filter + "%'" +
                " OR " + "MSG." + COL_MSG_TEXT + " like '%" + filter + "%')" +
                " GROUP BY " + "MSG." + COL_MSG_ID + ", " +
                "MSG." + COL_MSG_TIMESTAMP + ", " +
                "MSG." + COL_MSG_SUBJECT + ", " +
                "MSG." + COL_MSG_TEXT + ", " +
                "MSG." + COL_MSG_LEVEL +
                " ORDER BY " + "MSG." + COL_MSG_ID + " DESC;";
        Log.d(MessagesActivity.LOG_TAG, query);
        return sqLiteDatabase.rawQuery(query, null);
    }

    public void addMessage(String timestamp, String subject, String text, String level) {
        ContentValues values = new ContentValues();
        long msg_id;
        sqLiteDatabase.beginTransaction();
        try {
            // filling values for message insert
            values.put(COL_MSG_TIMESTAMP, timestamp);
            values.put(COL_MSG_SUBJECT, subject);
            values.put(COL_MSG_TEXT, text);
            values.put(COL_MSG_LEVEL, level);

            msg_id = sqLiteDatabase.insert(TBL_MESSAGES, null, values);

            // filling values for default message status insert
            values.clear();
            values.put(COL_MSTS_MSG_ID, msg_id);

            sqLiteDatabase.insert(TBL_MSG_STATUSES, null, values);
            sqLiteDatabase.setTransactionSuccessful();
        } finally {
            sqLiteDatabase.endTransaction();
        }
    }

    void setRead(long id) {
        String query = "SELECT MAX(" + COL_MSTS_STATUS + ") AS " + COL_MSTS_STATUS + " FROM " + TBL_MSG_STATUSES + " where " + COL_MSTS_MSG_ID + " = " + id + ";";
        ContentValues values = new ContentValues();
        Cursor statusCursor = sqLiteDatabase.rawQuery(query, null);

        if (statusCursor.moveToFirst()) {
            int currentStatus = statusCursor.getInt(statusCursor.getColumnIndex(COL_MSTS_STATUS));
            if (currentStatus == MSTS_STATUS_DEFAULT) {
                sqLiteDatabase.beginTransaction();
                try {
                    values.put(COL_MSTS_MSG_ID, id);
                    values.put(COL_MSTS_STATUS, MSTS_STATUS_READ);
                    sqLiteDatabase.insert(TBL_MSG_STATUSES, null, values);
                    sqLiteDatabase.setTransactionSuccessful();
                } finally {
                    sqLiteDatabase.endTransaction();
                }
            }
        }
        statusCursor.close();
    }

    void deleteMessage(long id) {
        sqLiteDatabase.beginTransaction();
        try {
            sqLiteDatabase.delete(TBL_MESSAGES, COL_MSG_ID + " = " + id, null);
            sqLiteDatabase.delete(TBL_MSG_STATUSES, COL_MSTS_MSG_ID + "=" + id, null);
            sqLiteDatabase.setTransactionSuccessful();
        } finally {
            sqLiteDatabase.endTransaction();
        }
    }

    private class DatabaseAccessor extends SQLiteOpenHelper {
        DatabaseAccessor(Context context, String schema, SQLiteDatabase.CursorFactory cursorFactory, int version) {
            super(context, schema, cursorFactory, version);
        }

        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            ContentValues values = new ContentValues();
            sqLiteDatabase.execSQL(TBL_MSGS_CREATE);
            sqLiteDatabase.execSQL(TBL_MSG_STS_CREATE);

            // filling db with test values
            for (int i = 0; i < 10; i++) {
                long msg_id;
                values.put(COL_MSG_TIMESTAMP, "2016/08/23 0" + i + ":00:00");
                values.put(COL_MSG_SUBJECT, "Subject number " + i);
                values.put(COL_MSG_TEXT, "Very long long long long long long long long long long long long long long long long long long long long long long long long long long long text number " + i);
                if (i % 4 == 0) {
                    values.put(COL_MSG_LEVEL, "OK");
                }
                else if (i % 3 == 0) {
                    values.put(COL_MSG_LEVEL, "CRITICAL");
                }
                else {
                    values.put(COL_MSG_LEVEL, "WARNING");
                }

                msg_id = sqLiteDatabase.insert(TBL_MESSAGES, null, values);
                values.clear();
                values.put(COL_MSTS_MSG_ID, msg_id);
                values.put(COL_MSTS_STATUS, 0);
                sqLiteDatabase.insert(TBL_MSG_STATUSES, null, values);
                values.clear();
            }
        }

        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
            if (oldVersion == 1) {
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TBL_MESSAGES);
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TBL_MSG_STATUSES);
                onCreate(sqLiteDatabase);
            }
        }
    }
}
