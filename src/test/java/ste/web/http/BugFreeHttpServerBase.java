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

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.util.Properties;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * @author ste
 */
public abstract class BugFreeHttpServerBase {

    @Rule
    public final TestRule PRINT_TEST_NAME = new TestWatcher() {
        protected void starting(Description description) {
          System.out.printf("\nTEST %s...\n", description.getMethodName());
        };
    };
    
    @Rule
    public final TemporaryFolder TESTDIR = new TemporaryFolder();
    
    @Rule
    public final ProvideSystemProperty TRUST_STORE
	 = new ProvideSystemProperty("javax.net.ssl.trustStore", "src/test/etc/castore");

    @Rule
    public final ProvideSystemProperty TRUST_STORE_PWD
	 = new ProvideSystemProperty("javax.net.ssl.trustStorePassword", "serverone");

    private static SSLContext clientCertificateContext;
    
    protected HttpServer server = null;

    @BeforeClass
    public static void setUpClass() throws Exception {
        Properties props = System.getProperties();
        props.put("javax.net.ssl.trustStoreType", "jks");
        props.put("javax.net.ssl.trustStore", "src/test/etc/castore");
        props.put("javax.net.ssl.trustStorePassword", "serverone");

        KeyStore ks = KeyStore.getInstance("PKCS12");
        FileInputStream fis = new FileInputStream("src/test/etc/mariorossi.p12");
        ks.load(fis, "serverone".toCharArray());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, "serverone".toCharArray());
        clientCertificateContext = SSLContext.getInstance("TLS");
        clientCertificateContext.init(kmf.getKeyManagers(), null, null);
    }

    @Before
    public void setUp() throws Exception  {
        HttpsURLConnection.setDefaultSSLSocketFactory(clientCertificateContext.getSocketFactory());
        server = createHttpServer();
    }

    @After
    public void tearDown() throws IOException {
        if (server.isRunning()) {
            server.stop();
        }
    }
    
    // ------------------------------------------------------- protected methods
    
    protected HttpServer createHttpServer() throws Exception {
        return createServer(HttpServer.ClientAuthentication.NONE);
    }
    
    protected HttpServer createHttpServerWithClientAuth() throws Exception {
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
