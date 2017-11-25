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
import java.util.HashMap;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HttpRequestHandler;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Test;
import static ste.web.http.Constants.CONFIG_HTTPS_SESSION_LIFETIME;
import ste.web.http.handlers.PrintSessionHandler;

/**
 *
 * @author ste
 */
public class BugFreeHttpServerSession extends BaseBugFreeHttpServer {
    @Test
    public void get_session_values_in_the_same_session() throws Exception {
        createAndStartServer();
        
        URL url = new URL("https://localhost:" + PORT + "/index.html");
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        
        then(conn.getResponseCode()).isEqualTo(HttpStatus.SC_OK);
        

        String sessionId = HttpUtils.extractSessionId(conn.getHeaderField("Set-Cookie"));
        //
        // The output of HttpSessionHandler shall be:
        //
        // id: <the session id>
        // counter: <an autoincremented 1-based value>
        //
        then(IOUtils.toString(conn.getInputStream(), "UTF8"))
            .contains(String.format("{id: %s}", sessionId))
            .contains("{counter: 1}");
        
        conn.disconnect();
        
        //
        // If we hit the same URL with the same client, we should not return 
        // a session id, but the counter shall be incremented
        //
        conn = (HttpURLConnection)url.openConnection();
        conn.setRequestProperty("Cookie", "JSESSIONID=" + sessionId + ";");
        
        then(conn.getResponseCode()).isEqualTo(HttpStatus.SC_OK);
        
        String newSessionId = HttpUtils.extractSessionId(conn.getHeaderField("Set-Cookie"));
        then(newSessionId).isNull();
        then(IOUtils.toString(conn.getInputStream(), "UTF8"))
            .contains(String.format("{id: %s}", sessionId))
            .contains("{counter: 2}");
    }
    
    @Test
    public void get_new_session() throws Exception {
        createAndStartServer();
        
        URL url = new URL("https://localhost:" + PORT + "/index.html");
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        
        then(conn.getResponseCode()).isEqualTo(HttpStatus.SC_OK);

        String sessionId = HttpUtils.extractSessionId(conn.getHeaderField("Set-Cookie"));
        then(sessionId).isNotNull();
        
        //
        // The output of HttpSessionHandler shall be:
        //
        // id: <the session id>
        // counter: <an autoincremented 1-based value>
        //
        then(IOUtils.toString(conn.getInputStream(), "UTF8"))
            .contains(String.format("{id: %s}", sessionId))
            .contains("{counter: 1}");
        
        conn.disconnect();
        
        //
        // If we hit the same URL with the a new client, we should get a new
        // session id and the counter shall restart
        //
        conn = (HttpURLConnection)url.openConnection();
        
        then(conn.getResponseCode()).isEqualTo(HttpStatus.SC_OK);

        String newSessionId = HttpUtils.extractSessionId(conn.getHeaderField("Set-Cookie"));
        then(newSessionId).isNotNull();
        
        then(newSessionId).isNotEqualTo(sessionId);
        then(IOUtils.toString(conn.getInputStream(), "UTF8"))
            .contains(String.format("{id: %s}", newSessionId))
            .contains("{counter: 1}");
    }

    @Test
    public void session_expiration() throws Exception {
        createAndStartServer();
        
        URL url = new URL("https://localhost:" + PORT + "/index.html");
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        
        then(conn.getResponseCode()).isEqualTo(HttpStatus.SC_OK);
        
        String sessionId = HttpUtils.extractSessionId(conn.getHeaderField("Set-Cookie"));
        then(sessionId).isNotNull();
        
        
        //
        // The output of HttpSessionHandler shall be:
        //
        // id: <the session id>
        // counter: <an autoincremented 1-based value>
        //
        then(IOUtils.toString(conn.getInputStream(), "UTF8"))
            .contains(String.format("{id: %s}", sessionId))
            .contains("{counter: 1}");
        
        conn.disconnect();
        
        //
        // If we hit the same URL with the same client after the expiration 
        // time, the session id shall not be reused
        //
        Thread.sleep(1000);
        
        conn = (HttpURLConnection)url.openConnection();
        then(conn.getResponseCode()).isEqualTo(HttpStatus.SC_OK);
        
        String newSessionId = HttpUtils.extractSessionId(conn.getHeaderField("Set-Cookie"));
        then(newSessionId).isNotEqualTo(sessionId);
        then(IOUtils.toString(conn.getInputStream(), "UTF8"))
            .contains(String.format("{id: %s}", newSessionId))
            .contains("{counter: 1}");
    }
    
    // ------------------------------------------------------- protected methods
    
    protected void createAndStartServer() throws Exception {
        createDefaultConfiguration();
        configuration.setProperty(CONFIG_HTTPS_SESSION_LIFETIME, "250");
        createServer();
        
        HashMap<String, HttpRequestHandler> handlers = new HashMap<>();
        handlers.put("*", new PrintSessionHandler());
        server.setHandlers(handlers);
        
        server.start(); waitServerStartup();
    }

}
