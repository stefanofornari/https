/*
 * Copyright (C) 2014 Stefano Fornari.
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

import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import static ste.web.http.Constants.CONFIG_SSL_PASSWORD;

/**
 *
 * @author ste
 */
public class BugFreeHttpServerStartup extends BugFreeHttpServerBase {
    
    @Rule
    public final ProvideSystemProperty SSL_PASSWORD
	 = new ProvideSystemProperty(CONFIG_SSL_PASSWORD, "20150630");


    @Test
    public void getHomePage() throws Exception {
        server.start(); waitServerStartup();

        URL url = new URL("https://localhost:8000/index.html");
        then(((HttpsURLConnection)url.openConnection()).getResponseCode())
            .isEqualTo(HttpsURLConnection.HTTP_OK);
    }

    @Test
    public void getContent() throws Exception {
        server.start(); waitServerStartup();

        URL url = new URL("https://localhost:8000/folder/notexisting.txt");
        then(((HttpsURLConnection)url.openConnection()).getResponseCode())
            .isEqualTo(HttpsURLConnection.HTTP_NOT_FOUND);
        
        url = new URL("https://localhost:8000/folder/readme.txt");
        then(((HttpsURLConnection)url.openConnection()).getResponseCode())
            .isEqualTo(HttpsURLConnection.HTTP_OK);
    }

    //
    // We do not want to send the version of the server in the Server HTTP
    // header
    //
    @Test
    public void noHTTPServerHeader() throws Exception {
        server.start(); waitServerStartup();

        URL url = new URL("https://localhost:8000/diskone/readme.txt");

        then(
            ((HttpsURLConnection)url.openConnection()).getHeaderField("Server")
        ).isNull();
    }

    @Test
    public void mssingClientAuthenticationWhenRequired() throws Exception {
        HttpServer server2 = null;
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, null, null);
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            configuration.setProperty(Constants.CONFIG_HTTPS_AUTH, "cert");
            server2 = new HttpServer(configuration);
            server2.start(); waitServerStartup();

            URL url = new URL("https://localhost:8000/index.html");
            try {
                ((HttpsURLConnection)url.openConnection()).getResponseCode();
                fail("SSL handshake not failed!");
            } catch (SSLHandshakeException x) {
                //
                // OK
                //
            }
        } finally {
            server2.stop();
        }
    }
}
