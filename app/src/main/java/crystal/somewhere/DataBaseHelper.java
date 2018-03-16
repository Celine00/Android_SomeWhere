package crystal.somewhere;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Crystal on 2017/12/18.
 */

public class DataBaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "SomeWhere.db";
    private static final String TABLE_NAME = "SomeWhereTable";
    private static final int DB_VERSION = 1;

    public DataBaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME
                + " (_id INTEGER PRIMARY KEY, "
                + "userName TEXT NOT NULL, "
                + "latitude REAL, "
                + "longitude REAL, "
                + "type TEXT NOT NULL, "
                + "name TEXT NOT NULL, "
                + "description TEXT, "
                + "content TEXT, "
                + "coverPage TEXT);";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    public void insert(String userName, double latitude, double longitude, String type, String name, String description) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("userName", userName);
        values.put("latitude", latitude);
        values.put("longitude", longitude);
        values.put("type", type);
        values.put("name", name);
        values.put("description", description);
        values.put("content", "");
        values.put("coverPage", "");
        db.insert(TABLE_NAME, null, values);
    }

    public void update(double latitude, double longitude, String type, String name, String description, String content, String coverPage) {
        SQLiteDatabase db = getWritableDatabase();
        String whereClause = "latitude = ? AND longitude = ?";
        String[] whereArgs = {Double.toString(latitude), Double.toString(longitude)};
        ContentValues values = new ContentValues();
        System.out.println("in onUpdate, type=="+ type);
        values.put("type", type);
        values.put("name", name);
        values.put("description", description);
        values.put("content", content);
        values.put("coverPage", coverPage);
        db.update(TABLE_NAME, values, whereClause, whereArgs);
    }

    public void delete(double latitude, double longitude) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_NAME, "latitude = ? AND longitude = ?", new String[]{Double.toString(latitude), Double.toString(longitude)});
    }

    //获取所有标记的names
    public ArrayList<String> query(Double lat, Double lon) {
        ArrayList<String> result = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from SomeWhereTable", null);
        if (cursor.getCount() != 0) {
            while (cursor.moveToNext()) {
                Double latitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
                Double longitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
                if (Math.abs(latitude-lat)<0.0000000000000001 && Math.abs(longitude-lon)<0.0000000000000001) {
                    String name = cursor.getString(cursor.getColumnIndex("name"));
                    String description = cursor.getString(cursor.getColumnIndex("description"));
                    String type = cursor.getString(cursor.getColumnIndex("type"));
                    String content = cursor.getString(cursor.getColumnIndex("content"));
                    String coverPage = cursor.getString(cursor.getColumnIndex("coverPage"));
                    result.add(name);
                    result.add(description);
                    result.add(type);
                    result.add(content);
                    result.add(coverPage);
                    break;
                }
            }
        }
        cursor.close();
        db.close();
        return result;
    }

    public List<Map<String, Object>> queryName(String username) {
        List<Map<String, Object>> result = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from SomeWhereTable", null);
        Log.e("sqlite", Integer.toString(cursor.getCount()));
        if (cursor.getCount() != 0) {
            while (cursor.moveToNext()) {
                String NAME = cursor.getString(cursor.getColumnIndex("userName"));
                if (TextUtils.equals(NAME, username)) {
                    Map<String, Object> temp = new HashMap<>();
                    Double latitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
                    Double longitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
                    String type = cursor.getString(cursor.getColumnIndex("type"));
                    String name = cursor.getString(cursor.getColumnIndex("name"));
                    String description = cursor.getString(cursor.getColumnIndex("description"));
                    temp.put("latitude", Double.toString(latitude));
                    temp.put("longitude", Double.toString(longitude));
                    temp.put("type", type);
                    temp.put("name", name);
                    temp.put("description", description);
                    result.add(temp);
                }
            }
        }
        cursor.close();
        return result;
    }
}
