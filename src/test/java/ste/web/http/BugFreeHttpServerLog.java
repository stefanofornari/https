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
import org.junit.After;
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
public class BugFreeHttpServerLog extends AbstractBugFreeHttpServer {
    
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
        LOG.setLevel(Level.INFO);
    }
    
    @After
    public void after() throws Exception {
        if (server != null) {
            server.stop(); waitServerShutdown();
        }
    }

    @Test
    public void log_at_info_accept_error_ssl() throws Exception {
        ListLogHandler h = createLogHandler();
    
        server.start(); waitServerStartup();

        HttpServer.RequestListenerThread listener = 
            (HttpServer.RequestListenerThread)PrivateAccess.getInstanceValue(server, "listenerThread");

        listener.interrupt();

        waitLogRecords(h, 3);
        then(h.getMessages()).contains(MSG_SOCKET_ACCEPT_FAILURE_SSL);
    }
    
    @Test
    public void log_at_info_accept_error_web() throws Exception {
        ListLogHandler h = createLogHandler();
        
        server.start(); waitServerStartup();

        HttpServer.RequestListenerThread listener = 
            (HttpServer.RequestListenerThread)PrivateAccess.getInstanceValue(server, "webListenerThread");

        listener.interrupt();

        waitLogRecords(h, 3);
        then(h.getMessages()).contains(MSG_SOCKET_ACCEPT_FAILURE_WEB);
    }
    
    @Test
    public void log_at_info_create_connection_error_ssl() throws Exception {
        ListLogHandler h = createLogHandler();
    
        makeDirtyTrickToFailConnectionCreation();

        server.start(); waitServerStartup();

        Socket s = new Socket("localhost", Integer.parseInt(PORT));
        s.getInputStream();
        s.close();

        waitLogRecords(h, 3);
        then(h.getMessages()).contains(MSG_SOCKET_CREATE_CONNECTION_SSL);
        
        revertDirtyTrickToFailConnectionCreation();
    }
    
    @Test
    public void log_at_info_create_connection_error_web() throws Exception {
        ListLogHandler h = createLogHandler();
        
        makeDirtyTrickToFailConnectionCreation();

        server.start(); waitServerStartup();

        Socket s = new Socket("localhost", Integer.parseInt(WEBPORT));
        s.getInputStream();
        s.close();

        waitLogRecords(h, 3);
        then(h.getMessages()).contains(MSG_SOCKET_CREATE_CONNECTION_WEB);
        
        revertDirtyTrickToFailConnectionCreation();
    }
    
    @Test
    public void log_at_info_listeners_startup() throws Exception {
        ListLogHandler h = createLogHandler();
        
        server.start(); waitServerStartup();

        waitLogRecords(h, 2);
        then(h.getMessages()).contains(
            "starting ssl listener on port " + PORT,
            "starting web listener on port " + WEBPORT
        );
    }

    
    // --------------------------------------------------------- private methods
        
    private void waitLogRecords(final ListLogHandler h, final int howMany)
    throws InterruptedException {
        //
        // When running with multiple tests in parallel, it may take a while...
        // let's wait up to 2 seconds
        //
        int i = 50;
        while ((--i>0) && (h.size() < howMany)) {
            Thread.sleep(20);
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
    
    
    private ListLogHandler createLogHandler() {
        ListLogHandler h = new ListLogHandler();
        LOG.addHandler(h);
        
        return h;
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
