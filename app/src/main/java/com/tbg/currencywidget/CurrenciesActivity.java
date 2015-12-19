package com.tbg.currencywidget;

import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;
import com.tbg.currencywidget.data.CurrenciesAdapter;
import com.tbg.currencywidget.data.CurrenciesAllAdapater;
import com.tbg.currencywidget.data.CurrenciesContentProvider;
import com.tbg.currencywidget.data.CurrenciesRatesContentProvider;
import com.tbg.currencywidget.data.CurrenciesRatesTable;
import com.tbg.currencywidget.data.CurrenciesTable;
import com.tbg.currencywidget.widget.CurrencyWidget;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

//public class CurrenciesActivity extends FragmentActivity implements
//		LoaderManager.LoaderCallbacks<Cursor>, OnClickListener,
//		OnItemClickListener
public class CurrenciesActivity extends FragmentActivity implements
        LoaderCallbacks<Cursor>, OnClickListener,
        OnItemClickListener {
    private InterstitialAd mInterstitialAd;

    private static final int USD_POSITION = 82;
    private static final int ACTIVITY_LOADER_ID = 197;
    private static final int SEARCH_LOADER_ID = 199;
    private static final int DIALOG_LOADER_ID = 198;
    private static final int FAVORITES_TAB_INDEX = 0;
    private static final int ALL_TAB_INDEX = 1;
    public final static int FROM_CURRENCY_SELECTOR = 15;
    public final static int TO_CURRENCY_SELECTOR = 16;
    // private Cursor cursor;
    private Button mBtnSubmit;
    private TextView tvFrom;
    private TextView tvTo;
    private ImageView ivTo;
    private ImageView ivFrom;

    private EditText etFrom;
    private EditText etTo;
    private TextView tvResult;
    private CurrenciesAdapter mDialogCursorAdapter;
    private CurrenciesAdapter allCurrenciesCursorAdapter;
    private long fromPosition;
    private long toPosition;
    private Loader<Cursor> currenciesLoader = null;
    private float exchangeRatio;
    private RatesLoader ratesLoader;
    private static String fromString;
    private static String toString;
    private DecimalFormat df;

    int widgetID = AppWidgetManager.INVALID_APPWIDGET_ID;
    Intent resultValue;
    private Resources res;
    private Cursor ratesCursor;

    private final int ACTIVITY_LAYOUT = R.layout.activity_layout;

    private class TabCurrenciesFilter implements FilterQueryProvider {


        @Override
        public Cursor runQuery(CharSequence constraint) {
            String selection = CurrenciesTable.COLUMN_ABBREVIATIONS
                    + " LIKE ? OR " + CurrenciesTable.COLUMN_CURRENCIES_NAME
                    + " LIKE ?";
            String valu = constraint.toString();
            String[] selectionArgs = new String[]{"%" + valu + "%",
                    "%" + valu + "%"};
            Cursor curs = getContentResolver().query(
                    CurrenciesContentProvider.CONTENT_URI, null, selection,
                    selectionArgs, null);
            return curs;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        Uri ratesUri = CurrenciesRatesContentProvider.CONTENT_URI;
        this.getApplicationContext()
                .getContentResolver()
                .registerContentObserver(ratesUri, true,
                        new MyContentObserver(null));

        IntentFilter filter = new IntentFilter(RatesLoader.ACTION_PARSING_ERROR);
        registerReceiver(messageReceiver, filter);
    }

    @Override
    protected void onPause() {
        unregisterReceiver(messageReceiver);
        super.onPause();
    }

    class MyContentObserver extends ContentObserver {
        public MyContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            Logger.debug("MyContentObserver.onChange( " + selfChange + ")");
            super.onChange(selfChange);
            // Do Work
            loadRatio();
        }
    }

    @Override
    protected void onStop() {
        if (ratesCursor != null) {
            ratesCursor.close();
        }
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // First get id of configurable widget
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        res = getResources();
        exchangeRatio = 0; // init exchange ratio

        if (extras != null) {
            widgetID = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // and checking if it's correct
        if (widgetID == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

        // configure response of the intent
        resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);

        // negative response
        setResult(RESULT_CANCELED, resultValue);




        // get position of stored currencies
        SharedPreferences sharedPreferences = getSharedPreferences(
                ConverterAppConstants.WIDGET_PREF, MODE_PRIVATE);
        fromPosition = sharedPreferences.getInt(
                ConverterAppConstants.WIDGET_FROM_POSITION + widgetID, 23);
        toPosition = sharedPreferences.getInt(
                ConverterAppConstants.WIDGET_TO_POSITION + widgetID,
                USD_POSITION); // usd poisition
        fromValue = sharedPreferences.getFloat(
                ConverterAppConstants.WIDGET_CONVERTED_FROM_AMOUNT + widgetID,
                1);
        toValue = sharedPreferences.getFloat(
                ConverterAppConstants.WIDGET_CONVERTED_TO_AMOUNT + widgetID, 1);

        // load rates
        // ratesLoader = RatesLoader.getInstance(this, this);
        ratesLoader = RatesLoader.getInstance();
        df = new DecimalFormat("#.###", new DecimalFormatSymbols(Locale.US));
        initViews(); // init views
        fillData(); // fill Activity variables with data
        initAdds();


    }

    private void initAdds() {
        // banner adds
        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        // interstital add
        mInterstitialAd = new InterstitialAd(getApplicationContext());
        String interstitialAddString = getResources().getString(R.string.interstitial_ad_unit_id);
        mInterstitialAd.setAdUnitId(interstitialAddString);

        //test for git

//        mInterstitialAd.setAdListener(new AdListener() {
//            @Override
//            public void onAdClosed() {
//                requestNewInterstitial();
////                beginPlayingGame();
//            }
//        });

//        requestNewInterstitial();
    }

    private void requestNewInterstitial() {
//        AdRequest adRequest = new AdRequest.Builder()
//                .addTestDevice("SEE_YOUR_LOGCAT_TO_GET_YOUR_DEVICE_ID")
//                .build();

//        mInterstitialAd.loadAd(adRequest);
    }

    private void initViews() {
        setContentView(ACTIVITY_LAYOUT);
        //
        // request focus to remove focusing on text view and opening keyboard on
        // start
        findViewById(R.id.mainLayout).requestFocus();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        etFrom = (EditText) findViewById(R.id.et_activity_from);
        etTo = (EditText) findViewById(R.id.et_activity_to);
        tvFrom = (TextView) findViewById(R.id.tv_from_curr);
        tvFrom.setOnClickListener(this);
        tvTo = (TextView) findViewById(R.id.tv_to_curr);
        tvTo.setOnClickListener(this);
        ivTo = (ImageView) findViewById(R.id.iv_activity_to);
        ivTo.setOnClickListener(this);
        ivFrom = (ImageView) findViewById(R.id.iv_activity_from);
        ivFrom.setOnClickListener(this);

        tvResult = (TextView) findViewById(R.id.tv_result);

        mBtnSubmit = (Button) findViewById(R.id.btn_submit);
        mBtnSubmit.setOnClickListener(this);

        TextWatcher etFromListener = new TextChangeListener(etFrom);
        TextWatcher etToListener = new TextChangeListener(etTo);
        etTo.addTextChangedListener(etToListener);
        etFrom.addTextChangedListener(etFromListener);

        ivFrom.setImageDrawable(Utils.getCurrencyDrawable(
                CurrenciesActivity.this, fromString));
        ivTo.setImageDrawable(Utils.getCurrencyDrawable(
                CurrenciesActivity.this, toString));
        tvFrom.setText(fromString);
        tvTo.setText(toString);
    }

    private void fillData() {
        // initializing loader
        if (currenciesLoader == null) {
            currenciesLoader = getSupportLoaderManager().initLoader(
                    ACTIVITY_LOADER_ID, null, this);
        } else {
            getSupportLoaderManager().restartLoader(ACTIVITY_LOADER_ID, null,
                    this);
        }
        String fromCurrency = tvFrom.getText().toString();
        String toCurrency = tvTo.getText().toString();
        // check if rates update
        ratesLoader.loadRates(fromCurrency, toCurrency);
        loadRatio();
        // clear text
        if (exchangeRatio > 0) {
            // make check what is the whole number, to make it base converted
            // value
            if (fromValue % 1 == 0) {
                toValue = fromValue * exchangeRatio;
            } else if (toValue % 1 == 0) {
                fromValue = toValue / exchangeRatio;
            } else {
                toValue = fromValue * exchangeRatio;
            }
            etFrom.setText(df.format(fromValue));
            etTo.setText(String.valueOf(toValue));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.listmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settins:
                showSettingsDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Dialog with settings for db updates interval.
     */
    private void showSettingsDialog() {
        final Dialog settingsDialog = new Dialog(CurrenciesActivity.this);
        settingsDialog.setContentView(R.layout.settings);
        settingsDialog.setTitle(R.string.update_interval);

        ListView lvInterval = (ListView) settingsDialog
                .findViewById(R.id.lv_settings_interval);
        final String[] intervalNames = res
                .getStringArray(R.array.update_intervals_names);
        final int[] intervalValues = res.getIntArray(R.array.update_intervals);
        ArrayAdapter<String> intervalAdapter = new ArrayAdapter<String>(
                getApplicationContext(),
                android.R.layout.simple_expandable_list_item_1, intervalNames);
        lvInterval.setAdapter(intervalAdapter);
        lvInterval.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapter, View view,
                                    int position, long arg3) {
//				String text = "You clicked " + intervalNames[position]
//						+ " it is " + intervalValues[position];
//				Utils.showShortToast(text, getApplicationContext());
                // save them to shared prefs
                SharedPreferences sharedPrefs = getSharedPreferences(
                        ConverterAppConstants.WIDGET_PREF, MODE_PRIVATE);
                Editor editor = sharedPrefs.edit();
                // save update interval to shared preferences
                editor.putInt(ConverterAppConstants.WIDGET_UPDATE_INTERVAL
                        + widgetID, intervalValues[position]);
                editor.commit();
                // rearm alarm Manager
                CurrencyWidget.disableAlarmManager(getApplicationContext(),
                        widgetID);
                CurrencyWidget.setAlarmManager(getApplicationContext(),
                        widgetID);
                settingsDialog.dismiss();
            }
        });

        settingsDialog.show();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        return super.onContextItemSelected(item);
    }

    public void onLoadFinished(
            android.support.v4.content.Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case ACTIVITY_LOADER_ID:
                initExchangeViews(cursor);
                // init show all currencies dialog
                if (allCurrenciesCursorAdapter != null) {
                    allCurrenciesCursorAdapter.swapCursor(cursor);
                }
                break;

            case DIALOG_LOADER_ID:
                mDialogCursorAdapter.swapCursor(cursor);
                break;
            // load rates
            case SEARCH_LOADER_ID:
                mDialogCursorAdapter.swapCursor(cursor);
            default:
                break;
        }
        ratesLoader.loadRates(fromString, toString);
        initViews();
        loadRatio();
    }

    /**
     * Load currencies pair, that you want to use in your activity
     *
     * @param cursor
     */
    private void initExchangeViews(Cursor cursor) {
        String fromString = null;
        String toString = null;
        if (cursor != null) {
            Logger.debug("Cursor not null");
            while (cursor.moveToNext()) {
                if (cursor.getInt(cursor
                        .getColumnIndex(CurrenciesTable.COLUMN_ID)) == fromPosition) {
                    Logger.debug("found from string");
                    fromString = cursor
                            .getString(cursor
                                    .getColumnIndex(CurrenciesTable.COLUMN_ABBREVIATIONS));

                }
                if (cursor.getInt(cursor
                        .getColumnIndex(CurrenciesTable.COLUMN_ID)) == toPosition) {
                    Logger.debug("initExchangeViews found to string");
                    toString = cursor
                            .getString(cursor
                                    .getColumnIndex(CurrenciesTable.COLUMN_ABBREVIATIONS));
                }
            }

        }
        CurrenciesActivity.fromString = fromString;
        CurrenciesActivity.toString = toString;
        loadRatio();
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> arg0) {
        // data is not available anymore, delete reference
        // adapter.swapCursor(null);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id,
                                                                    Bundle bundle) {
        CursorLoader cursorloader = null;
        // int bndlValue = bundle.getInt(BUNDLE_VALUE);
        switch (id) {
            case ACTIVITY_LOADER_ID:
                cursorloader = new CursorLoader(getApplicationContext(),
                        CurrenciesContentProvider.CONTENT_URI, null, null, null,
                        null);
                return cursorloader;
            case DIALOG_LOADER_ID:
                String[] projection = {CurrenciesTable.COLUMN_ID,
                        CurrenciesTable.COLUMN_ABBREVIATIONS,
                        CurrenciesTable.COLUMN_CURRENCIES_NAME,
                        CurrenciesTable.COLUMN_CURRENCIES_FAVORITES};
                cursorloader = new CursorLoader(this,
                        CurrenciesContentProvider.CONTENT_URI, projection,
                        CurrenciesTable.COLUMN_CURRENCIES_FAVORITES + "=?",
                        new String[]{"1"}, null);
                return cursorloader;
            // get rates from rates table
            // case SEARCH_LOADER_ID:
            // String[] projection1 = { CurrenciesTable.COLUMN_ID,
            // CurrenciesTable.COLUMN_ABBREVIATIONS,
            // CurrenciesTable.COLUMN_CURRENCIES_NAME,
            // CurrenciesTable.COLUMN_CURRENCIES_FAVORITES };
            // cursorloader = new CursorLoader(this,
            // CurrenciesContentProvider.CONTENT_URI, projection1,
            // CurrenciesTable.COLUMN_ABBREVIATIONS + "=?",
            // new String[] { "USD" }, null);
            // return cursorloader;
            default:
                break;
        }

        return cursorloader;
    }

    @Override
    public void onClick(View v) {
        SharedPreferences sp;
        switch (v.getId()) {
            case R.id.tv_from_curr:
            case R.id.iv_activity_from:
                // showFavoriteCurrenciesDialog(FROM_CURRENCY_SELECTOR);
                showTabbedDialog(FROM_CURRENCY_SELECTOR);
                break;
            case R.id.tv_to_curr:
            case R.id.iv_activity_to:
                showTabbedDialog(TO_CURRENCY_SELECTOR);
                break;
            case R.id.btn_submit:
                if(mInterstitialAd.isLoaded()){
                    mInterstitialAd.show();
                } else {
                    Toast.makeText(getApplication().getApplicationContext(),
                            "Interstitial ad was not ready", Toast.LENGTH_SHORT).show();
                }
                sp = saveValuesToSharedPrefs(fromPosition, toPosition);
                updateWidgetAndClose(sp);

                break;
            default:
                break;
        }
    }

    private void updateWidgetAndClose(SharedPreferences sharedPrefs) {
        // update widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        CurrencyWidget.updateWidget(this, appWidgetManager, widgetID);

        // positive reply
        setResult(RESULT_OK, resultValue);

        Logger.debug("finish config " + widgetID);
        finish();
    }

    /**
     * get the values from currency spinners and save them to shared preferences
     *
     * @param
     * @return
     */
    private SharedPreferences saveValuesToSharedPrefs(long fromPosition,
                                                      long toPosition) {
        // String fromCurrency = currencyAbbreviations[(int) fromPosition];
        // String toCurrency = currencyAbbreviations[(int) toPosition];
        String fromCurrency = fromString;
        String toCurrency = toString;
        // for some phone models replace "comma" in numbers value to "dot"
        String fromCurrencyString = etFrom.getText().toString()
                .replace(",", ".");
        String toCurrencyString = etTo.getText().toString().replace(",", ".");
        // check if there is another weird symbols
        toCurrencyString = toCurrencyString.replace("∞", ".");
        // get the amount of converted money "from" field
        float fromCurrencyAmount = (fromCurrencyString.length() > 0) ? Float
                .valueOf(fromCurrencyString) : 1;

        // get the amount of converted money "to" field
        float toCurrencyAmount = (toCurrencyString.length() > 0) ? Float
                .valueOf(toCurrencyString) : 1;

        // save them to shared prefs
        SharedPreferences sharedPrefs = getSharedPreferences(
                ConverterAppConstants.WIDGET_PREF, MODE_PRIVATE);
        Editor editor = sharedPrefs.edit();
        editor.putString(ConverterAppConstants.WIDGET_FROM_TEXT + widgetID,
                fromCurrency);
        editor.putString(ConverterAppConstants.WIDGET_TO_TEXT + widgetID,
                toCurrency);
        editor.putInt(ConverterAppConstants.WIDGET_FROM_POSITION + widgetID,
                (int) fromPosition);
        editor.putInt(ConverterAppConstants.WIDGET_TO_POSITION + widgetID,
                (int) toPosition);
        editor.putFloat(ConverterAppConstants.WIDGET_CONVERTED_FROM_AMOUNT
                + widgetID, fromCurrencyAmount);
        editor.putFloat(ConverterAppConstants.WIDGET_CONVERTED_TO_AMOUNT
                + widgetID, toCurrencyAmount);
        editor.commit();

        return sharedPrefs;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        String[] projection = {CurrenciesTable.COLUMN_ID,
                CurrenciesTable.COLUMN_ABBREVIATIONS,
                CurrenciesTable.COLUMN_CURRENCIES_NAME,
                CurrenciesTable.COLUMN_CURRENCIES_FAVORITES};
        String selection = CurrenciesTable.COLUMN_ID + "=?";
        String[] selectionArgs = new String[]{String.valueOf(id)};
        Cursor cursor = getContentResolver().query(
                CurrenciesContentProvider.CONTENT_URI, projection, selection,
                selectionArgs, null);
        if (cursor.moveToNext()) {
            String currencyName = cursor.getString(cursor
                    .getColumnIndex(CurrenciesTable.COLUMN_CURRENCIES_NAME));
            int isFavorite = cursor
                    .getInt(cursor
                            .getColumnIndex(CurrenciesTable.COLUMN_CURRENCIES_FAVORITES));
            Toast.makeText(getApplicationContext(),
                    currencyName + " is favorite: " + isFavorite,
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Tabbed Dialog with currencies rates
     */
    private void showTabbedDialog(final int from) {

        // create Query filter for search in tabs
        FilterQueryProvider filter = new TabCurrenciesFilter();
        String title = null;
        Resources res = getApplicationContext().getResources();
        selectedTabIndex = 0;
        switch (from) {
            case CurrenciesActivity.FROM_CURRENCY_SELECTOR:
                title = res.getString(R.string.from);
                break;

            case CurrenciesActivity.TO_CURRENCY_SELECTOR:
                title = res.getString(R.string.to);
                break;

            default:
                break;
        }

        final Dialog dialog = new Dialog(CurrenciesActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.tab_dialog_layout);
        dialog.setTitle(title);

        final TabHost tabs = (TabHost) dialog.findViewById(R.id.tabhost);

        tabs.setup();
        tabs.setOnTabChangedListener(new OnTabChangeListener() {

            @Override
            public void onTabChanged(String tabId) {
                selectedTabIndex = tabs.getCurrentTab();
            }
        });

        TabHost.TabSpec favoritesTab = tabs.newTabSpec("one");
        favoritesTab.setContent(R.id.lv_tab_favorites);
        favoritesTab.setIndicator("Favorites");

        ListView lv = (ListView) dialog.findViewById(R.id.lv_tab_favorites);
        mDialogCursorAdapter = new CurrenciesAdapter(getApplicationContext(),
                null, true);
        mDialogCursorAdapter.setFilterQueryProvider(filter);
        lv.setAdapter(mDialogCursorAdapter);
        lv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                setCurrencyValueAccordingToDialog(from, id);
                // restart loaders when dialog closed
                getSupportLoaderManager().restartLoader(ACTIVITY_LOADER_ID,
                        null, CurrenciesActivity.this);
                updateValuesOnDialogClosed();

                dialog.dismiss();
            }
        });

        TabHost.TabSpec allTabs = tabs.newTabSpec("two");
        allTabs.setContent(R.id.lv_tab_all);
        allTabs.setIndicator("All");
        ListView lv_all = (ListView) dialog.findViewById(R.id.lv_tab_all);
        allCurrenciesCursorAdapter = new CurrenciesAllAdapater(
                getApplicationContext(), null, true);
        lv_all.setAdapter(allCurrenciesCursorAdapter);

        getSupportLoaderManager().restartLoader(ACTIVITY_LOADER_ID, null,
                CurrenciesActivity.this);

        lv_all.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                ViewGroup group = (ViewGroup) ((ViewGroup) view).getChildAt(0); // nested
                // group
                Switch sw = (Switch) group.getChildAt(1); // index of switch
//				SwitchCompat sw = (SwitchCompat) group.getChildAt(1); // index of switch
                int answer = CurrenciesAdapter.getValueOfCheckbox(sw, (int) id);
                if (answer == 0) {
                    CurrenciesAdapter.updateFavoriteValue(
                            CurrenciesActivity.this, (int) id, 1); // update
                    // with 1
                } else if (answer == 1) {
                    CurrenciesAdapter.updateFavoriteValue(
                            CurrenciesActivity.this, (int) id, 0); // update
                    // with 0
                }
                setCurrencyValueAccordingToDialog(from, id);
                dialog.dismiss();
                updateValuesOnDialogClosed();
            }
        });
        tabs.addTab(favoritesTab);
        tabs.addTab(allTabs);
        int tabCount = tabs.getTabWidget().getChildCount();

        // change text color for Tab
        for (int i = 0; i < tabCount; i++) {
            TabWidget tabWidget = tabs.getTabWidget();
            tabWidget.setBackgroundColor(res.getColor(R.color.silver));
            TextView tv = (TextView) tabs.getTabWidget().getChildAt(i)
                    .findViewById(android.R.id.title);
            tv.setTextColor(res.getColor(R.color.foreground_darker));

        }

        getSupportLoaderManager().restartLoader(DIALOG_LOADER_ID, null,
                CurrenciesActivity.this);

        EditText eSearch = (EditText) dialog.findViewById(R.id.et_search);
        eSearch.setBackgroundColor(res.getColor(R.color.light_grey));

        // set filter for searching in currencies
        allCurrenciesCursorAdapter.setFilterQueryProvider(filter);

        eSearch.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
                if (selectedTabIndex == FAVORITES_TAB_INDEX) {
                    mDialogCursorAdapter.getFilter().filter(s);
                } else if (selectedTabIndex == ALL_TAB_INDEX) {
                    allCurrenciesCursorAdapter.getFilter().filter(s);
                }

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
                // TODO Auto-generated method stub

            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub

            }
        });

        dialog.show();
    }

    /**
     *
     */
    private void updateValuesOnDialogClosed() {
        loadRatio(); // first - update ratio cursor
        fillData(); // load new values to views
        clearViews();
    }

    private void clearViews() {
        etFrom.setText("");
        etTo.setText("");
    }

    /**
     * Monitors two EditText fields and updates values in one field according to
     * values in second field
     *
     * @author Pavel
     */
    class TextChangeListener implements TextWatcher {

        private View editText;

        public TextChangeListener(View view) {
            this.editText = view;
        }

        @Override
        public void afterTextChanged(Editable s) {

        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int adter) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
            if (exchangeRatio == 0) {

            }

            switch (editText.getId()) {
                case R.id.et_activity_from:
                    // update only if field is in focus
                    if (etFrom.isFocused()) {
                        Logger.debug("Currencies Activity.TextChangeListener.onTextChanged Updating \"from\" field exchangeRatio: "
                                + exchangeRatio);
                        if (s.length() > 0) {
                            double currentValue = Double.valueOf(s.toString());
                            double convertedValue = currentValue * exchangeRatio;
                            etTo.setText(df.format(convertedValue));
                        } else {
                            etTo.setText("");
                        }
                    }
                    break;
                case R.id.et_activity_to:
                    if (etTo.isFocused()) {
                        Logger.debug("Currencies Activity.TextChangeListener.onTextChanged Updating \"to\"  field exchangeRatio: "
                                + exchangeRatio);
                        if (s.length() > 0) {
                            double currentValue = Double.valueOf(s.toString());
                            double convertedValue = currentValue / exchangeRatio;
                            etFrom.setText(df.format(convertedValue));
                        } else {
                            etFrom.setText("");
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * set the currencies variable value according to values stored in db
     */
    private void loadRatio() {
        if (fromString == null || toString == null) {
            Logger.debug("CurrenciesActivity.setRatio from string or to string is null");
            return;
        }
        Uri ratesUri = CurrenciesRatesContentProvider.CONTENT_URI;
        ContentResolver cr = getContentResolver();
        String[] projection = null;
        String selection = CurrenciesRatesTable.COLUMN_FROM_CURRENCY
                + "=? AND " + CurrenciesRatesTable.COLUMN_TO_CURRENCY + "=?";
        String[] selectionArgs = {fromString, toString};
        ratesCursor = cr.query(ratesUri, projection, selection, selectionArgs,
                null);
        try {
            if (ratesCursor != null) {
                while (ratesCursor.moveToNext()) {
                    Logger.debug("CurrenciesActivity.loadRatio loading rates for "
                            + fromString + " to " + toString);
                    this.exchangeRatio = ratesCursor
                            .getFloat(ratesCursor
                                    .getColumnIndex(CurrenciesRatesTable.COLUMN_EXCHANGE_RATE));
                }
            }
        } finally {
            if (ratesCursor != null) {
                ratesCursor.close();
            }
        }
    }

    /**
     * @param from
     * @param id
     */
    private void setCurrencyValueAccordingToDialog(final int from, long id) {
        switch (from) {
            case FROM_CURRENCY_SELECTOR:
                fromPosition = id;
                break;

            case TO_CURRENCY_SELECTOR:
                toPosition = id;
                break;

            default:
                break;
        }
    }

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {

        // Receive broadcast from Rates Loader
        @Override
        public void onReceive(Context context, Intent intent) {
            int messageValue = intent.getIntExtra(
                    RatesLoader.ACTION_PARSING_MESSAGE, 0);
            String message = "";
            switch (messageValue) {
                case ConverterAppConstants.CLIENT_PROTOCOL_EXCEPTION:
                    message = res.getString(R.string.something_wrong);
                    break;

                case ConverterAppConstants.IO_EXCEPTION:
                    message = res.getString(R.string.no_internet);
                    break;

                case ConverterAppConstants.ILLEGAL_ARGUMENT:
                    message = res.getString(R.string.something_wrong);
                    break;
                case ConverterAppConstants.VALUES_LOADED:
                    message = "";
                    break;

                default:
                    break;
            }
            tvResult.setText(message);
        }

    };
    private float fromValue;
    private float toValue;
    private int selectedTabIndex;

}
