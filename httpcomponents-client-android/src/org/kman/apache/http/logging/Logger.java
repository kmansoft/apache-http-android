package org.kman.apache.http.logging;

import android.util.Log;

/**
 * An class to route non-wire (debug) logging through application code.
 * 
 * Used instead of Android "Log" class and so has identical interface (as far as the methods that
 * actually are used).
 */
public class Logger {

	public static final int DEBUG = Log.DEBUG;
	public static final int INFO = Log.INFO;
	public static final int WARN = Log.WARN;
	public static final int ERROR = Log.ERROR;

	public static void setLoggerCallback(LoggerCallback callback) {
		gLoggerCallback = callback;
	}

	private static volatile LoggerCallback gLoggerCallback;

	public static boolean isLoggable(String tag, int level) {
		final LoggerCallback callback = gLoggerCallback;
		if (callback != null) {
			return callback.isLoggable(tag, level);
		}

		return android.util.Log.isLoggable(tag, level);
	}

	public static void d(String tag, String msg) {
		final LoggerCallback callback = gLoggerCallback;
		if (callback != null && callback.d(tag, msg)) {
			return;
		}
		Log.d(tag, msg);
	}

	public static void d(String tag, String msg, Exception x) {
		final LoggerCallback callback = gLoggerCallback;
		if (callback != null && callback.d(tag, msg, x)) {
			return;
		}
		Log.d(tag, msg, x);
	}

	public static void i(String tag, String msg) {
		final LoggerCallback callback = gLoggerCallback;
		if (callback != null && callback.i(tag, msg)) {
			return;
		}
		Log.i(tag, msg);
	}

	public static void w(String tag, String msg) {
		final LoggerCallback callback = gLoggerCallback;
		if (callback != null && callback.w(tag, msg)) {
			return;
		}
		Log.w(tag, msg);
	}

	public static void w(String tag, String msg, Exception x) {
		final LoggerCallback callback = gLoggerCallback;
		if (callback != null && callback.w(tag, msg, x)) {
			return;
		}
		Log.w(tag, msg);
	}

	public static void e(String tag, String msg) {
		final LoggerCallback callback = gLoggerCallback;
		if (callback != null && callback.e(tag, msg)) {
			return;
		}
		Log.e(tag, msg);
	}

	public static void e(String tag, String msg, Exception x) {
		final LoggerCallback callback = gLoggerCallback;
		if (callback != null && callback.e(tag, msg, x)) {
			return;
		}
		Log.e(tag, msg, x);
	}

}
