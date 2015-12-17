package com.tbg.currencywidget.data;

import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.tbg.currencywidget.Logger;
import com.tbg.currencywidget.R;

import java.util.Arrays;
import java.util.List;

public class CurrenciesDBHelper extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "Currencies Convertor DB";
	Context context;

	public CurrenciesDBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.context = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		CurrenciesTable.onCreate(db);
		CurrenciesRatesTable.onCreate(db);
		addAllCurrencies(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		CurrenciesTable.onUpgrade(db, oldVersion, newVersion);
		CurrenciesRatesTable.onUpgrade(db, oldVersion, newVersion);
		addAllCurrencies(db);
	}

	private void addAllCurrencies(SQLiteDatabase db2) {
		Resources res = context.getResources();
		String currenciesAbbreviation[] = res
				.getStringArray(R.array.all_currencies_abbreviation);
		String currenciesNames[] = res
				.getStringArray(R.array.all_currencies_names);
		String InsertQuery = "";
		String[] favoriteCurrencies = { "AUD", "CAD", "CHF", "EUR", "GBP",
				"JPY", "USD" };
		List<String> favorites = Arrays.asList(favoriteCurrencies);
		for (int i = 0; i < currenciesNames.length; i++) {
			// check if currency should be in favoites
			if (favorites.contains(currenciesAbbreviation[i])) {
				InsertQuery = "Insert INTO "
						+ CurrenciesTable.CURRENCIES_TABLE_NAME + " ("
						+ CurrenciesTable.COLUMN_ABBREVIATIONS + ", "
						+ CurrenciesTable.COLUMN_CURRENCIES_NAME + ", "
						+ CurrenciesTable.COLUMN_CURRENCIES_FAVORITES
						+ ") VALUES ('" + currenciesAbbreviation[i] + "', '"
						+ currenciesNames[i] + "', '" + "1" + "')"; // add 1 as
																	// true
			} else {
				InsertQuery = "Insert INTO "
						+ CurrenciesTable.CURRENCIES_TABLE_NAME + " ("
						+ CurrenciesTable.COLUMN_ABBREVIATIONS + ", "
						+ CurrenciesTable.COLUMN_CURRENCIES_NAME + ", "
						+ CurrenciesTable.COLUMN_CURRENCIES_FAVORITES
						+ ") VALUES ('" + currenciesAbbreviation[i] + "', '"
						+ currenciesNames[i] + "', '" + "0" + "')"; // add 0 as
																	// false
			}
			db2.execSQL(InsertQuery);
		}
		Logger.debug(InsertQuery);

	}

}
