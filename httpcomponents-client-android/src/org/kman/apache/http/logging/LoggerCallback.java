package org.kman.apache.http.logging;

/**
 * An interface to route non-Wire logging through application code.
 * 
 * {@link Logger.er#setLogger.erCallback(Logger.erCallback)}
 */
public interface LoggerCallback {

	public boolean isLoggable(String tag, int level);

	public boolean d(String tag, String msg);

	public boolean d(String tag, String msg, Exception x);

	public boolean i(String tag, String msg);

	public boolean w(String tag, String msg);

	public boolean w(String tag, String msg, Exception x);

	public boolean e(String tag, String msg);

	public boolean e(String tag, String msg, Exception x);
}
