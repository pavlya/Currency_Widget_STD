package com.tbg.currencywidget;

import android.content.Context;

import java.io.IOException;
import java.io.OutputStreamWriter;

public class ConverterAppConstants {
	public static final String LOG_TAG = "CurrencyTestConverter";
	public static final String secondPart = "&to=";
	public static final String startUrlRE = "http://rate-exchange.appspot.com/currency?from=";
	public static final String startUrlYahoo = "http://finance.yahoo.com/d/quotes.csv?e=.csv&f=sl1d1t1&s=";
	public static final String endUrlYahoo = "=X";
//	public static final String REstartURL = "http://";
	public static final String startUrlFR = "http://www.freecurrencyconverterapi.com/api/convert?q=";
	public static final String startURLNotWorking = "http://rate-exchange.appspot.com";
	// public final static String WIDGET_COLOR = "widget_color_";
	public final static String GOOGLE_JSON_CODE = "rate";

	// values for Shared Preferences
	public final static String WIDGET_PREF = "widget_pref";
	public final static String WIDGET_FROM_TEXT = "widget_from_text_";
	public final static String WIDGET_UPDATE_INTERVAL = "widget_update_interval_";
	public final static String WIDGET_TO_TEXT = "widget_to_text_";
	public final static String WIDGET_FROM_POSITION = "widget_from_pos_";
	public final static String WIDGET_TO_POSITION = "widget_to_pos_";
	public final static String WIDGET_CONVERTED_FROM_AMOUNT = "widget_converted_amount_";
	public final static String WIDGET_CONVERTED_TO_AMOUNT = "widget_converted_amount_to";
	public final static int FROM_CURRENCY_SELECTOR = 13;
	public final static int TO_CURRENCY_SELECTOR = 17;

	// Message broadcast constants
	public final static int IO_EXCEPTION = 1043530;
	public final static int ILLEGAL_ARGUMENT = 1043531;
	public final static int CLIENT_PROTOCOL_EXCEPTION = 1043532;
	public final static int VALUES_LOADED = 1043533;

	public void writeToFile(Context ctx, String data, String filename) {
		try {
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(
					ctx.openFileOutput(filename, Context.MODE_PRIVATE));
			outputStreamWriter.write(data);
			outputStreamWriter.close();
		} catch (IOException e) {
			Logger.error( "File write failed: " + e.toString());
		}
	}
}
