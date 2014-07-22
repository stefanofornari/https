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
import java.net.InetAddress;
import java.util.logging.Logger;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpServerConnection;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestHandlerMapper;
import org.apache.http.protocol.HttpService;

/**
 * 
 * TODO: HttpSessionServiceBuilder
 * 
 */
public class HttpSessionService extends HttpService {
    
    public static final String LOG_PATTERN = "%s - - \"%s\" %d";
    
    private Logger LOG = Logger.getLogger(HttpServer.LOG_ACCESS);
    
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
        super.handleRequest(c, new HttpSessionContext());
    }
    
    // ------------------------------------------------------- protected methods
    
    @Override
    protected void doService(HttpRequest request, HttpResponse response, HttpContext context) 
    throws HttpException, IOException {
        selectSession(request, (HttpSessionContext)context);
        
        response.addHeader(((HttpSessionContext)context).getSession().getHeader());
        
        HttpInetConnection connection = (HttpInetConnection)context.getAttribute(HttpCoreContext.HTTP_CONNECTION);
        InetAddress remoteAddress = connection.getRemoteAddress();
        
        super.doService(request, response, context);
        
        LOG.info(String.format(
            LOG_PATTERN,
            remoteAddress.toString().substring(1),
            request.getRequestLine().toString(),
            response.getStatusLine().getStatusCode()
        ));
    }
    
    // --------------------------------------------------------- private methods
    
    private void selectSession(HttpRequest request, HttpSessionContext context) {
        String sessionId = null;
        for (Header h: request.getHeaders("Cookie")) {
            HttpCookie cookie = HttpCookie.parse(h.getValue()).get(0);
            if ("JSESSIONID".equals(cookie.getName())) {
                sessionId = cookie.getValue();
                break;
            }
        }
        
        context.setSession(sessions.get(sessionId));
    }
}
