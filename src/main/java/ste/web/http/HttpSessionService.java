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
import java.util.HashMap;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpServerConnection;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.HttpRequestHandlerMapper;
import org.apache.http.protocol.HttpService;

/**
 * 
 * TODO: sessions access must be thread safe
 * 
 */
public class HttpSessionService extends HttpService {
    
    private HashMap<String, HashMap<String, Object>> sessions;

    public HttpSessionService(HttpProcessor processor, HttpRequestHandlerMapper handlerMapper) {
        //
        // parameter validation is done in super()
        //
        super(processor, handlerMapper);
        
        sessions = new HashMap<>();
    }
    
    /**
     * Creates a HttpSessionService with a basic processor. Mainly used to build
     * specs.
     * 
     */
    protected HttpSessionService() {
       super(HttpProcessorBuilder.create().build(), null);
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
        for (Header h: request.getHeaders("Cookie")) {
            HttpCookie cookie = HttpCookie.parse(h.getValue()).get(0);
            if ("JSESSIONID".equals(cookie.getName())) {
                String id = cookie.getValue();
                session.setId(id);
                break;
            }
        }
        
        HashMap<String, Object> sessionData = sessions.get(session.getId());
        if (sessionData == null) {
            sessionData = new HashMap<>();
            sessions.put(session.getId(), sessionData);
        }
        session.setData(sessionData);
    }
}