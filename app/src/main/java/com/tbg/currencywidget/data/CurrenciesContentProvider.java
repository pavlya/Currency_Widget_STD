package com.tbg.currencywidget.data;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;

public class CurrenciesContentProvider extends ContentProvider {

	// database
	private CurrenciesDBHelper database;

	// used for the UriMatcher
	private static final int CURRENCIES = 10;
	private static final int CURRENCY_ID = 20;

	private static final String AUTHORITY = "com.tbg.pavlya.currencies.contentprovider";
	private static final String BASE_PATH = "currencies";

	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + BASE_PATH);
	public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
			+ "/currencies";
	public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/currency";

	private static final UriMatcher sURIMatcher = new UriMatcher(
			UriMatcher.NO_MATCH);
	static {
		sURIMatcher.addURI(AUTHORITY, BASE_PATH, CURRENCIES);
		sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", CURRENCY_ID);
	}

	@Override
	public boolean onCreate() {
		if(database == null){
		database = new CurrenciesDBHelper(getContext());
		return true;
		} else {
			return false;
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		// Using SQLiteQueryBuilder instead of query() method
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

		// check if the caller has requested a column which does not exists
		checkColumns(projection);

		// Set the table
		queryBuilder.setTables(CurrenciesTable.CURRENCIES_TABLE_NAME);
		int uriType = sURIMatcher.match(uri);
		switch (uriType) {
		case CURRENCIES:
			break;
		case CURRENCY_ID:
			// adding the ID to the original query
			queryBuilder.appendWhere(CurrenciesTable.COLUMN_ID + "="
					+ uri.getLastPathSegment());
			break;

		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		SQLiteDatabase db = database.getWritableDatabase();
		Cursor cursor = queryBuilder.query(db, projection, selection,
				selectionArgs, null, null, sortOrder);

		// make sure that potential listeners are getting notified
		cursor.setNotificationUri(getContext().getContentResolver(), uri);

		return cursor;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		int rowsDeleted = 0;
		long id = 0;
		switch (uriType) {
		case CURRENCIES:
			break;
		default:
			throw new IllegalArgumentException("Can't insert to this DB");
		}
		return null;

	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		int rowsDeleted = 0;
		switch (uriType) {
		case CURRENCY_ID:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsDeleted = sqlDB.delete(
						CurrenciesTable.CURRENCIES_TABLE_NAME,
						CurrenciesTable.COLUMN_ID + "=" + id, null);
			} else {
				rowsDeleted = sqlDB.delete(
						CurrenciesTable.CURRENCIES_TABLE_NAME,
						CurrenciesTable.COLUMN_ID + "=" + id + " and "
								+ selection, selectionArgs);
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsDeleted;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		int rowsUpdated = 0;
		switch (uriType) {
		case CURRENCIES:
			rowsUpdated = sqlDB.update(CurrenciesTable.CURRENCIES_TABLE_NAME,
					values, selection, selectionArgs);
			break;
		case CURRENCY_ID:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsUpdated = sqlDB.update(
						CurrenciesTable.CURRENCIES_TABLE_NAME, values,
						CurrenciesTable.COLUMN_ID + "=" + id, null);
			} else {
				rowsUpdated = sqlDB.update(
						CurrenciesTable.CURRENCIES_TABLE_NAME, values,
						CurrenciesTable.COLUMN_ID + "=" + id + " and "
								+ selection, selectionArgs);
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsUpdated;
	}

	private void checkColumns(String[] projection) {
		String[] available = { CurrenciesTable.COLUMN_ABBREVIATIONS,
				CurrenciesTable.COLUMN_CURRENCIES_FAVORITES,
				CurrenciesTable.COLUMN_CURRENCIES_NAME,
				CurrenciesTable.COLUMN_ID };
		if (projection != null) {
			HashSet<String> requestedColumns = new HashSet<String>(
					Arrays.asList(projection));
			HashSet<String> availableColumns = new HashSet<String>(
					Arrays.asList(available));
			// check if all columns which are requested are available
			if(!availableColumns.containsAll(requestedColumns)){
				throw new IllegalArgumentException("Unknown columns in projection");
			}
		}
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}
}
