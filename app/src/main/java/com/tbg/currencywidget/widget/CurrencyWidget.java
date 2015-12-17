package com.tbg.currencywidget.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.widget.RemoteViews;

import com.tbg.currencywidget.ConverterAppConstants;
import com.tbg.currencywidget.CurrenciesActivity;
import com.tbg.currencywidget.Logger;
import com.tbg.currencywidget.R;
import com.tbg.currencywidget.RatesLoader;
import com.tbg.currencywidget.Utils;
import com.tbg.currencywidget.data.CurrenciesRatesContentProvider;
import com.tbg.currencywidget.data.CurrenciesRatesTable;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Locale;

import static com.tbg.currencywidget.ConverterAppConstants.WIDGET_FROM_TEXT;
import static com.tbg.currencywidget.ConverterAppConstants.WIDGET_PREF;
import static com.tbg.currencywidget.ConverterAppConstants.WIDGET_TO_TEXT;

public class CurrencyWidget extends AppWidgetProvider {

	public static final String UPDATE_CURRENCY_FILTER = "com.tbg.currencywidget.widget.UPDATE";

	@Override
	public void onReceive(Context context, Intent intent) {
		Logger.debug("CurrencyWidget.onReceive() " + intent.getAction());
		AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
		ComponentName widgetComponent = new ComponentName(
				context.getPackageName(), this.getClass().getName());
		int[] widgetId = widgetManager.getAppWidgetIds(widgetComponent);
		int widgetNum = widgetId.length;

		// !!!!!!!!!!!!!!!!!!!!!!!!
		// TODO register new content observer
		// and create new content observer inner class
		// context.getContentResolver().registerContentObserver(uri,
		// notifyForDescendents, observer)

		// init widget
		if (intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
			for (int i = 0; i < widgetNum; i++) {
				setAlarmManager(context, widgetId[i]);
			}
		} else if (intent.getAction().equals(UPDATE_CURRENCY_FILTER)) {
			for (int i = 0; i < widgetNum; i++) {
				// widgetManager.updateAppWidget(widgetId[i], widgetViews);
				// update all widget instances
				updateWidget(context, widgetManager, widgetId[i]);
				// rearm alarm managers
				// disableAlarmManager(context, widgetId[i]);
				// setAlarmManager(context, widgetId[i]);
			}

		} else if (intent.getAction().equals(
				AppWidgetManager.ACTION_APPWIDGET_DISABLED)) {
			for (int i = 0; i < widgetNum; i++) {
				disableAlarmManager(context, widgetId[i]);
			}
		} else if (intent.getAction().equals(
				AppWidgetManager.ACTION_APPWIDGET_DELETED)) {
			for (int i = 0; i < widgetNum; i++) {
				disableAlarmManager(context, widgetId[i]);
			}
		}
	}

	public static void disableAlarmManager(Context context, int widgetId) {
		Logger.debug("CurrencyWidget.disableAlarmManager " + widgetId);
		Intent updateIntent = new Intent(UPDATE_CURRENCY_FILTER);
		PendingIntent pendingUpdateInent = PendingIntent.getBroadcast(context,
				widgetId, updateIntent, 0);
		AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(pendingUpdateInent);
	}

