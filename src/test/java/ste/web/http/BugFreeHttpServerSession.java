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

import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;
import org.apache.http.util.EntityUtils;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Test;
import ste.web.http.handlers.PrintSessionHandler;

/**
 *
 * @author ste
 */
public class BugFreeHttpServerSession extends BugFreeHttpServerBase {

    @Test
    public void getSessionValuesInTheSameSession() throws Exception {
        
        server.start(); Thread.sleep(25);
        
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet("https://localhost:8000/index.html");
        
        HttpResponse response = httpclient.execute(httpget);
        then(response.getStatusLine().getStatusCode())
            .isEqualTo(HttpStatus.SC_OK);

        List<Cookie> cookies = httpclient.getCookieStore().getCookies();
        then(cookies).isNotEmpty();
        
        String sessionId = null;
        for (Cookie c: cookies) {
            if ("JSESSIONID".equals(c.getName())) {
                sessionId = c.getValue().replace("\"", "");
                break;
            }
        }
        
        //
        // The output of HttpSessionHandler shall be:
        //
        // id: <the session id>
        // counter: <an autoincremented 1-based value>
        //
        then(EntityUtils.toString(response.getEntity()))
            .contains(String.format("{id: %s}", sessionId))
            .contains("{counter: 1}");
        
        //
        // If we hit the same URL with the same client, we should get the same 
        // session id, but the counter shall be incremented
        //
        response = httpclient.execute(httpget);
        then(response.getStatusLine().getStatusCode())
            .isEqualTo(HttpStatus.SC_OK);
        
        String newSessionId = null;
        for (Cookie c: cookies) {
            if ("JSESSIONID".equals(c.getName())) {
                newSessionId = c.getValue().replace("\"", "");
                break;
            }
        }
        then(newSessionId).isEqualTo(sessionId);
        then(EntityUtils.toString(response.getEntity()))
            .contains(String.format("{id: %s}", sessionId))
            .contains("{counter: 2}");
    }
    
    @Test
    public void getNewSession() throws Exception {
        
        server.start(); Thread.sleep(25);
        
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet("https://localhost:8000/index.html");
        
        HttpResponse response = httpclient.execute(httpget);
        then(response.getStatusLine().getStatusCode())
            .isEqualTo(HttpStatus.SC_OK);

        List<Cookie> cookies = httpclient.getCookieStore().getCookies();
        then(cookies).isNotEmpty();
        
        String sessionId = null;
        for (Cookie c: cookies) {
            if ("JSESSIONID".equals(c.getName())) {
                sessionId = c.getValue().replace("\"", "");
                break;
            }
        }
        
        //
        // The output of HttpSessionHandler shall be:
        //
        // id: <the session id>
        // counter: <an autoincremented 1-based value>
        //
        then(EntityUtils.toString(response.getEntity()))
            .contains(String.format("{id: %s}", sessionId))
            .contains("{counter: 1}");
        
        //
        // If we hit the same URL with the a new client, we should get a new
        // session id and the counter shall restart
        //
        httpclient = new DefaultHttpClient(); 
        response = httpclient.execute(httpget);
        then(response.getStatusLine().getStatusCode())
            .isEqualTo(HttpStatus.SC_OK);
        
        cookies = httpclient.getCookieStore().getCookies();
        then(cookies).isNotEmpty();
        
        String newSessionId = null;
        for (Cookie c: cookies) {
            if ("JSESSIONID".equals(c.getName())) {
                newSessionId = c.getValue().replace("\"", "");
                break;
            }
        }
        then(newSessionId).isNotEqualTo(sessionId);
        then(EntityUtils.toString(response.getEntity()))
            .contains(String.format("{id: %s}", newSessionId))
            .contains("{counter: 1}");
    }
    
    // ------------------------------------------------------- protected methods
    
    protected HttpServer createHttpServer() throws Exception {
        HttpServer s = super.createServer(HttpServer.ClientAuthentication.NONE);
        
        UriHttpRequestHandlerMapper handlers = new UriHttpRequestHandlerMapper();
        handlers.register("*", new PrintSessionHandler());
        s.setHandlers(handlers);
        
        return s;
    }

}
