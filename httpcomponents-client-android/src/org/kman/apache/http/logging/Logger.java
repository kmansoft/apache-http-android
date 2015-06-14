package org.kman.apache.http.logging;

import android.util.Log;

/**
 * An class to route non-wire (debug) logging through application code.
 * 
 * Used instead of Android "Log" class and so has identical interface (as far as the two methods
 * that actually are used).
 */
public class Logger {

	public static final int DEBUG = Log.DEBUG;

	public static void setLoggerCallback(LoggerCallback callback) {
		gLoggerCallback = callback;
	}

	private static volatile LoggerCallback gLoggerCallback;

	public static boolean isLoggable(String tag, int level) {
		final LoggerCallback callback = gLoggerCallback;
		if (callback != null) {
			return callback.isLoggable(tag, level);
		}

		return Log.isLoggable(tag, level);
	}

	public static void d(String tag, String msg) {
		final LoggerCallback callback = gLoggerCallback;
		if (callback != null && callback.d(tag, msg)) {
			return;
		}
		Log.d(tag, msg);
	}
}
