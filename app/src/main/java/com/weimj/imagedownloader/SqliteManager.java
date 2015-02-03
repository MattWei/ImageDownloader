package com.weimj.imagedownloader;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SqliteManager extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "tumblr";

    private static final int DATABASE_VERSION = 2;
    private static final String TUMBLR_TABLE_NAME = "tumblr";
    private static final String TUMBLR_TABLE_CREATE =
            "CREATE TABLE " + TUMBLR_TABLE_NAME + " (" +
            " _id          INTEGER PRIMARY KEY AUTOINCREMENT, " +
            " TUMBLR_ID	   INTEGER  NOT NULL, " +
            " NAME         TEXT     NOT NULL, " +
            " DATE         DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            " PATH         TEXT     NOT NULL, " +
            " TYPE	       TEXT     NOT NULL, " +
            " SIZE		   INTEGER  NOT NULL )";

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d("SqliteManager", "Create database");
        db.execSQL(TUMBLR_TABLE_CREATE);
    }

    SqliteManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onUpgrade (SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TUMBLR_TABLE_NAME);
        onCreate(db);
    }

    public boolean isInserted(Long tumblrId, String fileName, String type) {
        String sql = "SELECT _id from tumblr " +
                " WHERE TUMBLR_ID = ? AND NAME = ? AND TYPE = ?";

        Cursor cursor = getReadableDatabase().
                rawQuery(sql, new String[] {Long.toString(tumblrId), fileName, type});

        boolean isExits =  cursor.getCount() > 0 ? true : false;
        cursor.close();

        return isExits;

    }

    public boolean insert(TumblrPost post) {
        Log.d("Insert database", "Insert sql:" + post.getInsertSql());

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put("TUMBLR_ID", post.getTumblrId());
        contentValues.put("NAME", post.getFileName());
        contentValues.put("PATH", post.getPath());
        contentValues.put("TYPE", post.getType());
        contentValues.put("SIZE", post.getFileSize());

        long insert = db.insert(DATABASE_NAME, null, contentValues);

        return insert > 0 ? true : false;
    }


    public List<TumblrPost> select(String type, Long sinceId) {
        String sql = "SELECT _id, TUMBLR_ID, NAME, PATH, DATE, TYPE, SIZE from tumblr WHERE " + ""
                + " TYPE = '" + type + "'";

        if (sinceId > 0)
            sql += " AND ID >= '" + sinceId + "'";

        ArrayList<TumblrPost> posts = new ArrayList<TumblrPost>();

        try {
            Cursor cursor = getReadableDatabase().
                    rawQuery(sql, null);
            while (cursor.moveToNext()) {
                TumblrPost post = new TumblrPost();
                post.setId(cursor.getLong(0));
                post.setTumblrId(cursor.getLong(1));
                post.setFileName(cursor.getString(2));
                post.setPath(cursor.getString(3));
                post.setDate(cursor.getString(4));
                post.setType(cursor.getString(5));
                post.setFileSize(cursor.getInt(6));
                posts.add(post);
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }

        return posts;
    }
}
