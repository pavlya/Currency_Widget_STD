package com.tbg.currencywidget.data;

import android.database.sqlite.SQLiteDatabase;

import com.tbg.currencywidget.Logger;

public class CurrenciesTable {

	// Database table
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_ABBREVIATIONS = "abbreviations";
	public static final String COLUMN_CURRENCIES_NAME = "currencies_names";
	public static final String CURRENCIES_TABLE_NAME = "currencies_table";
	/**
	 * '0' the values is not in "favorites", '1' the value is in "favorites"
	 */
	public static final String COLUMN_CURRENCIES_FAVORITES = "currencies_favorites";

	// Database creation SQL statement
	private static final String TABLE_CREATE = ("CREATE TABLE "
			+ CURRENCIES_TABLE_NAME + " (" + COLUMN_ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_ABBREVIATIONS
			+ " TEXT, " + COLUMN_CURRENCIES_NAME + " TEXT, "
			+ COLUMN_CURRENCIES_FAVORITES + " INTEGER);");

	public static void onCreate(SQLiteDatabase database) {
		Logger.debug("Creating currencies table: " + TABLE_CREATE);
		database.execSQL(TABLE_CREATE);
	}

	public static void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion) {
		Logger.warning("Upgrading database from verson " + oldVersion + " to "
				+ newVersion + " to " + newVersion
				+ ", which will destroy all old data");
		database.execSQL("DROP TABLE IF EXISTS " + CURRENCIES_TABLE_NAME);
		onCreate(database);
	}
}
