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

public class CurrenciesRatesContentProvider extends ContentProvider {

	// used for the UriMatcher
	private static final int CURRENCIES_EXCHANGE = 10;
	private static final int CURRENCY_EXCHANGE_ID = 20;

	private static final String AUTHORITY = "com.tbg.pavlya.currencies_rates.contentprovider";
	private static final String BASE_PATH = "currencies_exchange";

	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + BASE_PATH);
	public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
			+ "/currencies_exchange";
	public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/currency_exchange";

	private static final UriMatcher sURIMatcher = new UriMatcher(
			UriMatcher.NO_MATCH);
	static {
		sURIMatcher.addURI(AUTHORITY, BASE_PATH, CURRENCIES_EXCHANGE);
		sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", CURRENCY_EXCHANGE_ID);
	}

	private CurrenciesDBHelper database;

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
//		String projectionString = "";
//		String selectionString = selection;
//		String selectionArgsString = "";
////		for (String string : projection) {
////			projectionString += string + " ";
////		}
//		for (String string : selectionArgs) {
//			selectionArgsString += string + " ";
//		}
//
//		Log.d(ConverterApp.LOG_TAG,
//				"CurrenciesRatesContentProvider.query projectionString: "
//						+ projectionString + " selectionString: "
//						+ selectionString + " selectionArgsString: "
//						+ selectionArgsString);
		// Using SQLiteQueryBuilder instead of query() method
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

		// check if the caller has requested a column which does not exists
		checkColumns(projection);

		// Set the table
		queryBuilder
				.setTables(CurrenciesRatesTable.CURRENCIES_RATES_TABLE_NAME);
		int uriType = sURIMatcher.match(uri);
		switch (uriType) {
		case CURRENCIES_EXCHANGE:
			break;
		case CURRENCY_EXCHANGE_ID:
			// adding the ID to the original query
			queryBuilder.appendWhere(CurrenciesRatesTable.COLUMN_ID + "="
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

	private void checkColumns(String[] projection) {
		String[] available = { CurrenciesRatesTable.COLUMN_ID,
				CurrenciesRatesTable.COLUMN_FROM_CURRENCY,
				CurrenciesRatesTable.COLUMN_TO_CURRENCY,
				CurrenciesRatesTable.COLUMN_EXCHANGE_RATE,
				CurrenciesRatesTable.COLUMN_LAST_REQUEST_DATE,
				CurrenciesRatesTable.COLUMN_LAST_UPDATE_DATE,
				CurrenciesRatesTable.COLUMN_CURRENTLY_IN_USE };
		if (projection != null) {
			HashSet<String> requestedColumns = new HashSet<String>(
					Arrays.asList(projection));
			HashSet<String> availableColumns = new HashSet<String>(
					Arrays.asList(available));
			// check if all columns which are requested are available
			if (!availableColumns.containsAll(requestedColumns)) {
				throw new IllegalArgumentException(
						"Unknown columns in projection");
			}
		}

	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		int rowsDeleted = 0;
		switch (uriType) {
		case CURRENCIES_EXCHANGE:
			rowsDeleted = sqlDB.delete(
					CurrenciesRatesTable.CURRENCIES_RATES_TABLE_NAME,
					selection, selectionArgs);
			break;
		case CURRENCY_EXCHANGE_ID:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsDeleted = sqlDB.delete(
						CurrenciesRatesTable.CURRENCIES_RATES_TABLE_NAME,
						CurrenciesRatesTable.COLUMN_ID + " = " + id, null);
			} else {
				rowsDeleted = sqlDB.delete(
						CurrenciesRatesTable.CURRENCIES_RATES_TABLE_NAME,
						CurrenciesRatesTable.COLUMN_ID + "=" + id + " and "
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
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		long id = 0;
		switch (uriType) {
		case CURRENCIES_EXCHANGE:
			id = sqlDB.insert(CurrenciesRatesTable.CURRENCIES_RATES_TABLE_NAME,
					null, values);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return Uri.parse(BASE_PATH + "/" + id);
	}

	@Override
	public boolean onCreate() {
		database = new CurrenciesDBHelper(getContext());
		return false;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		int rowsUpdated = 0;
		switch (uriType) {
		case CURRENCIES_EXCHANGE:
			rowsUpdated = sqlDB.update(
					CurrenciesRatesTable.CURRENCIES_RATES_TABLE_NAME, values,
					selection, selectionArgs);
			break;
		case CURRENCY_EXCHANGE_ID:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsUpdated = sqlDB
						.update(CurrenciesRatesTable.CURRENCIES_RATES_TABLE_NAME,
								values, CurrenciesRatesTable.COLUMN_ID + "="
										+ id, null);
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

}
