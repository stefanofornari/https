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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpConnectionFactory;
import org.apache.http.impl.DefaultBHttpServerConnection;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import static ste.web.http.Constants.CONFIG_SSL_PASSWORD;
import ste.xtest.logging.ListLogHandler;
import ste.xtest.reflect.PrivateAccess;

/**
 * TODO: cover web case too
 * 
 * @author ste
 */
public class BugFreeHttpServerLog extends BugFreeHttpServerBase {
    
    private static final String MSG_SOCKET_ACCEPT_FAILURE =
        "stopping to listen on port " + PORT + " (Socket closed)";
    private static final String MSG_SOCKET_CREATE_CONNECTION =
        "stopping to create connections (Connection error)";
    
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
    }

    @Test
    public void logAtInfoAcceptError() throws Exception {
        final ListLogHandler h = new ListLogHandler();
        LOG.addHandler(h);
        LOG.setLevel(Level.INFO);

        try {
            server.start();

            HttpServer.RequestListenerThread listener = 
                (HttpServer.RequestListenerThread)PrivateAccess.getInstanceValue(server, "listenerThread");

            ServerSocket s = (ServerSocket)PrivateAccess.getInstanceValue(listener, "serverSocket");
            s.close();
        } finally {
            if (server != null) {
                server.stop(); waitServerShutdown();
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
        
        makeDirtyTrickToFailConnectionCreation();

        try {
            server.start();

            Socket s = new Socket("localhost", Integer.parseInt(PORT));
            s.getInputStream();
            s.close();
        } finally {
            if (server != null) {
                server.stop(); waitServerShutdown();
            }
        }
        
        revertDirtyTrickToFailConnectionCreation();
        
        waitLogRecords(h);
        
        then(h.getMessages()).contains(MSG_SOCKET_ACCEPT_FAILURE);
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
