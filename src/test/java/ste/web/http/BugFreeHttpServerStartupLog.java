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

import java.io.File;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import static ste.web.http.BugFreeHttpServerBase.SSL_PASSWORD;
import static ste.web.http.Constants.CONFIG_HTTPS_AUTH;
import static ste.web.http.Constants.CONFIG_HTTPS_PORT;
import static ste.web.http.Constants.CONFIG_HTTPS_ROOT;
import static ste.web.http.Constants.CONFIG_HTTPS_WEB_PORT;
import static ste.web.http.Constants.CONFIG_SSL_PASSWORD;
import ste.web.http.handlers.FileHandler;
import ste.xtest.logging.ListLogHandler;

/**
 *
 * @author ste
 * 
 * TODO: do the same with web
 */
public class BugFreeHttpServerStartupLog {
    
    private static final String MSG_PORT_BINDING_FAILURE =
        "unable to start the ssl server becasue it was not possible to bind port 8000 (Address already in use)";
    
    private static final Logger LOG = Logger.getLogger(HttpServer.LOG_SERVER);
        
    @Rule
    public final ProvideSystemProperty SSL_PASSWORD_PROPERTY
	 = new ProvideSystemProperty(CONFIG_SSL_PASSWORD, SSL_PASSWORD);
    
    @Before
    public void setUp() throws Exception {   
        //
        // Logger.getLogger() returns the same instance to multiple threads
        // therefore each method must add its own handler; we clean up the 
        // handlers before starting.
        //
        for (Handler h: LOG.getHandlers()) {
            LOG.removeHandler(h);
        }
    }

    @Test
    public void logAtInfoAddressAlreadyBound() throws Exception {
        final ListLogHandler h = new ListLogHandler();
        LOG.addHandler(h);
        LOG.setLevel(Level.INFO);

        HttpServer server1 = createServer(), server2 = createServer();
        
        try {
            server1.start();
            server2.start();
            waitLogRecords(h);
        } finally {
            server1.stop(); 
            server2.stop();
            Thread.sleep(100);
        }
        
        then(server2.isRunning()).isFalse();
        then(h.getMessages()).contains(MSG_PORT_BINDING_FAILURE);
    }
    
    // --------------------------------------------------------- private methods
    
    private HttpServer createServer() throws Exception {
        File root = new File("src/test");
        
        PropertiesConfiguration configuration= new PropertiesConfiguration();
        configuration.setProperty(CONFIG_HTTPS_ROOT, root.getAbsolutePath());
        configuration.setProperty(CONFIG_HTTPS_PORT, "8000");
        configuration.setProperty(CONFIG_HTTPS_WEB_PORT, "7000");
        configuration.setProperty(CONFIG_HTTPS_AUTH, "none");
        configuration.setProperty(CONFIG_SSL_PASSWORD, SSL_PASSWORD);
        
        HttpServer s = new HttpServer(configuration);
        UriHttpRequestHandlerMapper handlers = new UriHttpRequestHandlerMapper();
        handlers.register("*", new FileHandler(root.getPath()));

        HttpServer server = new HttpServer(configuration);
        server.setHandlers(handlers);

        return server;
    }
    
    private void waitLogRecords(final ListLogHandler h) throws InterruptedException {
        //
        // When running with multiple tests in parallel, it may take a while...
        // let's wait up to 2 seconds
        //
        int i = 0;
        while ((++i<50) && (h.size() == 0)) {
            Thread.sleep(100);
        }
    }
    
}
