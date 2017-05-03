package com.tbg.currencywidget;

import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.tbg.currencywidget.data.CurrenciesRatesContentProvider;
import com.tbg.currencywidget.data.CurrenciesRatesTable;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;

import static com.tbg.currencywidget.ConverterAppConstants.LOG_TAG;

/**
 * Load Currency Rates from Database if there is now rates, loads rates from
 * Google API
 * 
 * @author Pavel
 * 
 */
public class RatesLoader extends Application {

	public static final String ACTION_PARSING_ERROR = "com.tbg.currencywidget.action.ERROR";
	public static final String ACTION_PARSING_MESSAGE = "parsing_message";
	private Context context;
	private static RatesLoader instance;
	String fromCurrency;
	String toCurrency;
	private static long hour = 60 * 60 * 1000;
	private CurrencyLoader currencyLoader;

	// public static RatesLoader getInstance(Context context) {
	// if (instance == null) {
	// instance = new RatesLoader(context);
	// }
	// return instance;
	// }

	public static RatesLoader getInstance() {
		return instance;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		instance = this;
		this.context = getApplicationContext();
	}

	// // only update without loading value
	// private RatesLoader(Context context) {
	// this.context = context;
	// }

	/**
	 * Check if rates are older, than one hour. In case, the rates are outdated,
	 * load new rates from Internet.
	 * 
	 * @param fromCurrency
	 * @param toCurrency
	 */
	public void loadRates(String fromCurrency, String toCurrency) {
		// Check if there is no wrong values
		if (fromCurrency == null || toCurrency == null
				|| fromCurrency.length() < 1 || toCurrency.length() < 1) {
			Log.d(LOG_TAG, "RatesLoder.getRates from currency " + fromCurrency
					+ " toCurrency " + toCurrency);
			return;
		}

		Cursor cursor = getCursorWithRates(fromCurrency, toCurrency);
		// there is no values with current currency pair in db
		if (cursor.getCount() == 0) {
			insertNewCurrenciesPair(fromCurrency, toCurrency);
		} else {
			// loads value from cursor
			while (cursor.moveToNext()) {
				// get date of last update
				long lastUpdate = cursor
						.getLong(cursor
								.getColumnIndex(CurrenciesRatesTable.COLUMN_LAST_UPDATE_DATE));
				long currentTime = java.lang.System.currentTimeMillis();
				// check if update is older than one hour
				if ((currentTime - lastUpdate) > hour) {
					updateExistingCurrencyPair(cursor);
				}
			}
		}

		if (cursor != null) {
			cursor.close();
		}
	}

	/**
	 * 
	 * @param cursor
	 */
	private void updateExistingCurrencyPair(Cursor cursor) {
		int id = cursor.getInt(cursor
				.getColumnIndex(CurrenciesRatesTable.COLUMN_ID));
		String fromCurrencyName = cursor.getString(cursor
				.getColumnIndex(CurrenciesRatesTable.COLUMN_FROM_CURRENCY));
		String toCurrencyName = cursor.getString(cursor
				.getColumnIndex(CurrenciesRatesTable.COLUMN_TO_CURRENCY));

		CurrencyGetterType getterType = CurrencyGetterType.UPDATE;
		// avoid mess up with loaders
		if (currencyLoader == null
				|| currencyLoader.getStatus().equals(AsyncTask.Status.FINISHED)) {
			currencyLoader = new CurrencyLoader(getterType, id);
			currencyLoader.execute(fromCurrencyName, toCurrencyName);
		}
	}

	/**
	 * Insert new Currencies Pair to DB
	 * 
	 * @param fromCurrency
	 * @param toCurrency
	 */
	private void insertNewCurrenciesPair(String fromCurrency, String toCurrency) {
		// load rates from internet if there is no values in db
		Log.d(LOG_TAG,
				"RatesLoader.getRates Cursor is empty. Loading Rates from Google API");
		CurrencyGetterType getterType = CurrencyGetterType.INSERT;
		if (currencyLoader == null
				|| currencyLoader.getStatus().equals(AsyncTask.Status.FINISHED)) {
			currencyLoader = new CurrencyLoader(getterType);
			currencyLoader.execute(fromCurrency, toCurrency);
		}
	}

	/**
	 * 
	 * @param fromCurrency
	 * @param toCurrency
	 * @return load cursor for specified pair
	 */
	public Cursor getCursorWithRates(String fromCurrency, String toCurrency) {
		String selection = CurrenciesRatesTable.COLUMN_FROM_CURRENCY
				+ "=? AND " + CurrenciesRatesTable.COLUMN_TO_CURRENCY + "=?";
		// String[] selectionArgs = { this.fromCurrency, this.toCurrency };
		String[] selectionArgs = { fromCurrency, toCurrency };
		String[] projection = null;
		Uri ratesUri = CurrenciesRatesContentProvider.CONTENT_URI;
		ContentResolver cr = context.getContentResolver();

		Cursor cursor = cr.query(ratesUri, projection, selection,
				selectionArgs, null);
		return cursor;
	}

