/*
 * Copyright (C) 2013 Stefano Fornari.
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
import java.net.URL;
import java.security.KeyStore;
import java.util.Properties;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.After;
import org.junit.Test;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * TODO: change castore and keystore's passwords
 *
 * @author ste
 */
public class BugFreeHttpServer {

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
    private HttpServer server = null;

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
        //File serverone = TESTDIR.newFolder("serverone");
        //FileUtils.copyDirectory(new File("src/test/serverone-1"), serverone);
        HttpsURLConnection.setDefaultSSLSocketFactory(clientCertificateContext.getSocketFactory());
        server = createHttpServer();
    }

    @After
    public void tearDown() throws IOException {
        if (server.isRunning()) {
            server.stop();
        }
    }

    @Test
    public void constructor() throws Exception {
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

    /*
    @Test
    public void noClientAuthentication() throws Exception {
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, null, null);
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        Configuration c = ConfigurationOne.getInstance();
        c.setProperty(CONFIG_SERVERONE_AUTH, "none");

        ServerOne.bootstrap();

        Thread.sleep(250);

        URL url = new URL("https://localhost:8443/diskone/readme.txt");
        then(((HttpsURLConnection)url.openConnection()).getResponseCode())
            .isEqualTo(HttpsURLConnection.HTTP_OK);
    }
    */
    
    // --------------------------------------------------------- private methods
    
    private HttpServer createHttpServer() throws Exception {
        return createServer(HttpServer.ClientAuthentication.NONE);
    }
    
    private HttpServer createHttpServerWithClientAuth() throws Exception {
        return createServer(HttpServer.ClientAuthentication.CERTIFICATE);
    }
    
    private HttpServer createServer(HttpServer.ClientAuthentication auth) throws Exception {
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
