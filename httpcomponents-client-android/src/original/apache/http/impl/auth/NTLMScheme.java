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
package original.apache.http.impl.auth;

import java.util.Locale;

import org.kman.apache.http.logging.Logger;

import original.apache.http.Header;
import original.apache.http.HttpRequest;
import original.apache.http.annotation.NotThreadSafe;
import original.apache.http.auth.AUTH;
import original.apache.http.auth.AuthenticationException;
import original.apache.http.auth.Credentials;
import original.apache.http.auth.InvalidCredentialsException;
import original.apache.http.auth.MalformedChallengeException;
import original.apache.http.auth.NTCredentials;
import original.apache.http.message.BufferedHeader;
import original.apache.http.util.Args;
import original.apache.http.util.CharArrayBuffer;

/**
 * NTLM is a proprietary authentication scheme developed by Microsoft
 * and optimized for Windows platforms.
 *
 * @since 4.0
 */
@NotThreadSafe
public class NTLMScheme extends AuthSchemeBase {

    private static final String TAG = "NTLMScheme";
    private static /* final */ boolean LOG_AUTH = false;

    enum State {
        UNINITIATED,
        CHALLENGE_RECEIVED,
        MSG_TYPE1_GENERATED,
        MSG_TYPE2_RECEVIED,
        MSG_TYPE3_GENERATED,
        FAILED,
    }

    private final NTLMEngine engine;

    private State state;
    private String challenge;

    public NTLMScheme(final NTLMEngine engine) {
        super();
        Args.notNull(engine, "NTLM engine");
        this.engine = engine;
        this.state = State.UNINITIATED;
        this.challenge = null;
    }

    /**
     * @since 4.3
     */
    public NTLMScheme() {
        this(new NTLMEngineImpl());
    }

    public String getSchemeName() {
        return "ntlm";
    }

    public String getParameter(final String name) {
        // String parameters not supported
        return null;
    }

    public String getRealm() {
        // NTLM does not support the concept of an authentication realm
        return null;
    }

    public boolean isConnectionBased() {
        return true;
    }

    private static final int MAX_CHALLENGE_COUNT = 10;
    private static final int MAX_AUTHENTICATE_COUNT = 10;

    private int mParseChallengeCount;
    private int mAuthenticateCount;

    @Override
    protected void parseChallenge(
            final CharArrayBuffer buffer,
            final int beginIndex, final int endIndex) throws MalformedChallengeException {
        this.challenge = buffer.substringTrimmed(beginIndex, endIndex);

        // DEBUG
        Logger.i(TAG, String.format(Locale.US, "parseChallenge: challenge = \"%s\", this.state = \"%s\"",
                this.challenge, this.state));

        if (this.challenge.length() == 0) {
            if (this.state == State.UNINITIATED) {
                this.state = State.CHALLENGE_RECEIVED;
            } else {
                this.state = State.FAILED;
            }
        } else if (mParseChallengeCount++ < MAX_CHALLENGE_COUNT) {
            // kman: misconfigured load balancers???
            Logger.i(TAG, String.format(Locale.US, "parseChallenge: retrying State.MSG_TYPE2_RECEVIED"));
            this.state = State.MSG_TYPE2_RECEVIED;
        } else {
            if (this.state.compareTo(State.MSG_TYPE1_GENERATED) < 0) {
                this.state = State.FAILED;
                throw new MalformedChallengeException("Out of sequence NTLM response message");
            } else if (this.state == State.MSG_TYPE1_GENERATED) {
                this.state = State.MSG_TYPE2_RECEVIED;
            }
        }
    }

    public Header authenticate(
            final Credentials credentials,
            final HttpRequest request) throws AuthenticationException {
        NTCredentials ntcredentials = null;
        try {
            ntcredentials = (NTCredentials) credentials;
        } catch (final ClassCastException e) {
            throw new InvalidCredentialsException(
             "Credentials cannot be used for NTLM authentication: "
              + credentials.getClass().getName());
        }

        if (LOG_AUTH) {
            // kman: logging
            Logger.i(TAG, String.format(Locale.US, "authenticate: challenge = \"%s\", old state = \"%s\"", this.challenge,
                this.state));
        }

        if (this.state == State.FAILED && mAuthenticateCount++ < MAX_AUTHENTICATE_COUNT) {
            // kman: misconfigured load balancers???
            Logger.i(TAG, String.format(Locale.US, "authenticate: retrying FAILED -> CHALLENGE_RECEIVED"));
            this.state = State.CHALLENGE_RECEIVED;
        }

        String response = null;
        if (this.state == State.FAILED) {
            throw new AuthenticationException("NTLM authentication failed");
        } else if (this.state == State.CHALLENGE_RECEIVED) {

            if (LOG_AUTH) {
                // kman: logging
                Logger.i(TAG, String.format(Locale.US, "authenticate: generate type 1, domain = %s, workstatation = %s",
                        ntcredentials.getDomain(), ntcredentials.getWorkstation()));
            }

            response = this.engine.generateType1Msg(
                    ntcredentials.getDomain(),
                    ntcredentials.getWorkstation());
            this.state = State.MSG_TYPE1_GENERATED;
        } else if (this.state == State.MSG_TYPE2_RECEVIED) {
            if (LOG_AUTH) {
                // kman: logging
                Logger.i(TAG, String.format(Locale.US,
                        "authenticate: generate type 3, username = %s, domain = %s, workstatation = %s",
                        ntcredentials.getUserName(), ntcredentials.getDomain(), ntcredentials.getWorkstation()));
            }

            response = this.engine.generateType3Msg(
                    ntcredentials.getUserName(),
                    ntcredentials.getPassword(),
                    ntcredentials.getDomain(),
                    ntcredentials.getWorkstation(),
                    this.challenge);
            this.state = State.MSG_TYPE3_GENERATED;
        } else {
            throw new AuthenticationException("Unexpected state: " + this.state);
        }
        final CharArrayBuffer buffer = new CharArrayBuffer(32);
        if (isProxy()) {
            buffer.append(AUTH.PROXY_AUTH_RESP);
        } else {
            buffer.append(AUTH.WWW_AUTH_RESP);
        }
        buffer.append(": NTLM ");
        buffer.append(response);

        if (LOG_AUTH) {
            // kman: logging
            Logger.i(TAG, String.format(Locale.US,
                "authenticate: response = \"%s\", new state = \"%s\"", response, this.state));
        }

        return new BufferedHeader(buffer);
    }

    public boolean isComplete() {
        return this.state == State.MSG_TYPE3_GENERATED || this.state == State.FAILED;
    }

}