	private void insertCurrenciesRatioToDb(double currencyRatio,
			String fromCurrency, String toCurrency) {
		ContentValues values = new ContentValues();
		long currentTime = Calendar.getInstance().getTimeInMillis();

		String columnFromCurrencyName = CurrenciesRatesTable.COLUMN_FROM_CURRENCY;
		String columnToCurrencyName = CurrenciesRatesTable.COLUMN_TO_CURRENCY;
		String columnExchangeRateName = CurrenciesRatesTable.COLUMN_EXCHANGE_RATE;
		String columnLastRequestDateName = CurrenciesRatesTable.COLUMN_LAST_REQUEST_DATE;
		String columnLastUpdateDateName = CurrenciesRatesTable.COLUMN_LAST_UPDATE_DATE;
		String columnCurrentlyInUseName = CurrenciesRatesTable.COLUMN_CURRENTLY_IN_USE;

		double exchangeRate = currencyRatio;
		long lastRequestDate = currentTime;
		long lastUpdateDate = currentTime;
		int currentlyInUse = 1; // for future check if currency is selected

		values.put(columnFromCurrencyName, fromCurrency);
		values.put(columnToCurrencyName, toCurrency);
		values.put(columnExchangeRateName, exchangeRate);
		values.put(columnLastRequestDateName, lastRequestDate);
		values.put(columnLastUpdateDateName, lastUpdateDate);
		values.put(columnCurrentlyInUseName, currentlyInUse);

		context.getContentResolver().insert(
				CurrenciesRatesContentProvider.CONTENT_URI, values);

	}

	// /**
	// * Update the rates in DB
	// *
	// * @param currencyRatio
	// * current exchange ratio of the currencies
	// * @param fromCurrency
	// * abbreviation of the currency
	// * @param toCurrency
	// * abbreviation of the currency
	// * @param id
	// * of the row
	// */
	// private void updateCurrenciesRatioToDb(double currencyRatio,
	// String fromCurrency, String toCurrency, int id) {
	// ContentValues values = new ContentValues();
	// long currentTime = Calendar.getInstance().getTimeInMillis();
	//
	// String columnFromCurrencyName =
	// CurrenciesRatesTable.COLUMN_FROM_CURRENCY;
	// String columnToCurrencyName = CurrenciesRatesTable.COLUMN_TO_CURRENCY;
	// String columnExchangeRateName =
	// CurrenciesRatesTable.COLUMN_EXCHANGE_RATE;
	// String columnLastRequestDateName =
	// CurrenciesRatesTable.COLUMN_LAST_REQUEST_DATE;
	// String columnLastUpdateDateName =
	// CurrenciesRatesTable.COLUMN_LAST_UPDATE_DATE;
	// String columnCurrentlyInUseName =
	// CurrenciesRatesTable.COLUMN_CURRENTLY_IN_USE;
	// // String columnID = CurrenciesRatesTable.COLUMN_ID;
	//
	// double exchangeRate = currencyRatio;
	// long lastRequestDate = currentTime;
	// long lastUpdateDate = currentTime;
	// int currentlyInUse = 1; // for future check if currency is selected
	//
	// values.put(columnFromCurrencyName, fromCurrency);
	// values.put(columnToCurrencyName, toCurrency);
	// values.put(columnExchangeRateName, exchangeRate);
	// values.put(columnLastRequestDateName, lastRequestDate);
	// values.put(columnLastUpdateDateName, lastUpdateDate);
	// values.put(columnCurrentlyInUseName, currentlyInUse);
	// Uri ratesUri = Uri.parse(CurrenciesRatesContentProvider.CONTENT_URI
	// + "/" + id);
	//
	// Log.d(LOG_TAG,
	// "RatesLoader.updateCurrenciesRatioToDb update to rates cursor ID "
	// + id);
	// String where = null;
	// String[] selectionArgs = null;
	// context.getContentResolver().update(ratesUri, values, where,
	// selectionArgs);
	// }

