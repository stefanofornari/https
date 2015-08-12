/*
 * Copyright (C) 2015 Stefano Fornari.
 * All Rights Reserved.  No use, copying or distribution of this
 * work may be made except in accordance with a valid license
 * agreement from Stefano Fornari.  This notice must be
 * included on all copies, modifications and derivatives of this
 * work.
 *
 * STEFANO FORNARI MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY
 * OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. STEFANO FORNARI SHALL NOT BE LIABLE FOR ANY
 * DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 */
package ste.web.http;

import java.io.IOException;
import java.net.URL;
import java.util.Base64;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Before;
import org.junit.Test;
import static ste.web.http.BugFreeHttpServerBase.HOME;
import static ste.web.http.Constants.CONFIG_HTTPS_AUTH;
import static ste.web.http.Constants.CONFIG_HTTPS_PORT;
import static ste.web.http.Constants.CONFIG_HTTPS_ROOT;
import static ste.web.http.Constants.CONFIG_HTTPS_SESSION_LIFETIME;
import static ste.web.http.Constants.CONFIG_HTTPS_WEBROOT;
import static ste.web.http.Constants.CONFIG_SSL_PASSWORD;
import ste.web.http.handlers.FileHandler;

/**
 *
 * @author ste
 */
public class BugFreeHttpServerClientAuthenticationBasic extends BugFreeHttpServerBase {
    
    private URL index = null, auth = null;
    
    @Before
    @Override
    public void set_up() throws Exception {
        createDefaultConfiguration();
        configuration.setProperty(CONFIG_HTTPS_AUTH, "basic");
        
        UriHttpRequestHandlerMapper handlers = new UriHttpRequestHandlerMapper();
        handlers.register("*", new FileHandler(DOCROOT));
        handlers.register("/auth/*", new BasicAuthHandler());
        
        server = new HttpServer(configuration);
        server.setHandlers(handlers);
        
        index = new URL("https://localhost:" + server.getPort() + "/index.html");
        auth = new URL("https://localhost:" + server.getPort() + "/auth/index.html");
    }
    
    @Test
    public void mssing_client_authentication_when_required_401() throws Exception {
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, null, null);
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            server.start(); waitServerStartup();

            then(
                ((HttpsURLConnection)auth.openConnection()).getResponseCode()
            ).isEqualTo(HttpStatus.SC_UNAUTHORIZED);
        } finally {
            server.stop(); waitServerShutdown();
        }
    }
    
    @Test
    public void mssing_client_authentication_when_not_required_200() throws Exception {
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, null, null);
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            server.start(); waitServerStartup();

            then(
                ((HttpsURLConnection)index.openConnection()).getResponseCode()
            ).isEqualTo(HttpStatus.SC_OK);
        } finally {
            server.stop(); waitServerShutdown();
        }
    }
    
    @Test
    public void get_home_page_with_credentials() throws Exception {
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, null, null);
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            server.start(); waitServerStartup();
            
            HttpsURLConnection conn = (HttpsURLConnection)auth.openConnection();
            conn.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString("hello:world".getBytes()));

            then(
                conn.getResponseCode()
            ).isEqualTo(HttpStatus.SC_OK);
        } finally {
            server.stop(); waitServerShutdown();
        }
    }
    
    // -------------------------------------------------------- BasicAuthHandler
    
    private class BasicAuthHandler implements HttpRequestHandler {

        @Override
        public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
            if (request.getFirstHeader("Authorization") == null) {
                response.setStatusCode(HttpStatus.SC_UNAUTHORIZED);
            }
        }
        
    }

}
