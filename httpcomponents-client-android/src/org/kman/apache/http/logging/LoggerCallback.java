package org.kman.apache.http.logging;


/**
 * An interface to route non-Wire logging through application code.
 * 
 * {@link Logger#setLoggerCallback(LoggerCallback)}
 */
public interface LoggerCallback {

	public boolean isLoggable(String tag, int level);

	public boolean d(String tag, String msg);

}
