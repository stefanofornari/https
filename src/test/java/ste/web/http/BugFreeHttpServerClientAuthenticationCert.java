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
import java.net.URL;
import java.security.KeyStore;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import static ste.web.http.BugFreeHttpServerBase.SSL_PASSWORD;
import static ste.web.http.Constants.CONFIG_HTTPS_AUTH;
import static ste.web.http.Constants.CONFIG_HTTPS_PORT;
import static ste.web.http.Constants.CONFIG_HTTPS_ROOT;
import static ste.web.http.Constants.CONFIG_HTTPS_SESSION_LIFETIME;
import static ste.web.http.Constants.CONFIG_HTTPS_WEBROOT;
import static ste.web.http.Constants.CONFIG_HTTPS_WEB_PORT;
import static ste.web.http.Constants.CONFIG_SSL_PASSWORD;
import ste.web.http.handlers.FileHandler;

/**
 *
 * @author ste
 */
public class BugFreeHttpServerClientAuthenticationCert extends BugFreeHttpServerBase {
    
    private URL url = null;
    
    @Rule
    public final ProvideSystemProperty CA_STORE_PROPERTY
	 = new ProvideSystemProperty("javax.net.ssl.trustStore", "src/test/etc/castore");

    @Rule
    public final ProvideSystemProperty CA_STORE_PWD_PROPERTY
	 = new ProvideSystemProperty("javax.net.ssl.trustStorePassword", SSL_PASSWORD);
    
    @Rule
    public final ProvideSystemProperty CA_STORE__PROPERTY
	 = new ProvideSystemProperty("javax.net.ssl.trustStoreType", "jks");
    
    
    @Before
    @Override
    public void set_up() throws Exception {
        createDefaultConfiguration();
        configuration.setProperty(CONFIG_HTTPS_AUTH, "cert");
        createServer();
        
        url = new URL("https://localhost:" + server.getPort() + "/index.html");
    }
    
    @Test
    public void mssing_client_authentication_when_required() throws Exception {
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, null, null);
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            server.start(); waitServerStartup();

            try {
                ((HttpsURLConnection)url.openConnection()).getResponseCode();
                fail("SSL handshake not failed!");
            } catch (SSLHandshakeException x) {
                //
                // OK
                //
            }
        } finally {
            server.stop(); waitServerShutdown();
        }
    }
    
    @Test
    public void get_home_page() throws Exception {
        //
        // The client key store must contain the private keys of the client
        // identity (mario rossi) obatined importing a signed certificate
        // (through the sign-request/sign/import sign-respons process) and the
        // keys of the CA (I still need to understand why since they are already
        // in the truststore...). 
        // For example:
        //   mario rossi -> signed by localhost
        //   localhost -> self signed and stored in castore too
        //
        KeyStore ks = KeyStore.getInstance("jks");
        FileInputStream fis = new FileInputStream("src/test/etc/client.jks");
        ks.load(fis, SSL_PASSWORD.toCharArray());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, SSL_PASSWORD.toCharArray());
        
        SSLContext clientCertificateContext = SSLContext.getInstance("TLS");
        clientCertificateContext.init(kmf.getKeyManagers(), null, null);
        
        HttpsURLConnection.setDefaultSSLSocketFactory(clientCertificateContext.getSocketFactory());
        
        server.start(); waitServerStartup();

        then(((HttpsURLConnection)url.openConnection()).getResponseCode())
            .isEqualTo(HttpsURLConnection.HTTP_OK);
    }

}
