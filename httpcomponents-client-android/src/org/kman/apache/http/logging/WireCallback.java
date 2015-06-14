package org.kman.apache.http.logging;

import java.io.IOException;
import java.io.InputStream;

import original.apache.http.impl.conn.Wire;

/**
 * An interface to route Wire logging through application code.
 * 
 * {@link Wire#setWireCallback(WireCallback)}
 */
public interface WireCallback {

	public boolean isWireLogEnabled();

	public boolean onWireLogData(final String header, final InputStream instream) throws IOException;
}
