package com.eightbitcloud.internode.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import com.eightbitcloud.internode.PreferencesSerialiser;

public class AccountProvider extends ContentProvider {
    public static final String AUTHORITY = "com.eightbitcloud.internode.accountprovider";

    private static final String TAG = "AccountUsageProvider";
    private static final int ACCOUNT_TAG = 1;
    private static final int ACCOUNT_ITEM_TAG = 2;
    private static final int SERVICE_TAG = 3;
    private static final int SERVICE_ITEM_TAG = 4;

    private static final String ACCOUNTS_TABLE_NAME = "accounts";
    private static final String SERVICES_TABLE_NAME = "services";

    public static final Uri    ACCOUNTS_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + ACCOUNTS_TABLE_NAME);
    public static final Uri    SERVICES_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + SERVICES_TABLE_NAME);

    private static UriMatcher sUriMatcher;
    private static HashMap<String, String> accountProjectionMap;
    private static HashMap<String, String> serviceProjectionMap;
    private DatabaseHelper dbHelper;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, ACCOUNTS_TABLE_NAME, ACCOUNT_TAG);
        sUriMatcher.addURI(AUTHORITY, ACCOUNTS_TABLE_NAME + "/#", ACCOUNT_ITEM_TAG); // To match specific accounts
        sUriMatcher.addURI(AUTHORITY, SERVICES_TABLE_NAME, SERVICE_TAG); 
        sUriMatcher.addURI(AUTHORITY, SERVICES_TABLE_NAME + "/#/*", SERVICE_ITEM_TAG); 
        
         accountProjectionMap = new HashMap<String, String>();
         
         accountProjectionMap.put(Account.ID, Account.ID);
         accountProjectionMap.put(Account.USERNAME, Account.USERNAME);
         accountProjectionMap.put(Account.PASSWORD, Account.PASSWORD);
         accountProjectionMap.put(Account.PROVIDER, Account.PROVIDER);

         serviceProjectionMap = new HashMap<String, String>();
         serviceProjectionMap.put(Service.ID, Service.ID);
         serviceProjectionMap.put(Service.ACCOUNT_ID, Service.ACCOUNT_ID);
         serviceProjectionMap.put(Service.SERVICE_PROVIDER, Service.SERVICE_PROVIDER);
         serviceProjectionMap.put(Service.SERVICE_ID, Service.SERVICE_ID);
         serviceProjectionMap.put(Service.DATA, Service.DATA);
         serviceProjectionMap.put(Service.UPDATED, Service.UPDATED);
         serviceProjectionMap.put(Service.STATUS, Service.STATUS);
    }


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        int count;

        switch (sUriMatcher.match(uri)) {
        case ACCOUNT_ITEM_TAG:
            String idPart = uri.getPathSegments().get(1);
            selectionArgs = new String[] { idPart };
            selection = Account.ID + " = ?";
        case ACCOUNT_TAG:
            count = db.delete(ACCOUNTS_TABLE_NAME, selection, selectionArgs);
            break;
            
        case SERVICE_ITEM_TAG:
            String accountPart = uri.getPathSegments().get(1);
            String servicePart = uri.getPathSegments().get(2);
            selectionArgs = new String[] { accountPart, servicePart };
            selection = Service.ACCOUNT_ID + " = ? and " + Service.SERVICE_ID + " = ?";
        case SERVICE_TAG:
            count = db.delete(SERVICES_TABLE_NAME, selection, selectionArgs);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
        case ACCOUNT_TAG:
            return Account.CONTENT_TYPE;
        case ACCOUNT_ITEM_TAG:
            return Account.ITEM_TYPE;
        case SERVICE_TAG:
            return Service.CONTENT_TYPE;
        case SERVICE_ITEM_TAG:
            return Service.ITEM_TYPE;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int tag = sUriMatcher.match(uri);
        if (tag != ACCOUNT_TAG && tag != SERVICE_TAG) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        if (tag == ACCOUNT_TAG) {
            long rowId = db.insertOrThrow(ACCOUNTS_TABLE_NAME, Account.PROVIDER, values);
            if (rowId != -1) {
                Uri noteUri = ContentUris.withAppendedId(ACCOUNTS_CONTENT_URI, rowId);
                getContext().getContentResolver().notifyChange(noteUri, null);
                return noteUri;
            }
        } else {
            Log.d(TAG, "Inserting service.  Values is " + values);
            long rowId = db.insertOrThrow(SERVICES_TABLE_NAME, Service.ID, values);
            if (rowId != -1) {
                int accountID = values.getAsInteger(Service.ACCOUNT_ID);
                String serviceID = values.getAsString(Service.SERVICE_ID);
                Uri noteUri = SERVICES_CONTENT_URI.buildUpon().appendPath(Integer.toString(accountID)).appendPath(serviceID).build();
                // TODO do we need to notify the list URI as well?
                getContext().getContentResolver().notifyChange(noteUri, null);
                return noteUri;
            }
            
        }
        
        throw new SQLException("Failed to insert row into " + uri);
    }


    @Override
    public boolean onCreate() {
        dbHelper = new DatabaseHelper(getContext());
        SharedPreferences prefs = getContext().getSharedPreferences(PreferencesSerialiser.PREFS_FILE, Context.MODE_PRIVATE);
        
        if (prefs.getString("acct1.username", null) != null && !prefs.getBoolean("copiedToProvider", false)) {
            List<Account> accounts = new ArrayList<Account>();
            PreferencesSerialiser.deserialise(prefs, accounts);
            
            for (Account acct: accounts) {
                Uri uri = insert(ACCOUNTS_CONTENT_URI, acct.getValues());
                Log.d(TAG, "Inserted " + uri);
            }
            // Don't bother with the statistics, as they will be auto-fetched.
            
            // Write a sentinel value, so this isn't done again.
            prefs.edit().putBoolean("copiedToProvider", true).commit();
        }
        return true;

    }
    
    @Override 
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {
        case ACCOUNT_ITEM_TAG:
            String idPart = uri.getPathSegments().get(1);
            qb.appendWhere(Account.ID + " = " + idPart);
            // Fall through
        case ACCOUNT_TAG:
            qb.setTables(ACCOUNTS_TABLE_NAME);
            qb.setProjectionMap(accountProjectionMap);
            break;
        case SERVICE_ITEM_TAG:
            String accountPart = uri.getPathSegments().get(1);
            String servicePart = uri.getPathSegments().get(2);
            selectionArgs = new String[] { accountPart, servicePart };
            selection = Service.ACCOUNT_ID + " = ? and " + Service.SERVICE_ID + " = ?";
            // Fall through
        case SERVICE_TAG:
            qb.setTables(SERVICES_TABLE_NAME);
            qb.setProjectionMap(serviceProjectionMap);
            break;
            
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null,
                null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);

        return c;

    }

    @Override
    public int update(Uri uri, ContentValues values, String where,  String[] whereArgs) {

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case ACCOUNT_ITEM_TAG:
            // Override the where clause to mean our own ID
            String idPart = uri.getPathSegments().get(1);
            where = Account.ID + " = " + idPart;
            // FallThrough

        case ACCOUNT_TAG:
            count = db.update(ACCOUNTS_TABLE_NAME, values, where, whereArgs);
            break;
        case SERVICE_ITEM_TAG:
            String accountPart = uri.getPathSegments().get(1);
            String servicePart = uri.getPathSegments().get(2);
            whereArgs = new String[] { accountPart, servicePart };
            where = Service.ACCOUNT_ID + " = ? and " + Service.SERVICE_ID + " = ?";
            // Fall through
        case SERVICE_TAG:
            if (values.containsKey(Service.ACCOUNT_ID) && values.getAsInteger(Service.ACCOUNT_ID) == 0) {
                Log.e(TAG, "Updating service without an account ID", new Throwable());
            }
            count = db.update(SERVICES_TABLE_NAME, values, where, whereArgs);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;

    }
    

    /**
     * Database helper for this content provider.  Provides table creation/upgrade scripts,
     * as well as lazy-loading capabilities for the db connection.
     * 
     * @author bruce
     *
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 5;
        private static final String DATABASE_NAME = "accounts.db";

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + ACCOUNTS_TABLE_NAME + " ("
                    + Account.ID + " INTEGER PRIMARY KEY AUTOINCREMENT," 
                    + Account.USERNAME  + " TEXT, " 
                    + Account.PASSWORD + " TEXT, " 
                    + Account.PROVIDER + " TEXT"
                    + ");");
            db.execSQL("CREATE TABLE " + SERVICES_TABLE_NAME + " ("
                    + Service.ID + " INTEGER PRIMARY KEY AUTOINCREMENT," 
                    + Service.ACCOUNT_ID + " INTEGER, " 
                    + Service.SERVICE_PROVIDER  + " TEXT, "  // KIND of redundant, as this is in the account too, but its useful 
                    + Service.SERVICE_ID  + " TEXT, " 
                    + Service.UPDATED + " INTEGER, " 
                    + Service.STATUS + " INTEGER, " 
                    + Service.DATA + " LONGTEXT " 
                    + ");");

            // TODO uniqueness constratints?
            
            
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + ACCOUNTS_TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + SERVICES_TABLE_NAME);
            onCreate(db);
        }
    }
}
