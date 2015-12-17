package com.tbg.currencywidget;

import android.util.Log;

public class Logger {
	private static final String LOG_TAG = "CurrencyTestConverter";
	
	private static String getLogString(String format, Object ... args){
		//Minor optimization, only call String.format if necessary
		if(args.length == 0){
			return format;
		}
		return String.format(format, args);
	}
	
	/*
	 * INFO, WARNING, ERROR log levels print always
	 */
	
	public static void error(String format, Object... args){
		Log.e(LOG_TAG, getLogString(format, args));
	}
	
	public static void warning(String format, Object... args){
		Log.w(LOG_TAG, getLogString(format, args));
	}
	
	public static void warning(Throwable throwable){
		Log.w(LOG_TAG,throwable);
	}
	
	public static void info(String format, Object... args){
		Log.i(LOG_TAG, getLogString(format, args));
	}
	
	/* The DEBUG and VERBOSE log levels are protected by DEBUG flag */
	
	public static void debug(String format, Object... args){
		if(!BuildConfig.DEBUG){
			return;
		}
		Log.d(LOG_TAG, getLogString(format, args));
	}
	
	public static void verbose(String format, Object... args){
		if(!BuildConfig.DEBUG){
			return;
		}
		Log.v(LOG_TAG, getLogString(format, args));
	}
}
