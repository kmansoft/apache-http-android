/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package original.apache.http.impl.conn;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.kman.apache.http.logging.WireCallback;

import android.util.Log;
import original.apache.http.annotation.Immutable;
import original.apache.http.util.Args;

/**
 * Logs data to the wire LOG.
 * TODO: make package private. Should not be part of the public API.
 *
 * @since 4.0
 */
@Immutable
public class Wire {

    private final static String TAG = "Wire";
    private final String id;
    
    /**
     * kman - added logging callback
     */
    public static void setWireCallback(WireCallback callback) {
    	gWireCallback = callback;	
    }
    
    private static volatile WireCallback gWireCallback;

    /**
     * @since 4.3
     */
    public Wire(final String id) {
        this.id = id;
    }

    private void wire(final String header, final InputStream instream)
      throws IOException {
    	// kman
    	final WireCallback callback = gWireCallback;
    	if (callback != null && callback.onWireLogData(header, instream)) {
    		// Done
    		return;
    	}
    	
        final StringBuilder buffer = new StringBuilder();
        int ch;
        while ((ch = instream.read()) != -1) {
            if (ch == 13) {
                buffer.append("[\\r]");
            } else if (ch == 10) {
                    buffer.append("[\\n]\"");
                    buffer.insert(0, "\"");
                    buffer.insert(0, header);
                    Log.d(TAG, id + " " + buffer.toString());
                    buffer.setLength(0);
            } else if ((ch < 32) || (ch > 127)) {
                buffer.append("[0x");
                buffer.append(Integer.toHexString(ch));
                buffer.append("]");
            } else {
                buffer.append((char) ch);
            }
        }
        if (buffer.length() > 0) {
            buffer.append('\"');
            buffer.insert(0, '\"');
            buffer.insert(0, header);
            Log.d(TAG, id + " " + buffer.toString());
        }
    }


    public boolean enabled() {
    	// kman
    	final WireCallback callback = gWireCallback;
    	if (callback != null) {
    		return callback.isWireLogEnabled();
    	}
        return Log.isLoggable(TAG, Log.DEBUG);
    }

    public void output(final InputStream outstream)
      throws IOException {
        Args.notNull(outstream, "Output");
        wire(">> ", outstream);
    }

    public void input(final InputStream instream)
      throws IOException {
        Args.notNull(instream, "Input");
        wire("<< ", instream);
    }

    public void output(final byte[] b, final int off, final int len)
      throws IOException {
        Args.notNull(b, "Output");
        wire(">> ", new ByteArrayInputStream(b, off, len));
    }

    public void input(final byte[] b, final int off, final int len)
      throws IOException {
        Args.notNull(b, "Input");
        wire("<< ", new ByteArrayInputStream(b, off, len));
    }

    public void output(final byte[] b)
      throws IOException {
        Args.notNull(b, "Output");
        wire(">> ", new ByteArrayInputStream(b));
    }

    public void input(final byte[] b)
      throws IOException {
        Args.notNull(b, "Input");
        wire("<< ", new ByteArrayInputStream(b));
    }

    public void output(final int b)
      throws IOException {
        output(new byte[] {(byte) b});
    }

    public void input(final int b)
      throws IOException {
        input(new byte[] {(byte) b});
    }

    public void output(final String s)
      throws IOException {
        Args.notNull(s, "Output");
        output(s.getBytes());
    }

    public void input(final String s)
      throws IOException {
        Args.notNull(s, "Input");
        input(s.getBytes());
    }
}
