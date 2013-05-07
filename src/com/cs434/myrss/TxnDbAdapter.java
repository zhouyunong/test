package com.cs434.myrss;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Date;

import com.cs434.myrss.RssParser.Item;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

public class TxnDbAdapter {
	private static final String TAG = "TxnDbAdapter";
	public static final String KEY_ROWID = "_id";
	public static final String KEY_TITLE = "title";
	public static final String KEY_LINK = "link";
	public static final String KEY_DESCRIPTION = "description";
	public static final String KEY_PUBDATE = "pubDate";
	public static final String[] HOT_COLUMNS = new String[] { KEY_TITLE, KEY_LINK, KEY_DESCRIPTION, KEY_PUBDATE};
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private Cursor myCursor;

    private static final String HOT_TABLE = "TopHeadlines";
    private static final String DATABASE_NAME = "headlines";
    private static final int DATABASE_VERSION = 9;
    private static final String HOT_TABLE_CREATE = "create table if not exists TopHeadlines ( _id integer primary key autoincrement, title text null, link text null, "
        + "description text null, pubDate text null)";
    
    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
          super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        	try{
        		db.execSQL(HOT_TABLE_CREATE);
        	}catch(SQLException e){
        		e.printStackTrace();
        	}
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
          Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
              + newVersion + " which destroys all old data");
          db.execSQL("DROP TABLE IF EXISTS " + HOT_TABLE);
          onCreate(db);
        }
      }
    private final Context mContext;
    public TxnDbAdapter(Context context){
      this.mContext = context;
    }

    public boolean open() throws SQLException {
      try
      {
      mDbHelper = new DatabaseHelper(mContext);
      mDb = mDbHelper.getWritableDatabase();
      }
      catch(Exception e)
      {
    	  e.printStackTrace();
      }
      return true;
    }

    public void close() {
      mDbHelper.close();
    }

    public final static DateFormat DB_DATE_FORMATTER = new SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss.SSS");
    
    public long insertHeadline(Item item) {
    	
    	long i=0;
    	
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_TITLE, item.title);
        initialValues.put(KEY_LINK, item.link);
        initialValues.put(KEY_DESCRIPTION, item.description);
        initialValues.put(KEY_PUBDATE, item.pubDate);
        try
        {
          i= mDb.insert(HOT_TABLE, null, initialValues);
        }
        catch(Exception e)
        {
        	e.printStackTrace();	
        }
        return i;
      }
    
    public Cursor fetchAllHeadlines() {
    	try
    	{
    	 Cursor mCursor = mDb.query(HOT_TABLE, new String[]{ KEY_ROWID, KEY_TITLE, KEY_LINK, KEY_DESCRIPTION,KEY_PUBDATE}, null, null, null, null,KEY_ROWID + " DESC");
    		
    		
    	if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    	}
    	catch(Exception e)
    	{
    		e.getMessage().toString();
    	}
		return null;
      }
    public Cursor fetchHeadline(Integer id){
    	Cursor mCursor = mDb.query(HOT_TABLE, new String[]{KEY_DESCRIPTION, KEY_PUBDATE},
		KEY_ROWID + " = " + id, null, null, null, null);
    	if (mCursor != null) {
    		mCursor.moveToFirst();
    	}
    	return mCursor;
    }
    public void addTransactions(List<Item> items) {
    	boolean hasBeenNotified = false;
        try {
          mDb.beginTransaction();

          for (Item item : items) {
        	// ------------------------------------------------------------------
        	// Apply some cosmetics before inserting item
          	// Remove all newline characters in title so it is displayed in
          	// a more user friendly way
          	item.title = item.title.replace ('\n', ' ');
          	// Replace all occurences of one or more whitespace characters with just
          	// one whitespace character.
          	item.title = item.title.replaceAll("(\\s+)", " ");
          	// ------------------------------------------------------------------
          	// Avoid redundancy - only insert item if it doesn't already exist
        	// Check if item already exists by querying all tuples and checking
          	// whether the title of the item is already present
          	myCursor = mDb.query(HOT_TABLE, new String[]{KEY_ROWID, KEY_TITLE},
          			KEY_TITLE+"= \""+item.title+"\"",
          			null, null, null, null);
          	// myCursor = mDb.rawQuery("select _id from topheadlines where title = " + item.title + ";", null);
          	// Cursor myCursr = mDb.query(HOT_TABLE, new String[]{ KEY_TITLE+"="+item.title, KEY_ROWID}, null, null, null, null, null);
        	if (myCursor.getCount() == 0) {
        		insertHeadline(item);
        		// Let the user know we have fetched new feeds
        		if (hasBeenNotified == false) {
        			Toast.makeText(mContext, "You have new feeds!",
        					Toast.LENGTH_SHORT).show();
        			hasBeenNotified = true;
        		}
        	}
          }
          limitRows(HOT_TABLE, 50);
          mDb.setTransactionSuccessful();
        } finally {
          mDb.endTransaction();
        }
      }
    public int limitRows(String tablename, int limit) {
        Cursor cursor = mDb.rawQuery("SELECT " + KEY_TITLE + " FROM " + tablename + " ORDER BY " + KEY_TITLE + " DESC LIMIT 1 OFFSET ?",
            new String[] { limit - 1 + "" });

        int deleted = 0;

        if (cursor != null && cursor.moveToFirst()) {
          long limitId = cursor.getLong(0);
          deleted = mDb.delete(tablename, KEY_LINK + "<" + limitId, null);
        }

        cursor.close();
        return deleted;
      }
    
    public void clearData() {
        deleteAllHeadlines();
      }

      public boolean deleteAllHeadlines() {
        return mDb.delete(HOT_TABLE, null, null) > 0;
      }
      public String getDateTime() {
          DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
          Date date = new Date();
          return dateFormat.format(date);
      }
      
   public boolean dropTable()
   {
	   mDb.execSQL("DROP TABLE IF EXISTS " + HOT_TABLE);
	   return true;
   }
   
   public boolean createTable()
   {
	   mDb.execSQL(HOT_TABLE_CREATE);
	   return true;
   }
}