	private void updateCurrenciesRatioToDb(double currencyRatio, int id) {
		ContentValues values = new ContentValues();
		long currentTime = Calendar.getInstance().getTimeInMillis();

		String columnExchangeRateName = CurrenciesRatesTable.COLUMN_EXCHANGE_RATE;
		String columnLastRequestDateName = CurrenciesRatesTable.COLUMN_LAST_REQUEST_DATE;
		String columnLastUpdateDateName = CurrenciesRatesTable.COLUMN_LAST_UPDATE_DATE;
		String columnCurrentlyInUseName = CurrenciesRatesTable.COLUMN_CURRENTLY_IN_USE;
		// String columnID = CurrenciesRatesTable.COLUMN_ID;

		double exchangeRate = currencyRatio;
		long lastRequestDate = currentTime;
		long lastUpdateDate = currentTime;
		int currentlyInUse = 1; // for future check if currency is selected

		values.put(columnExchangeRateName, exchangeRate);
		values.put(columnLastRequestDateName, lastRequestDate);
		values.put(columnLastUpdateDateName, lastUpdateDate);
		values.put(columnCurrentlyInUseName, currentlyInUse);
		Uri ratesUri = Uri.parse(CurrenciesRatesContentProvider.CONTENT_URI
				+ "/" + id);

		Log.d(LOG_TAG,
				"RatesLoader.updateCurrenciesRatioToDb update to rates cursor ID "
						+ id);
		String where = null;
		String[] selectionArgs = null;
		context.getContentResolver().update(ratesUri, values, where,
				selectionArgs);
	}

	private class CurrencyLoader extends AsyncTask<String, Void, Double> {

		private CurrencyGetterType getterType;
		private int rowID;
		private String fromCurr;
		private String toCurr;

		@Override
		protected Double doInBackground(String... params) {
			if (params.length >= 2) {
				fromCurr = params[0];
				toCurr = params[1];
				int message = 0;
				double response = -1d;
				try{
					response = getCurrencyRatesFromYahoo(fromCurr, toCurr);
					if(response > 0){
						return response;
					}
				}catch (ClientProtocolException e){
					Log.d(LOG_TAG, e.getMessage());
					sendBroadcast(message);
				} catch (IOException e) {
					e.printStackTrace();
					message = ConverterAppConstants.IO_EXCEPTION;
					Log.d(LOG_TAG, e.getMessage());
					// TODO broadcast no internet message or something like this
					sendBroadcast(message);
				}
				try {
					response = getCurrencyJsonRE(params[0], params[1]);
				} catch (ClientProtocolException e) {
					message = ConverterAppConstants.CLIENT_PROTOCOL_EXCEPTION;
					Log.d(LOG_TAG, e.getMessage());
					sendBroadcast(message);
				} catch (IOException e) {
					message = ConverterAppConstants.IO_EXCEPTION;
					Log.d(LOG_TAG, e.getMessage());
					// TODO broadcast no internet message or something like this
					sendBroadcast(message);
				} catch (IllegalArgumentException e) {
					message = ConverterAppConstants.ILLEGAL_ARGUMENT;
					Log.d(LOG_TAG, e.getMessage());
					sendBroadcast(message);
				}
				Logger.debug("RatesLoader doInBackground response: " + response);
				// load from "free currency converter"
				if (response < 0) {
					try {
						response = getCurrencyJsonFR(fromCurr, toCurr);
					} catch (ClientProtocolException e) {
						message = ConverterAppConstants.CLIENT_PROTOCOL_EXCEPTION;
						Log.d(LOG_TAG, e.getMessage());
						sendBroadcast(message);
					} catch (IOException e) {
						message = ConverterAppConstants.IO_EXCEPTION;
						Log.d(LOG_TAG, e.getMessage());
						// TODO broadcast no internet message or something like
						// this
						sendBroadcast(message);
					} catch (IllegalArgumentException e) {
						message = ConverterAppConstants.ILLEGAL_ARGUMENT;
						Log.d(LOG_TAG, e.getMessage());
						sendBroadcast(message);
					}
				}
				if (response > 0) {
					message = ConverterAppConstants.VALUES_LOADED;
					sendBroadcast(message);
					return response;
				}
			}
			return -1d;
		}

		private void sendBroadcast(int message) {
			Intent broadcastIntent = new Intent(ACTION_PARSING_ERROR);
			broadcastIntent.putExtra(ACTION_PARSING_MESSAGE, message);
			context.sendBroadcast(broadcastIntent);
		}

		@Override
		protected void onPostExecute(Double result) {
			if (result != -1 && result > 0) {

				switch (getterType) {
				case INSERT:
					insertCurrenciesRatioToDb(result, fromCurr, toCurr);
					break;

				case UPDATE:
					updateCurrenciesRatioToDb(result, rowID);
					break;

				default:
					break;
				}
				// notify activity, that loading is complete
				int message = ConverterAppConstants.VALUES_LOADED;
				sendBroadcast(message);
			}
		}

		public CurrencyLoader(CurrencyGetterType getterType) {
			this.getterType = getterType;
		}

		public CurrencyLoader(CurrencyGetterType getterType, int id) {
			this.getterType = getterType;
			this.rowID = id;
		}

	}

