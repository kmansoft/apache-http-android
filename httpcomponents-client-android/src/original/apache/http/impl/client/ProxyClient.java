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

package original.apache.http.impl.client;

import java.io.IOException;
import java.net.Socket;

import original.apache.http.params.HttpParams;

import original.apache.http.ConnectionReuseStrategy;
import original.apache.http.HttpEntity;
import original.apache.http.HttpException;
import original.apache.http.HttpHost;
import original.apache.http.HttpRequest;
import original.apache.http.HttpResponse;
import original.apache.http.HttpVersion;
import original.apache.http.auth.AUTH;
import original.apache.http.auth.AuthSchemeRegistry;
import original.apache.http.auth.AuthScope;
import original.apache.http.auth.AuthStateHC4;
import original.apache.http.auth.Credentials;
import original.apache.http.client.config.AuthSchemes;
import original.apache.http.client.config.RequestConfig;
import original.apache.http.client.params.HttpClientParamConfig;
import original.apache.http.client.protocol.HttpClientContext;
import original.apache.http.client.protocol.RequestClientConnControl;
import original.apache.http.config.ConnectionConfig;
import original.apache.http.conn.HttpConnectionFactory;
import original.apache.http.conn.ManagedHttpClientConnection;
import original.apache.http.conn.routing.HttpRoute;
import original.apache.http.conn.routing.RouteInfo.LayerType;
import original.apache.http.conn.routing.RouteInfo.TunnelType;
import original.apache.http.entity.BufferedHttpEntityHC4;
import original.apache.http.impl.DefaultConnectionReuseStrategyHC4;
import original.apache.http.impl.auth.BasicSchemeFactoryHC4;
import original.apache.http.impl.auth.DigestSchemeFactoryHC4;
import original.apache.http.impl.auth.HttpAuthenticator;
import original.apache.http.impl.auth.NTLMSchemeFactory;
import original.apache.http.impl.conn.ManagedHttpClientConnectionFactory;
import original.apache.http.impl.execchain.TunnelRefusedException;
import original.apache.http.message.BasicHttpRequest;
import original.apache.http.params.BasicHttpParams;
import original.apache.http.params.HttpParamConfig;
import original.apache.http.protocol.BasicHttpContextHC4;
import original.apache.http.protocol.HttpContext;
import original.apache.http.protocol.HttpCoreContext;
import original.apache.http.protocol.HttpProcessor;
import original.apache.http.protocol.HttpRequestExecutor;
import original.apache.http.protocol.ImmutableHttpProcessor;
import original.apache.http.protocol.RequestTargetHostHC4;
import original.apache.http.protocol.RequestUserAgentHC4;
import original.apache.http.util.Args;
import original.apache.http.util.EntityUtilsHC4;

/**
 * ProxyClient can be used to establish a tunnel via an HTTP proxy.
 */
@SuppressWarnings("deprecation")
public class ProxyClient {

    private final HttpConnectionFactory<HttpRoute, ManagedHttpClientConnection> connFactory;
    private final ConnectionConfig connectionConfig;
    private final RequestConfig requestConfig;
    private final HttpProcessor httpProcessor;
    private final HttpRequestExecutor requestExec;
    private final ProxyAuthenticationStrategy proxyAuthStrategy;
    private final HttpAuthenticator authenticator;
    private final AuthStateHC4 proxyAuthState;
    private final AuthSchemeRegistry authSchemeRegistry;
    private final ConnectionReuseStrategy reuseStrategy;

    /**
     * @since 4.3
     */
    public ProxyClient(
            final HttpConnectionFactory<HttpRoute, ManagedHttpClientConnection> connFactory,
            final ConnectionConfig connectionConfig,
            final RequestConfig requestConfig) {
        super();
        this.connFactory = connFactory != null ? connFactory : ManagedHttpClientConnectionFactory.INSTANCE;
        this.connectionConfig = connectionConfig != null ? connectionConfig : ConnectionConfig.DEFAULT;
        this.requestConfig = requestConfig != null ? requestConfig : RequestConfig.DEFAULT;
        this.httpProcessor = new ImmutableHttpProcessor(
                new RequestTargetHostHC4(), new RequestClientConnControl(), new RequestUserAgentHC4());
        this.requestExec = new HttpRequestExecutor();
        this.proxyAuthStrategy = new ProxyAuthenticationStrategy();
        this.authenticator = new HttpAuthenticator();
        this.proxyAuthState = new AuthStateHC4();
        this.authSchemeRegistry = new AuthSchemeRegistry();
        this.authSchemeRegistry.register(AuthSchemes.BASIC, new BasicSchemeFactoryHC4());
        this.authSchemeRegistry.register(AuthSchemes.DIGEST, new DigestSchemeFactoryHC4());
        this.authSchemeRegistry.register(AuthSchemes.NTLM, new NTLMSchemeFactory());
        this.reuseStrategy = new DefaultConnectionReuseStrategyHC4();
    }

