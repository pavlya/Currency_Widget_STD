package com.tbg.currencywidget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStreamWriter;

import static com.tbg.currencywidget.ConverterAppConstants.LOG_TAG;

public class Utils {

	private Utils(){
		
	}
	
	public static void writeToFile(String data, String filename, Context context) {
		try {
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(
					context.openFileOutput(filename, Context.MODE_PRIVATE));
			outputStreamWriter.write(data);
			outputStreamWriter.close();
		} catch (IOException e) {
			Log.e("Exception", "File write failed: " + e.toString());
		}
	}
	
	public static Drawable getCurrencyDrawable(Context ctx,
			String currencyAbbreviation) {
		Resources res = ctx.getResources();
		Drawable drawable = null;
		if (currencyAbbreviation == null) {
			return res.getDrawable(R.drawable.place_holder);
		}
		String currencyString = currencyAbbreviation.toLowerCase();
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

	public static int getCurrencyDrawableID(Context ctx,
			String currencyAbbreviation) {
		Resources res = ctx.getResources();
		if (currencyAbbreviation == null) {
			return R.drawable.place_holder;
		}
		String currencyString = currencyAbbreviation.toLowerCase();
		try {
			int drawableID = res.getIdentifier(currencyString, "drawable",
					ctx.getPackageName());
			if (drawableID > 0)
				return drawableID;
		} catch (Exception e) {
			// TODO: handle exception
		}

		return R.drawable.place_holder;
	}
	
	public static void playSystemAlarm(Context context) {
		try {
			Log.d(LOG_TAG, "Playing alarm");
			Uri notification = RingtoneManager
					.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			Ringtone r = RingtoneManager.getRingtone(context, notification);
			r.play();
		} catch (Exception e) {
			Log.d(LOG_TAG, "Error playing alarm");
		}
	}
	
	public static void showShortToast(String message, Context context) {
		Toast.makeText(context, message, Toast.LENGTH_SHORT)
				.show();
	}
}
