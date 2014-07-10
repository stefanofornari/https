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
import java.net.HttpCookie;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpServerConnection;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestHandlerMapper;
import org.apache.http.protocol.HttpService;

/**
 * 
 * TODO: HttpSessionServiceBuilder
 * 
 */
public class HttpSessionService extends HttpService {
    
    private SessionCache sessions;

    public HttpSessionService(HttpProcessor processor, HttpRequestHandlerMapper handlerMapper) {
        //
        // parameter validation is done in super()
        //
        super(processor, handlerMapper);
        
        Long lifetime = Long.getLong("ste.http.session.lifetime", 15*60*1000);
        sessions = new SessionCache(lifetime);
    }
    
    
    public void handleRequest(final HttpServerConnection c)
    throws HttpException, IOException {
        super.handleRequest(c, new HttpSession());
    }
    
    // ------------------------------------------------------- protected methods
    
    @Override
    protected void doService(HttpRequest request, HttpResponse response, HttpContext context) 
    throws HttpException, IOException {
        HttpSession session = (HttpSession)context;
        
        buildHttpSession(request, session);        
        response.addHeader(session.getHeader());
        
        super.doService(request, response, session);
    }
    
    // --------------------------------------------------------- private methods
    
    private void buildHttpSession(HttpRequest request, HttpSession session) {
        String sessionId = null;
        for (Header h: request.getHeaders("Cookie")) {
            HttpCookie cookie = HttpCookie.parse(h.getValue()).get(0);
            if ("JSESSIONID".equals(cookie.getName())) {
                sessionId = cookie.getValue();
                break;
            }
        }
        
        HttpSession existingSession = sessions.get(sessionId);
        session.setId(existingSession.getId());
        session.putAll(existingSession);
    }
}
