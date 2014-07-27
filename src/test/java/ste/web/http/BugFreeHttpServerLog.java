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
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpConnectionFactory;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnectionFactory;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import ste.web.http.HttpServer.ClientAuthentication;
import ste.web.http.handlers.FileHandler;
import ste.xtest.logging.ListLogHandler;
import ste.xtest.reflect.PrivateAccess;

/**
 *
 * @author ste
 */
public class BugFreeHttpServerLog {
    
    private static final String MSG_SOCKET_ACCEPT_FAILURE =
        "stopping to listen on port 8000 (Socket closed)";
    private static final String MSG_SOCKET_CREATE_CONNECTION =
        "stopping to create connections (Connection error)";
    
    private static final Logger LOG = Logger.getLogger(HttpServer.LOG_SERVER);
        
    @Rule
    public final ProvideSystemProperty SSL_PASSWORD
	 = new ProvideSystemProperty("ste.http.ssl.password", "20150630");
    
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
    public void logAtInfoAcceptError() throws Exception {
        final ListLogHandler h = new ListLogHandler();
        LOG.addHandler(h);
        LOG.setLevel(Level.INFO);

        HttpServer server = null;
        try {
            server = createServer();
            server.start();

            HttpServer.RequestListenerThread listener = 
                (HttpServer.RequestListenerThread)PrivateAccess.getInstanceValue(server, "requestListenerThread");

            ServerSocket s = (ServerSocket)PrivateAccess.getInstanceValue(listener, "serverSocket");
            s.close();
        } finally {
            if (server != null) {
                server.stop();
            }
        }
        
        waitLogRecords(h);
        
        then(h.getMessages()).contains(MSG_SOCKET_ACCEPT_FAILURE);
    }
    
    @Test
    public void logAtInfoCreateConnectionError() throws Exception {
        final ListLogHandler h = new ListLogHandler();
        LOG.addHandler(h);
        LOG.setLevel(Level.INFO);

        HttpServer server = null;
        try {
            server = createServer();
            server.start();

            HttpServer.RequestListenerThread listener = 
                (HttpServer.RequestListenerThread)PrivateAccess.getInstanceValue(server, "requestListenerThread");
            PrivateAccess.setInstanceValue(listener, "connectionFactory", new TestConnectionFactory());

            Socket s = new Socket("localhost", 8000);
            s.getInputStream();
            s.close();
        } finally {
            if (server != null) {
                server.stop();
            }
        }
        waitLogRecords(h);
        
        then(h.getMessages()).contains(MSG_SOCKET_ACCEPT_FAILURE);
    }
    
    // --------------------------------------------------------- private methods
    
    private HttpServer createServer() throws Exception {
        File root = new File("src/test");
        UriHttpRequestHandlerMapper handlers = new UriHttpRequestHandlerMapper();
        handlers.register("*", new FileHandler(root.getPath()));

        HttpServer server = new HttpServer(root.getAbsolutePath(), ClientAuthentication.NONE, 8000, handlers);

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
    
    // --------------------------------------------------- TestConnectionFactory
    
    class TestConnectionFactory implements HttpConnectionFactory<DefaultBHttpServerConnection> {
        @Override
        public DefaultBHttpServerConnection createConnection(Socket socket) throws IOException {
            System.out.println("Fake HttpConnectionFactory!!!");
            throw new IOException("Connection error");
        }
    }
    
}
