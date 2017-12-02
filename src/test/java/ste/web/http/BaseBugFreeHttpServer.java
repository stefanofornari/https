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

import ste.web.http.handlers.FileHandler;
import java.util.HashMap;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import static ste.web.http.Constants.CONFIG_HTTPS_AUTH;
import static ste.web.http.Constants.CONFIG_HTTPS_ROOT;
import static ste.web.http.Constants.CONFIG_HTTPS_SESSION_LIFETIME;
import static ste.web.http.Constants.CONFIG_HTTPS_WEBROOT;
import static ste.web.http.Constants.CONFIG_HTTPS_WEB_PORT;
import static ste.web.http.Constants.CONFIG_SSL_PASSWORD;
import static ste.web.http.Constants.CONFIG_HTTPS_SSL_PORT;

/**
 * @author ste
 */
public abstract class BaseBugFreeHttpServer {
    
    protected static final String SSL_PASSWORD = "20150630";
    protected static final String HOME = "src/test";
    protected static final String DOCROOT = "src/test/docroot";
    protected static final String PORT = "8400";
    protected static final String WEBPORT = "8888";

    @Rule
    public final TestRule PRINT_TEST_NAME = new TestWatcher() {
        protected void starting(Description description) {
          System.out.printf("\nTEST %s...\n", description.getMethodName());
        };
    };
    
    @Rule
    public final ProvideSystemProperty TRUST_STORE_PROPERTY
	 = new ProvideSystemProperty("javax.net.ssl.trustStore", "src/test/conf/castore");

    @Rule
    public final ProvideSystemProperty TRUST_STORE_PWD_PROPERTY
	 = new ProvideSystemProperty("javax.net.ssl.trustStorePassword", SSL_PASSWORD);
    
    //@Rule public final ProvideSystemProperty SSL_DEBUG_PROPERTY = new ProvideSystemProperty("javax.net.debug", "ssl");

    protected Configuration configuration = null;
    protected HttpServer server = null;

    @Before
    public void before() throws Exception  {
        createDefaultConfiguration();
        createServer();
    }

    @After
    public void after() throws Exception {
        if (server != null) {
            server.stop();
        }
    }
    
    // ------------------------------------------------------- protected methods
    
    protected void waitServerStartup() throws Exception {
        long start = System.currentTimeMillis();
        int maxTry = 400;
        while ((--maxTry > 0) && !server.isRunning()) {
            Thread.sleep(20);
        }
        
        if (maxTry == 0) {
            throw new InterruptedException(
                "server not ready in " + (System.currentTimeMillis()-start) + " ms"
            );
        }
    }
    
    protected void waitServerShutdown() throws Exception {
        long start = System.currentTimeMillis();
        int maxTry = 400;
        while ((--maxTry > 0) && server.isRunning()) {
            Thread.sleep(20);
        }
        
        if (maxTry == 0) {
            throw new InterruptedException(
                "server not stopped in " + (System.currentTimeMillis()-start) + " ms"
            );
        }
    }
    
    protected void createServer() throws Exception {
        HashMap<String, HttpRequestHandler> handlers = new HashMap<>();
        handlers.put("*", new FileHandler(DOCROOT));
        
        server = new HttpServer(configuration);
        server.setHandlers(handlers);
    }
    
        protected void createDefaultConfiguration() {
        configuration = new PropertiesConfiguration();
        configuration.setProperty(CONFIG_HTTPS_ROOT, HOME);
        configuration.setProperty(CONFIG_HTTPS_SSL_PORT, PORT);
        configuration.setProperty(CONFIG_HTTPS_WEB_PORT, WEBPORT);
        configuration.setProperty(CONFIG_HTTPS_WEBROOT, DOCROOT);
        configuration.setProperty(CONFIG_HTTPS_AUTH, "none");
        configuration.setProperty(CONFIG_SSL_PASSWORD, SSL_PASSWORD);
        configuration.setProperty(CONFIG_HTTPS_SESSION_LIFETIME, String.valueOf(15*60*1000));
    }
}
