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

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.Socket;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import static ste.web.http.Constants.CONFIG_SSL_PASSWORD;
import ste.xtest.logging.ListLogHandler;
import ste.xtest.reflect.PrivateAccess;

/**
 * 
 * @author ste
 */
public class BugFreeHttpServerLog extends BugFreeHttpServerBase {
    
    private static final String MSG_SOCKET_ACCEPT_FAILURE_SSL =
        "stopping to listen on port " + PORT + " (Socket closed)";
    private static final String MSG_SOCKET_ACCEPT_FAILURE_WEB =
        "stopping to listen on port " + WEBPORT + " (Socket is closed)";
    private static final String MSG_SOCKET_CREATE_CONNECTION_SSL =
        "stopping to create connections on port " + PORT + " (connection error: fake HttpConnectionFactory!!!)";
    private static final String MSG_SOCKET_CREATE_CONNECTION_WEB =
        "stopping to create connections on port " + WEBPORT + " (connection error: fake HttpConnectionFactory!!!)";
    
    private static final Logger LOG = Logger.getLogger(HttpServer.LOG_SERVER);
    
    private BasicHttpConnectionFactory factory = null;
    
    private ListLogHandler h = null;
        
    @Rule
    public final ProvideSystemProperty SSL_PASSWORD
	 = new ProvideSystemProperty(CONFIG_SSL_PASSWORD, "20150630");
    
    @Before
    @Override
    public void set_up() throws Exception {   
        super.set_up();
        //
        // Logger.getLogger() returns the same instance to multiple threads
        // therefore each method must add its own handler; we clean up the 
        // handlers before starting.
        //
        for (Handler h: LOG.getHandlers()) {
            LOG.removeHandler(h);
        }
        
        h = new ListLogHandler();
        LOG.addHandler(h);
        LOG.setLevel(Level.INFO);
    }

    @Test
    public void log_at_info_accept_error_ssl() throws Exception {
        try {
            server.start(); waitServerStartup();

            HttpServer.RequestListenerThread listener = 
                (HttpServer.RequestListenerThread)PrivateAccess.getInstanceValue(server, "listenerThread");

            listener.interrupt();
            
            waitLogRecords(h);
            then(h.getMessages()).contains(MSG_SOCKET_ACCEPT_FAILURE_SSL);
        } finally {
            if (server != null) {
                server.stop(); waitServerShutdown();
            }
        }
    }
    
    @Test
    public void log_at_info_accept_error_web() throws Exception {
        try {
            server.start(); waitServerStartup();

            HttpServer.RequestListenerThread listener = 
                (HttpServer.RequestListenerThread)PrivateAccess.getInstanceValue(server, "webListenerThread");
            
            listener.interrupt();
            
            waitLogRecords(h);
            then(h.getMessages()).contains(MSG_SOCKET_ACCEPT_FAILURE_WEB);
        } finally {
            if (server != null) {
                server.stop(); waitServerShutdown();
            }
        }
    }
    
    @Test
    public void log_at_info_create_connection_error_ssl() throws Exception {
        try {
            makeDirtyTrickToFailConnectionCreation();
            
            server.start(); waitServerStartup();

            Socket s = new Socket("localhost", Integer.parseInt(PORT));
            s.getInputStream();
            s.close();
            
            waitLogRecords(h);
            then(h.getMessages()).contains(MSG_SOCKET_CREATE_CONNECTION_SSL);
        } finally {
            if (server != null) {
                server.stop(); waitServerShutdown();
            }
            revertDirtyTrickToFailConnectionCreation();
        }
    }
    
    @Test
    public void log_at_info_create_connection_error_web() throws Exception {
        try {
            makeDirtyTrickToFailConnectionCreation();
            
            server.start(); waitServerStartup();

            Socket s = new Socket("localhost", Integer.parseInt(WEBPORT));
            s.getInputStream();
            s.close();
            
            waitLogRecords(h);
            then(h.getMessages()).contains(MSG_SOCKET_CREATE_CONNECTION_WEB);
        } finally {
            if (server != null) {
                server.stop(); waitServerShutdown();
            }
            revertDirtyTrickToFailConnectionCreation();
        }
    }
    
    //
    // TODO: log when the server starts
    // TODO: log when the server does not start
    //
    
    // --------------------------------------------------------- private methods
        
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
    
    private void makeDirtyTrickToFailConnectionCreation()
    throws Exception {
        factory = BasicHttpConnectionFactory.INSTANCE;
        
        Field f = BasicHttpConnectionFactory.class.getField("INSTANCE");
        
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(f, f.getModifiers() & ~Modifier.FINAL);
        
        f.set(null, new TestConnectionFactory());
    }
    
    private void revertDirtyTrickToFailConnectionCreation()
    throws Exception {
        Field f = BasicHttpConnectionFactory.class.getField("INSTANCE");
        
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(f, f.getModifiers() & ~Modifier.FINAL);
        
        f.set(null, factory);
    }
    
    // --------------------------------------------------- TestConnectionFactory
    
    class TestConnectionFactory extends BasicHttpConnectionFactory {
        @Override
        public BasicHttpConnection createConnection(Socket socket) throws IOException {
            System.out.println("Fake HttpConnectionFactory!!!");
            throw new IOException("connection error: fake HttpConnectionFactory!!!");
        }
    }
    
}