	private double getCurrencyRatesFromYahoo(String fromCurr, String toCurr) throws IOException {
		// create for calling to yahoo api
		String urlString = ConverterAppConstants.startUrlYahoo+fromCurr+toCurr+ConverterAppConstants.endUrlYahoo;
		URL url = new URL(urlString);
		URLConnection connection = url.openConnection();
		BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String result= "";
		String inputLine;
		while((inputLine = br.readLine()) != null){
			System.out.println(inputLine);
			result+= inputLine;
		}
		// split response
		String[] output = result.split(",");
		// the rates are the second value in CS
		String ratesOutput = output[1];
		return Double.valueOf(ratesOutput);
	}

	/**
	 * 
	 * @param from
	 *            represents currency to convert
	 * @param to
	 *            represents second currency
	 * @return ratio of the currencies in String representation
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	private double getCurrencyJsonRE(String from, String to)
			throws ClientProtocolException, IOException,
			IllegalArgumentException {
		// build url from parameters
		StringBuilder currBuilder = new StringBuilder(
				ConverterAppConstants.startUrlRE);
		currBuilder.append(from).append(ConverterAppConstants.secondPart)
				.append(to);
		Log.d(LOG_TAG, "RatesLoader.getCurrencyJsonRE json String: "
				+ currBuilder.toString());
		return jsonToDoubleRatioRE(loadJsonFromHttp(currBuilder));
	}

	/**
	 * Load rates from www.freecurrencyconverterapi.com site
	 * 
	 * @param from
	 *            represents currency to convert
	 * @param to
	 *            represents second currency
	 * @return ratio of the currencies in String representation
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	private double getCurrencyJsonFR(String from, String to)
			throws ClientProtocolException, IOException,
			IllegalArgumentException {
		// build url from parameters
		StringBuilder currBuilder = new StringBuilder(
				ConverterAppConstants.startUrlFR);
		currBuilder.append(from).append("-").append(to);
		Log.d(LOG_TAG, "RatesLoader.getCurrencyJsonFR json String: "
				+ currBuilder.toString());
		return jsonToDoubleRatioFR(loadJsonFromHttp(currBuilder), from, to);
	}

	public String loadJsonFromHttp(StringBuilder currBuilder)
			throws IOException, ClientProtocolException {
		StringBuilder builder = new StringBuilder();
		HttpClient client = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(currBuilder.toString());

		HttpResponse response = client.execute(httpGet);
		StatusLine statusLine = response.getStatusLine();
		int statusCode = statusLine.getStatusCode();
		if (statusCode == 200) {
			HttpEntity entity = response.getEntity();
			InputStream content = entity.getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					content));
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
		}
		Log.d(LOG_TAG,
				"RatesLoader.getCurrencyJson json response: "
						+ builder.toString());
		return builder.toString();
	}

	/**
	 * 
	 * @param jsonString
	 *            response from google currencies
	 * @return number represent ratio between two currencies
	 */
	private double jsonToDoubleRatioRE(String jsonString) {
		if (null == jsonString) {
			return -1d;
		}
		try {
			Log.d(LOG_TAG, "jsonString: " + jsonString);
			JSONObject jsonObj = new JSONObject(jsonString);
			String responseString = jsonObj
					.getString(ConverterAppConstants.GOOGLE_JSON_CODE);
			Log.d(LOG_TAG, "Json response value(rates): " + responseString);
			return Double.valueOf(responseString);
		} catch (JSONException e) {
			e.printStackTrace();
			return -1d;
		}
	}

	/**
	 * 
	 * @param jsonString
	 *            response from google currencies
	 * @return number represent ratio between two currencies
	 */
	private double jsonToDoubleRatioFR(String jsonString, String fromCurr, String toCurr) {
		if (null == jsonString) {
			return -1d;
		}
		try {
			Log.d(LOG_TAG, "jsonString: " + jsonString);
			// get Parent object
			StringBuilder currencyPairBuilder = new StringBuilder(fromCurr);
			currencyPairBuilder.append("-").append(toCurr);

			// get the json
			JSONObject parentObject = new JSONObject(jsonString);
			Log.d(LOG_TAG, "currencyObject " + parentObject);
			// get the values of "results" tag
			JSONObject resultsObject = parentObject.getJSONObject("results");
			Log.d(LOG_TAG, "resultsObject " + resultsObject);
			// get the result of conversion
			JSONObject jsonObj = resultsObject
					.getJSONObject((currencyPairBuilder.toString()));
			String responseString = jsonObj.getString("val");
			Log.d(LOG_TAG, "Json response value(rates): " + responseString);
			return Double.valueOf(responseString);
		} catch (JSONException e) {
			e.printStackTrace();
			return -1d;
		}
	}

	public enum CurrencyGetterType {
		INSERT, UPDATE;
	}

}