    /**
     * @deprecated (4.3) use {@link ProxyClient#ProxyClient(HttpConnectionFactory, ConnectionConfig, RequestConfig)}
     */
    @Deprecated
    public ProxyClient(final HttpParams params) {
        this(null,
                HttpParamConfig.getConnectionConfig(params),
                HttpClientParamConfig.getRequestConfig(params));
    }

    /**
     * @since 4.3
     */
    public ProxyClient(final RequestConfig requestConfig) {
        this(null, null, requestConfig);
    }

    public ProxyClient() {
        this(null, null, null);
    }

    /**
     * @deprecated (4.3) do not use.
     */
    @Deprecated
    public HttpParams getParams() {
        return new BasicHttpParams();
    }

    /**
     * @deprecated (4.3) do not use.
     */
    @Deprecated
    public AuthSchemeRegistry getAuthSchemeRegistry() {
        return this.authSchemeRegistry;
    }

    public Socket tunnel(
            final HttpHost proxy,
            final HttpHost target,
            final Credentials credentials) throws IOException, HttpException {
        Args.notNull(proxy, "Proxy host");
        Args.notNull(target, "Target host");
        Args.notNull(credentials, "Credentials");
        HttpHost host = target;
        if (host.getPort() <= 0) {
            host = new HttpHost(host.getHostName(), 80, host.getSchemeName());
        }
        final HttpRoute route = new HttpRoute(
                host,
                this.requestConfig.getLocalAddress(),
                proxy, false, TunnelType.TUNNELLED, LayerType.PLAIN);

        final ManagedHttpClientConnection conn = this.connFactory.create(
                route, this.connectionConfig);
        final HttpContext context = new BasicHttpContextHC4();
        HttpResponse response;

        final HttpRequest connect = new BasicHttpRequest(
                "CONNECT", host.toHostString(), HttpVersion.HTTP_1_1);

        final BasicCredentialsProviderHC4 credsProvider = new BasicCredentialsProviderHC4();
        credsProvider.setCredentials(new AuthScope(proxy.getHostName(), proxy.getPort()), credentials);

        // Populate the execution context
        context.setAttribute(HttpCoreContext.HTTP_TARGET_HOST, target);
        context.setAttribute(HttpCoreContext.HTTP_CONNECTION, conn);
        context.setAttribute(HttpCoreContext.HTTP_REQUEST, connect);
        context.setAttribute(HttpClientContext.HTTP_ROUTE, route);
        context.setAttribute(HttpClientContext.PROXY_AUTH_STATE, this.proxyAuthState);
        context.setAttribute(HttpClientContext.CREDS_PROVIDER, credsProvider);
        context.setAttribute(HttpClientContext.AUTHSCHEME_REGISTRY, this.authSchemeRegistry);
        context.setAttribute(HttpClientContext.REQUEST_CONFIG, this.requestConfig);

        this.requestExec.preProcess(connect, this.httpProcessor, context);

        for (;;) {
            if (!conn.isOpen()) {
                final Socket socket = new Socket(proxy.getHostName(), proxy.getPort());
                conn.bind(socket);
            }

            this.authenticator.generateAuthResponse(connect, this.proxyAuthState, context);

            response = this.requestExec.execute(connect, conn, context);

            final int status = response.getStatusLine().getStatusCode();
            if (status < 200) {
                throw new HttpException("Unexpected response to CONNECT request: " +
                        response.getStatusLine());
            }
            if (this.authenticator.isAuthenticationRequested(proxy, response,
                    this.proxyAuthStrategy, this.proxyAuthState, context)) {
                if (this.authenticator.handleAuthChallenge(proxy, response,
                        this.proxyAuthStrategy, this.proxyAuthState, context)) {
                    // Retry request
                    if (this.reuseStrategy.keepAlive(response, context)) {
                        // Consume response content
                        final HttpEntity entity = response.getEntity();
                        EntityUtilsHC4.consume(entity);
                    } else {
                        conn.close();
                    }
                    // discard previous auth header
                    connect.removeHeaders(AUTH.PROXY_AUTH_RESP);
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        final int status = response.getStatusLine().getStatusCode();

        if (status > 299) {

            // Buffer response content
            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                response.setEntity(new BufferedHttpEntityHC4(entity));
            }

            conn.close();
            throw new TunnelRefusedException("CONNECT refused by proxy: " +
                    response.getStatusLine(), response);
        }
        return conn.getSocket();
    }

}
