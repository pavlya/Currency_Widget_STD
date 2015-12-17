package com.tbg.currencywidget.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import com.tbg.currencywidget.Logger;
import com.tbg.currencywidget.Utils;

public class CurrencyWidgetBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		PowerManager pm = (PowerManager) context
				.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(
				PowerManager.PARTIAL_WAKE_LOCK, "YOUR TAG");
		// Acquire the lock
		wl.acquire();
		Logger.debug("BroadcastReceiver run");
		Utils.playSystemAlarm(context);
		// Release the lock
		wl.release();
	}

}
