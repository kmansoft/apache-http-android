package org.kman.apache.http.logging;

import java.io.IOException;
import java.io.InputStream;

public interface WireCallback {

	public boolean isWireLogEnabled();
	public boolean onWireLogData(final String header, final InputStream instream) throws IOException;
}