	/**
	 * 
	 * @param context
	 * @param widgetId
	 *            used to create alarm manager according to widget
	 */
	public static void setAlarmManager(Context context, int widgetId) {
		Intent updateIntent = new Intent(UPDATE_CURRENCY_FILTER);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
				widgetId, updateIntent, 0);
		AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		// Update values from shared preferences
		SharedPreferences sharedPrefs = context.getSharedPreferences(
				ConverterAppConstants.WIDGET_PREF, Context.MODE_PRIVATE);
		int repeatingInterval = sharedPrefs.getInt(
				ConverterAppConstants.WIDGET_UPDATE_INTERVAL + widgetId, 60);
		Logger.debug("CurrencyWidget.setAlarmManager " + widgetId
				+ " repeatingInterval " + repeatingInterval);
		// update according to repeat interval or every 60 minutes
		alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(),
				1000 * 60 * repeatingInterval, pendingIntent);
	}

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
		// Create alarm Manager and run it every defined time
		// TODO Load interval from shared preference instead of hardcoded value
		// getPendingIntent(context);
	}

	// private void getPendingIntent(Context context) {
	// super.onEnabled(context);
	// PendingIntent anIntent = PendingIntent.getBroadcast(context, 0,
	// new Intent("CurrencyWidgetBroadcastReceiver"),
	// PendingIntent.FLAG_UPDATE_CURRENT);
	// AlarmManager alarmMgr = (AlarmManager) context
	// .getSystemService(Context.ALARM_SERVICE);
	// alarmMgr.setRepeating(AlarmManager.RTC, System.currentTimeMillis(),
	// 60000, anIntent);
	// }

	@Override
	public void onDisabled(Context context) {
		super.onDisabled(context);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		Logger.debug("On Update " + Arrays.toString(appWidgetIds));

		for (int id : appWidgetIds) {
			updateWidget(context, appWidgetManager, id);
		}
	}

	// private void loadCursor(Context context) {
	// // load data from currencies contet provider
	// Cursor cursor = context.getContentResolver().query(
	// CurrenciesContentProvider.CONTENT_URI, null, null, null, null);
	// }

	public static void updateWidget(Context context,
			AppWidgetManager appWidgetManager, int id) {

		SharedPreferences sharedPrefs = context.getSharedPreferences(
				WIDGET_PREF, Context.MODE_PRIVATE);
		Logger.debug("CurrencyWidget.updateWidget " + id);
		// read preferences
		// get the names and abbreviation of currencies

		String fromCurrencyAbbreviation = sharedPrefs.getString(
				ConverterAppConstants.WIDGET_FROM_TEXT + id, "EUR");
		String toCurrencyAbbreviation = sharedPrefs.getString(
				ConverterAppConstants.WIDGET_TO_TEXT + id, "USD");

		// update for firstime with provided values
		// Editor editor = sharedPrefs.edit();
		// editor.putString(ConverterApp.WIDGET_FROM_TEXT + id,
		// fromCurrencyAbbreviation);
		// editor.putString(ConverterApp.WIDGET_TO_TEXT + id,
		// toCurrencyAbbreviation);
		// editor.commit();
		float currencyFromAmount = sharedPrefs.getFloat(
				ConverterAppConstants.WIDGET_CONVERTED_FROM_AMOUNT + id, 1);
		float fromCurrencyValue = currencyFromAmount;
		float toCurrencyValue = sharedPrefs.getFloat(
				ConverterAppConstants.WIDGET_CONVERTED_TO_AMOUNT + id, 0);

		RatesLoader ratesLoader = RatesLoader.getInstance();
		Logger.debug("CurrencyWidget.updateWidget fromCurrency + "
				+ fromCurrencyAbbreviation + " toCurrency "
				+ toCurrencyAbbreviation);

		Logger.debug("CurrencyWidget.updateWidget fromCurrency amount: "
				+ fromCurrencyValue + " toCurrency amount" + toCurrencyValue);
		// update Rates
		ratesLoader.loadRates(fromCurrencyAbbreviation, toCurrencyAbbreviation);

		float rates = getRatio(context, fromCurrencyAbbreviation,
				toCurrencyAbbreviation);

		// check what field should be the "base" field, by checking what number
		// is a whole number
		if (fromCurrencyValue % 1 == 0) {
			toCurrencyValue = rates * currencyFromAmount;
		} else if (toCurrencyValue % 1 == 0) {
			fromCurrencyValue = toCurrencyValue / rates;
		} else {
			toCurrencyValue = rates * currencyFromAmount;
		}

		// show only 2 digits after ".", use US formatting, because need dot
		// instead of comma for separating decimals
		DecimalFormat df = new DecimalFormat("#.##", new DecimalFormatSymbols(
				Locale.US));
		String fromResponse = df.format(fromCurrencyValue);
		String toResponse = df.format(toCurrencyValue);

		// configure drawables
		int fromDrawableID = Utils.getCurrencyDrawableID(context,
				fromCurrencyAbbreviation);
		int toDrawableID = Utils.getCurrencyDrawableID(context,
				toCurrencyAbbreviation);

		// configuring widget appearance
		RemoteViews widgetView = new RemoteViews(context.getPackageName(),
				R.layout.widget);
		widgetView.setTextViewText(R.id.tv_widget_from, fromResponse);
		widgetView.setTextViewText(R.id.tv_widget_to, toResponse);
		widgetView.setImageViewResource(R.id.iv_widget_from, fromDrawableID);
		widgetView.setImageViewResource(R.id.iv_widget_to, toDrawableID);

		// load ConverterActivity onClick
		Intent configIntent = new Intent(context, CurrenciesActivity.class);
		configIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
		configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
		PendingIntent pIntent = PendingIntent.getActivity(context, id,
				configIntent, 0);
		widgetView.setOnClickPendingIntent(R.id.ll_widget_parent, pIntent);
		// rearm loaders
		// update widget
		appWidgetManager.updateAppWidget(id, widgetView);
	}

	// private static void startUpdatesScheduler(Context context, int id) {
	// // configure Alarm Manager
	// AlarmManager alarmManager = (AlarmManager) context
	// .getSystemService(Context.ALARM_SERVICE);
	// Intent intent = new Intent(context,
	// CurrencyWidgetBroadcastReceiver.class);
	// PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
	// // first call 30 seconds after install
	// // call with interval of 3 seconds
	// alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
	// System.currentTimeMillis() + 1000 * 30, 10 * 1000, pi);
	// }

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);

		// remove preference
		Editor editor = context.getSharedPreferences(WIDGET_PREF,
				Context.MODE_PRIVATE).edit();
		for (int widgetID : appWidgetIds) {
			editor.remove(WIDGET_FROM_TEXT + widgetID);
			editor.remove(WIDGET_TO_TEXT + widgetID);

			deletUpdatesScheduler(context, editor, widgetID);
		}

		editor.commit();
	}

	/**
	 * 
	 * @param context
	 * @param editor
	 * @param widgetID
	 *            used to delete alarm manager according to widget
	 */
	private void deletUpdatesScheduler(Context context, Editor editor,
			int widgetID) {

		// Intent intent = new Intent(context,
		// CurrencyWidgetBroadcastReceiver.class);
		// PendingIntent pi = PendingIntent.getBroadcast(context, widgetID,
		// intent, 0);
		// AlarmManager alarmManager = (AlarmManager) context
		// .getSystemService(Context.ALARM_SERVICE);
		// alarmManager.cancel(pi);
	}

	private static float getRatio(Context context, String fromString,
			String toString) {
		Uri ratesUri = CurrenciesRatesContentProvider.CONTENT_URI;
		ContentResolver cr = context.getContentResolver();
		String[] projection = null;
		String selection = CurrenciesRatesTable.COLUMN_FROM_CURRENCY
				+ "=? AND " + CurrenciesRatesTable.COLUMN_TO_CURRENCY + "=?";
		String[] selectionArgs = { fromString, toString };
		Cursor ratesCursor = cr.query(ratesUri, projection, selection,
				selectionArgs, null);
		float exchangeRatio = 0;
		while (ratesCursor.moveToNext()) {
			exchangeRatio = ratesCursor.getFloat(ratesCursor
					.getColumnIndex(CurrenciesRatesTable.COLUMN_EXCHANGE_RATE));
		}

		if (ratesCursor != null) {
			ratesCursor.close();
		}

		return exchangeRatio;
	}

}
