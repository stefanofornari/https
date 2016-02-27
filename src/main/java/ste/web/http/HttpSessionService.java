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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpCookie;
import java.net.InetAddress;
import java.util.logging.Logger;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpServerConnection;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestHandlerMapper;
import org.apache.http.protocol.HttpService;
import ste.web.acl.User;

/**
 * 
 * TODO: HttpSessionServiceBuilder
 * TODO: shall session handling be implemented with a HttpRequestInterceptor ?
 * 
 */
public class HttpSessionService extends HttpService {
    
    public static final String LOG_PATTERN = "%s - %s \"%s\" %d";
    
    private Logger LOG = Logger.getLogger(HttpServer.LOG_ACCESS);
    
    private SessionCache sessions;

    public HttpSessionService(
        HttpProcessor processor, 
        HttpRequestHandlerMapper handlerMapper,
        long lifetime
    ) {
        //
        // parameter validation is done in super()
        //
        super(processor, handlerMapper);
        
        sessions = new SessionCache(lifetime);
    }
    
    
    public void handleRequest(final HttpServerConnection c)
    throws HttpException, IOException {
        //
        // TODO: server error handling (not related to IO or protocol)
        //
        super.handleRequest(c, new HttpSessionContext());
    }
        
    // ------------------------------------------------------- protected methods
    
    @Override
    protected void doService(HttpRequest request, HttpResponse response, HttpContext context) 
    throws HttpException, IOException {
        String sessionId = sessionId(request, (HttpSessionContext)context);
        
        HttpSession session = sessions.get(sessionId);
        ((HttpSessionContext)context).setSession(session);
        
        if (!session.getId().equals(sessionId)) {
            response.setHeader(session.getHeader());
        }

        response.setEntity(createEmptyEntity());
        
        HttpInetConnection connection = (HttpInetConnection)context.getAttribute(HttpCoreContext.HTTP_CONNECTION);
        InetAddress remoteAddress = connection.getRemoteAddress();
        
        setPrincipal(request, (HttpSessionContext)context);
        
        super.doService(request, response, context);
        
        LOG.info(String.format(
            LOG_PATTERN,
            remoteAddress.toString().substring(1),
            session.getId(),
            request.getRequestLine().toString(),
            response.getStatusLine().getStatusCode()
        ));
    }
    
    // --------------------------------------------------------- private methods
    
    private String sessionId(HttpRequest request, HttpSessionContext context) {
        for (Header h: request.getHeaders("Cookie")) {
            String sessionId = extractSessionId(h.getValue());
            if (sessionId != null) {
                return sessionId;
            }
        }
        
        return null;
    }
    
    private String extractSessionId(String cookies) {
        final String DELIMITER = "JSESSIONID=";
        final int DELIMITER_SIZE = DELIMITER.length();
        
        int i = cookies.lastIndexOf(DELIMITER);
        if (i>=0) {
            int e = cookies.indexOf(";", i+DELIMITER_SIZE);
            return (e >= 0) ? cookies.substring(i+DELIMITER_SIZE, e).replace("\"", "") 
                            : cookies.substring(i+DELIMITER_SIZE).replace("\"", "");
        }
        return null;
    }
    
    private void setPrincipal(HttpRequest request, HttpSessionContext context) {
        Header h = request.getFirstHeader(HttpHeaders.AUTHORIZATION);
        if (h != null) {
            context.setPrincipal(userFromAuthotizationHeader(h));
        }
    }
    
    private BasicHttpEntity createEmptyEntity() {
        BasicHttpEntity e = new BasicHttpEntity();
        e.setContentLength(0);
        e.setContent(new ByteArrayInputStream(new byte[0]));
        
        return e;
    }

    private User userFromAuthotizationHeader(final Header authorization) {
        Pair<String, String> cred = HttpUtils.parseBasicAuth(authorization);
        
        User user = null;
        
        if (cred != null) {
            user = new User(cred.getLeft());
            user.setSecret(cred.getRight());
        }
        
        return user;
    }
}
