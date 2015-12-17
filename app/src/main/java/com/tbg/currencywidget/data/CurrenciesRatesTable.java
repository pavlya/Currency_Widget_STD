package com.tbg.currencywidget.data;

import android.database.sqlite.SQLiteDatabase;

import com.tbg.currencywidget.Logger;

public class CurrenciesRatesTable {

	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_FROM_CURRENCY = "from_currency_abbreviation";
	public static final String COLUMN_TO_CURRENCY = "to_currency_abbreviation";
	public static final String CURRENCIES_RATES_TABLE_NAME = "currencies_rates_table";
	public static final String COLUMN_EXCHANGE_RATE = "exchange_rate";
	public static final String COLUMN_LAST_REQUEST_DATE = "last_request_date";
	public static final String COLUMN_LAST_UPDATE_DATE = "last_update_date";
	public static final String COLUMN_CURRENTLY_IN_USE = "currently_in_use";

	private static final String TABLE_CREATE = ("CREATE TABLE "
			+ CURRENCIES_RATES_TABLE_NAME + " (" + COLUMN_ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_FROM_CURRENCY
			+ " TEXT, " + COLUMN_TO_CURRENCY + " TEXT, " + COLUMN_EXCHANGE_RATE
			+ " REAL, " + COLUMN_LAST_REQUEST_DATE + " REAL, "
			+ COLUMN_LAST_UPDATE_DATE + " REAL, " + COLUMN_CURRENTLY_IN_USE + " INTEGER);");

	public static void onCreate(SQLiteDatabase database) {
		Logger.debug("Creating currencies rates table: " + TABLE_CREATE);
		database.execSQL(TABLE_CREATE);
	}

	public static void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion) {
		Logger.warning("Upgrading database from verson " + oldVersion + " to "
				+ newVersion + " to " + newVersion
				+ ", which will destroy all old data");
		database.execSQL("DROP TABLE IF EXISTS " + CURRENCIES_RATES_TABLE_NAME);
		onCreate(database);
	}
}
