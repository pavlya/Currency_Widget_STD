package com.tbg.currencywidget.data;


import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.tbg.currencywidget.R;

public class CurrenciesAllAdapater extends CurrenciesAdapter {

	public CurrenciesAllAdapater(Context context, Cursor c, boolean autoRequery) {
		super(context, c, autoRequery);
		// TODO Auto-generated constructor stub
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

		final int id = cursor.getInt(cursor.getColumnIndex(CurrenciesTable.COLUMN_ID));

		currName = cursor.getString(cursor
				.getColumnIndex(CurrenciesTable.COLUMN_CURRENCIES_NAME));
		TextView tvCurrenciesNames = (TextView) view
				.findViewById(R.id.tv_curr_name);
		tvCurrenciesNames.setText(currName);

		ImageView ivIcon = (ImageView) view.findViewById(R.id.iv_icon_currency);
		ivIcon.setImageDrawable(getCurrencyDrawable(context, currAbbreviation));

//		Switch cbFavorites = (Switch) view.findViewById(R.id.cb_favorite);
//		Switch
//		cbFavorites.setVisibility(View.INVISIBLE);
	}

}
