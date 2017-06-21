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

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static ste.web.http.HttpServer.LOG_ACCESS;
import ste.xtest.logging.ListLogHandler;

/**
 * This bug free code is meant to be high-level end-to-end specification. For
 * finer logging specifications see the underlined implementations:
 * <ul>
 *   <li>BugFreeHttpSessionService
 * </ul>
 * 
 * @author ste
 */
public class BugFreeHttpServerAccessLog extends BaseBugFreeHttpServer {
        
    private static final Logger LOG = Logger.getLogger(LOG_ACCESS);
    
    @BeforeClass
    public static void set_up_class() throws Exception {
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
    public void log_access_for_ok_requests() throws Exception {
        final ListLogHandler h = new ListLogHandler();
        LOG.addHandler(h);
        LOG.setLevel(Level.INFO);
        
        server.start(); waitServerStartup();
        
        // format: <remote address> <user> <session id> "<request> <protocol version>" <status>
        
        URL url = new URL("https://localhost:" + PORT + "/index.html");
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        String sessionId = HttpUtils.extractSessionId(conn.getHeaderField("Set-Cookie"));
        
        then(h.getMessages()).contains("127.0.0.1 - " + sessionId + " \"GET /index.html HTTP/1.1\" 200");
        
        url = new URL("http://localhost:" + WEBPORT + "/index.html");
        conn = (HttpURLConnection)url.openConnection();
        sessionId = HttpUtils.extractSessionId(conn.getHeaderField("Set-Cookie"));
        then(h.getMessages()).contains("127.0.0.1 - " + sessionId + " \"GET /index.html HTTP/1.1\" 200");
        
        url = new URL("http://localhost:" + WEBPORT + "/index2.html");
        conn = (HttpURLConnection)url.openConnection();
        sessionId = HttpUtils.extractSessionId(conn.getHeaderField("Set-Cookie"));
        then(h.getMessages()).contains("127.0.0.1 - " + sessionId + " \"GET /index2.html HTTP/1.1\" 404");
    }
    
    // --------------------------------------------------------- private methods
    
}
