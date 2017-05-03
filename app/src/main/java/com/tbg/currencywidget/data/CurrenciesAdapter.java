package com.tbg.currencywidget.data;


import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.tbg.currencywidget.ConverterAppConstants;
import com.tbg.currencywidget.Logger;
import com.tbg.currencywidget.R;

/**
 * 
 * Adapter for dialog with currencies displayed in it
 * 
 */
public class CurrenciesAdapter extends CursorAdapter {

	private LayoutInflater layoutInflater;

	Context context;

	public CurrenciesAdapter(Context context, Cursor c, boolean autoRequery) {
		super(context, c, autoRequery);
		this.context = context;
		layoutInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public void bindView(View view, final Context context, Cursor cursor) {
		String currName = null;
		String currAbbreviation = null;
		int favorite = 0;

		currAbbreviation = cursor.getString(cursor
				.getColumnIndex(CurrenciesTable.COLUMN_ABBREVIATIONS));
		TextView tvAbbreviation = (TextView) view
				.findViewById(R.id.tv_abbreviation);
		tvAbbreviation.setText(currAbbreviation);

		final int id = cursor.getInt(cursor
				.getColumnIndex(CurrenciesTable.COLUMN_ID));

		currName = cursor.getString(cursor
				.getColumnIndex(CurrenciesTable.COLUMN_CURRENCIES_NAME));
		TextView tvCurrenciesNames = (TextView) view
				.findViewById(R.id.tv_curr_name);
		tvCurrenciesNames.setText(currName);

		ImageView ivIcon = (ImageView) view.findViewById(R.id.iv_icon_currency);
		ivIcon.setImageDrawable(getCurrencyDrawable(context, currAbbreviation));

		Switch cbFavorites = (Switch) view.findViewById(R.id.cb_favorite);
		favorite = cursor.getInt(cursor
				.getColumnIndex(CurrenciesTable.COLUMN_CURRENCIES_FAVORITES));
		// init checkbox(switch)
		if (favorite == 1) {
			cbFavorites.setChecked(true);
		} else {
			cbFavorites.setChecked(false);
		}
		cbFavorites.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				updateCurrencyFavorites(context, v, id);
			}
		});

	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
		final View view = layoutInflater.inflate(R.layout.currency_row, null);
		return view;
	}

	public static void updateCurrencyFavorites(final Context context, View v,
			int id) {
		int favorite = getValueOfCheckbox(v, id);

		updateFavoriteValue(context, id, favorite);
	}

	public static void updateFavoriteValue(final Context context, int id,
			int favorite) {
		ContentValues values = new ContentValues();
		values.put(CurrenciesTable.COLUMN_CURRENCIES_FAVORITES, favorite);

		Uri currencyUri = Uri.parse(CurrenciesContentProvider.CONTENT_URI + "/"
				+ id);
		// update checkbox(switch)
		context.getContentResolver().update(currencyUri, values, null, null);
	}

	public static int getValueOfCheckbox(View v, int id) {
		Switch cb = (Switch) v;
		int favorite;
		Logger.debug("CurrenciesAdapter.upDateCurrencyFavorites id: " + id);
		if (cb.isChecked()) {
			// Toast.makeText(context, "is checked", Toast.LENGTH_SHORT).show();
			favorite = 1;
		} else {
			// Toast.makeText(context, "is unchecked",
			// Toast.LENGTH_SHORT).show();
			favorite = 0;
		}
		return favorite;
	}

	// convert currency abbreviation to drawable (using drawable name);
	public static Drawable getCurrencyDrawable(Context ctx,
			String currencyAbbreviation) {
		Resources res = ctx.getResources();
		Drawable drawable = null;
		if (currencyAbbreviation == null) {
			return res.getDrawable(R.drawable.place_holder);
		}
		String currencyString = currencyAbbreviation.toLowerCase();
		if (currencyString.equals(ConverterAppConstants.TurkishL)){
			currencyString = "_" + currencyString;
		}
		int drawableID = 0;
		try {
			drawableID = res.getIdentifier(currencyString, "drawable",
					ctx.getPackageName());
			drawable = res.getDrawable(drawableID);
			if (drawable != null)
				return drawable;
		} catch (Exception e) {
			// TODO: handle exception
		}

		return res.getDrawable(R.drawable.place_holder);
	}

}
