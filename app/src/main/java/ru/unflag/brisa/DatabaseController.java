package ru.unflag.brisa;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

class DatabaseController {
    // messages table column names
    static final String COL_MSG_ID = "id";
    static final String COL_MSG_TIMESTAMP = "timestamp";
    static final String COL_MSG_SUBJECT = "subject";
    static final String COL_MSG_TEXT = "text";
    static final String COL_MSG_LEVEL = "level";
    static final String COL_MSTS_STATUS = "status";
    // database constants
    // schema constants
    private static final String SCHEMA_NAME = "brisa";
    private static final int SCHEMA_VERSION = 1;
    private static final String TBL_MESSAGES = "messages";
    private static final String TBL_MSG_STATUSES = "msg_statuses";
    private static final String COL_MSG_CREATED_AT = "created_at";
    // msg_statuses column names
    private static final String COL_MSTS_ID = "id";
    private static final String COL_MSTS_MSG_ID = "msg_id";
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
        return sqLiteDatabase.rawQuery(query, null);
    }

    void addMessage(String timestamp, String subject, String text, String level) {
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

    void setAllRead() {
        String getIds = "SELECT " + COL_MSTS_MSG_ID + ", MAX(" + COL_MSTS_STATUS + ") FROM " + TBL_MSG_STATUSES +
                " GROUP BY " + COL_MSTS_MSG_ID + " HAVING MAX(" + COL_MSTS_STATUS + ") = " + MSTS_STATUS_DEFAULT + ";";
        String setRead = "INSERT INTO " + TBL_MSG_STATUSES + " (" + COL_MSTS_MSG_ID + ", " + COL_MSTS_STATUS + ") VALUES (?, " + MSTS_STATUS_READ + ");";
        Cursor unreadCursor = sqLiteDatabase.rawQuery(getIds, null);
        long[] ids = new long[unreadCursor.getCount()];

        if (unreadCursor.moveToFirst()) {
            for (int i = 0; i < unreadCursor.getCount(); i++) {
                ids[i] = unreadCursor.getLong(unreadCursor.getColumnIndex(COL_MSTS_MSG_ID));
                unreadCursor.moveToNext();
            }
            sqLiteDatabase.beginTransaction();
            try {
                SQLiteStatement statement = sqLiteDatabase.compileStatement(setRead);
                for (int i = 0; i < ids.length; i++) {
                    statement.bindLong(1, ids[i]);
                    statement.execute();
                }
                sqLiteDatabase.setTransactionSuccessful();
            } finally {
                sqLiteDatabase.endTransaction();
            }
        }
        unreadCursor.close();
    }

    void deleteMessage(long id) {
        Log.d(MessagesActivity.LOG_TAG, "Deleting Message Id = " + id);
        sqLiteDatabase.beginTransaction();
        try {
            sqLiteDatabase.delete(TBL_MESSAGES, COL_MSG_ID + " = " + id, null);
            sqLiteDatabase.delete(TBL_MSG_STATUSES, COL_MSTS_MSG_ID + "=" + id, null);
            sqLiteDatabase.setTransactionSuccessful();
        } finally {
            sqLiteDatabase.endTransaction();
        }
    }

    void deleteAllMessages() {
        sqLiteDatabase.beginTransaction();
        try {
            sqLiteDatabase.delete(TBL_MESSAGES, null, null);
            sqLiteDatabase.delete(TBL_MSG_STATUSES, null, null);
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
            long msg_id;
            values.put(COL_MSG_TIMESTAMP, "2017/02/17 12:13:20");
            values.put(COL_MSG_SUBJECT, "SERVER0: disk_free_space");
            values.put(COL_MSG_TEXT, "Service: disk_free_space\nHost: server0.dmn\nAddress: 10.34.0.13\nState: WARNING\nDate/Time: 17-02-2017 12:13:20\nAdditional Info:\n/server0/root/logs - 86%");
            values.put(COL_MSG_LEVEL, "WARNING");
            msg_id = sqLiteDatabase.insert(TBL_MESSAGES, null, values);
            values.clear();
            values.put(COL_MSTS_MSG_ID, msg_id);
            values.put(COL_MSTS_STATUS, 0);
            sqLiteDatabase.insert(TBL_MSG_STATUSES, null, values);
            values.clear();
            values.put(COL_MSG_TIMESTAMP, "2017/02/17 12:17:27");
            values.put(COL_MSG_SUBJECT, "SERVER0: disk_free_space");
            values.put(COL_MSG_TEXT, "Service: disk_free_space\nHost: server0.dmn\nAddress: 10.34.0.13\nState: OK\nDate/Time: 17-02-2017 12:17:27\nAdditional Info:\n/server0/root/logs - 46%");
            values.put(COL_MSG_LEVEL, "OK");
            msg_id = sqLiteDatabase.insert(TBL_MESSAGES, null, values);
            values.clear();
            values.put(COL_MSTS_MSG_ID, msg_id);
            values.put(COL_MSTS_STATUS, 0);
            sqLiteDatabase.insert(TBL_MSG_STATUSES, null, values);
            values.clear();
            values.put(COL_MSG_TIMESTAMP, "2017/02/17 12:20:33");
            values.put(COL_MSG_SUBJECT, "SERVER3: disk_free_space");
            values.put(COL_MSG_TEXT, "Service: disk_free_space\nHost: server3.dmn\nAddress: 10.34.0.15\nState: CRITICAL\nDate/Time: 17-02-2017 12:20:33\nAdditional Info:\n/server3/root/logs - 97%");
            values.put(COL_MSG_LEVEL, "CRITICAL");
            msg_id = sqLiteDatabase.insert(TBL_MESSAGES, null, values);
            values.clear();
            values.put(COL_MSTS_MSG_ID, msg_id);
            values.put(COL_MSTS_STATUS, 0);
            sqLiteDatabase.insert(TBL_MSG_STATUSES, null, values);
            values.clear();
            values.put(COL_MSG_TIMESTAMP, "2017/02/17 12:40:41");
            values.put(COL_MSG_SUBJECT, "SERVER3: disk_free_space");
            values.put(COL_MSG_TEXT, "Service: disk_free_space\nHost: server3.dmn\nAddress: 10.34.0.15\nState: WARNING\nDate/Time: 17-02-2017 12:40:31\nAdditional Info:\n/server3/root/logs - 82%");
            values.put(COL_MSG_LEVEL, "WARNING");
            msg_id = sqLiteDatabase.insert(TBL_MESSAGES, null, values);
            values.clear();
            values.put(COL_MSTS_MSG_ID, msg_id);
            values.put(COL_MSTS_STATUS, 0);
            sqLiteDatabase.insert(TBL_MSG_STATUSES, null, values);
            values.clear();
            values.put(COL_MSG_TIMESTAMP, "2017/02/17 12:49:21");
            values.put(COL_MSG_SUBJECT, "SERVER3: disk_free_space");
            values.put(COL_MSG_TEXT, "Service: disk_free_space\nHost: server3.dmn\nAddress: 10.34.0.15\nState: OK\nDate/Time: 17-02-2017 12:49:21\nAdditional Info:\n/server3/root/logs - 54%");
            values.put(COL_MSG_LEVEL, "OK");
            msg_id = sqLiteDatabase.insert(TBL_MESSAGES, null, values);
            values.clear();
            values.put(COL_MSTS_MSG_ID, msg_id);
            values.put(COL_MSTS_STATUS, 0);
            sqLiteDatabase.insert(TBL_MSG_STATUSES, null, values);
            values.clear();
            values.put(COL_MSG_TIMESTAMP, "2017/02/17 12:58:11");
            values.put(COL_MSG_SUBJECT, "SERVER7: free_memory");
            values.put(COL_MSG_TEXT, "Service: free_memory\nHost: server7.dmn\nAddress: 10.36.0.10\nState: WARNING\nDate/Time: 17-02-2017 12:58:11\nAdditional Info:\nfree mem 25.5G including zfs cache 17.9G");
            values.put(COL_MSG_LEVEL, "WARNING");
            msg_id = sqLiteDatabase.insert(TBL_MESSAGES, null, values);
            values.clear();
            values.put(COL_MSTS_MSG_ID, msg_id);
            values.put(COL_MSTS_STATUS, 0);
            sqLiteDatabase.insert(TBL_MSG_STATUSES, null, values);
            values.clear();
            values.put(COL_MSG_TIMESTAMP, "2017/02/17 13:12:23");
            values.put(COL_MSG_SUBJECT, "SERVER7: free_memory");
            values.put(COL_MSG_TEXT, "Service: free_memory\nHost: server7.dmn\nAddress: 10.36.0.10\nState: OK\nDate/Time: 17-02-2017 13:12:23\nAdditional Info:\nfree mem 40.7G including zfs cache 21.9G");
            values.put(COL_MSG_LEVEL, "OK");
            msg_id = sqLiteDatabase.insert(TBL_MESSAGES, null, values);
            values.clear();
            values.put(COL_MSTS_MSG_ID, msg_id);
            values.put(COL_MSTS_STATUS, 0);
            sqLiteDatabase.insert(TBL_MSG_STATUSES, null, values);
            values.clear();
            values.put(COL_MSG_TIMESTAMP, "2017/02/17 13:32:43");
            values.put(COL_MSG_SUBJECT, "SERVER4: load_avg");
            values.put(COL_MSG_TEXT, "Service: load_avg\nHost: server4.dmn\nAddress: 10.34.0.42\nState: CRITICAL\nDate/Time: 17-02-2017 13:32:43\nAdditional Info:\n19.58 (>=19)");
            values.put(COL_MSG_LEVEL, "CRITICAL");
            msg_id = sqLiteDatabase.insert(TBL_MESSAGES, null, values);
            values.clear();
            values.put(COL_MSTS_MSG_ID, msg_id);
            values.put(COL_MSTS_STATUS, 0);
            sqLiteDatabase.insert(TBL_MSG_STATUSES, null, values);
            values.clear();
            values.put(COL_MSG_TIMESTAMP, "2017/02/17 13:57:06");
            values.put(COL_MSG_SUBJECT, "SERVER4: load_avg");
            values.put(COL_MSG_TEXT, "Service: load_avg\nHost: server4.dmn\nAddress: 10.34.0.42\nState: CRITICAL\nDate/Time: 17-02-2017 13:57:07\nAdditional Info:\n18.99");
            values.put(COL_MSG_LEVEL, "OK");
            msg_id = sqLiteDatabase.insert(TBL_MESSAGES, null, values);
            values.clear();
            values.put(COL_MSTS_MSG_ID, msg_id);
            values.put(COL_MSTS_STATUS, 0);
            sqLiteDatabase.insert(TBL_MSG_STATUSES, null, values);
            values.clear();
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
