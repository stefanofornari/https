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
import org.apache.http.protocol.UriHttpRequestHandlerMapper;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Test;

/**
 * TODO: change castore and keystore's passwords
 *
 * @author ste
 */
public class BugFreeHttpServerStartup extends BugFreeHttpServerBase {

    @Test
    public void homeCanNotBeNullInConstructor() throws Exception {
        try {
            new HttpServer(null, HttpServer.ClientAuthentication.NONE, 8000, null);
            fail("missing not illegal parameter check");
        } catch (IllegalArgumentException x) {
            then(x.getMessage()).contains("home can not be null");
        }
    }
    
    @Test
    public void homeMustExistConstructor() throws Exception {
        final String NOTEXISTING = "/notexisting";
        final String EXISTING = "src/test/docroot/index.html";
        try {
            new HttpServer(NOTEXISTING, HttpServer.ClientAuthentication.NONE, 8000, null);
            fail("missing not illegal parameter check");
        } catch (IllegalArgumentException x) {
            then(x.getMessage()).contains("must exist").contains(NOTEXISTING);
        }
        
        try {
            new HttpServer(EXISTING, HttpServer.ClientAuthentication.NONE, 8000, null);
            fail("missing not illegal parameter check");
        } catch (IllegalArgumentException x) {
            then(x.getMessage()).contains("must be a directory").contains(EXISTING);
        }
    }
    
    @Test
    public void portCanNotBeNegativeOrZeroInConstructor() throws Exception {
        try {
            new HttpServer(".", HttpServer.ClientAuthentication.NONE, 0, null);
            fail("missing illegal parameter check");
        } catch (IllegalArgumentException x) {
            then(x.getMessage()).contains("port can not be <= 0");
        }
        
        try {
            new HttpServer(".", HttpServer.ClientAuthentication.NONE, -2, null);
            fail("missing illegal parameter check");
        } catch (IllegalArgumentException x) {
            then(x.getMessage()).contains("port can not be <= 0");
        }
        
    }

    /**
     * If not specified it should start listening on a default porto (i.e. 8080).
     * Otherwise we can specify the HTTP port to use.
     *
     */
    @Test
    public void getPort() throws Throwable {
        then(server.getPort()).isEqualTo(8000);
    }

    @Test
    public void getHomePage() throws Exception {
        server.start(); Thread.sleep(25);

        URL url = new URL("https://localhost:8000/index.html");
        then(((HttpsURLConnection)url.openConnection()).getResponseCode())
            .isEqualTo(HttpsURLConnection.HTTP_OK);
    }

    @Test
    public void getContent() throws Exception {
        server.start(); Thread.sleep(25);

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
        server.start(); Thread.sleep(25);

        URL url = new URL("https://localhost:8000/diskone/readme.txt");

        then(
            ((HttpsURLConnection)url.openConnection()).getHeaderField("Server")
        ).isNull();
    }

    @Test
    public void mssingClientAuthenticationWhenRequired() throws Exception {
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, null, null);
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        server = createHttpServerWithClientAuth();
        server.start(); Thread.sleep(25);

        URL url = new URL("https://localhost:8000/index.html");
        try {
            ((HttpsURLConnection)url.openConnection()).getResponseCode();
            fail("SSL handshake not failed!");
        } catch (SSLHandshakeException x) {
            //
            // OK
            //
        }
        
    }
    
    // ------------------------------------------------------- protected methods
    
    private HttpServer createHttpServer() throws Exception {
        return createServer(HttpServer.ClientAuthentication.NONE);
    }
    
    private HttpServer createHttpServerWithClientAuth() throws Exception {
        return createServer(HttpServer.ClientAuthentication.CERTIFICATE);
    }
    
    // --------------------------------------------------------- private methods
    
    HttpServer createServer(HttpServer.ClientAuthentication auth) throws Exception {
        final String HOME = "src/test";
        final String DOCROOT = "src/test/docroot";
        
        UriHttpRequestHandlerMapper handlers = new UriHttpRequestHandlerMapper();
        handlers.register("*", new FileHandler(DOCROOT));
        
        HttpServer server = new HttpServer(
            HOME,
            auth, 
            8000, 
            handlers
        );
        
        return server;
    }

}
