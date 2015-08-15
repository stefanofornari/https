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
import java.util.HashMap;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerMapper;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import static ste.web.http.Constants.CONFIG_SSL_PASSWORD;
import ste.web.http.handlers.RestrictedResourceHandler;
import ste.xtest.reflect.PrivateAccess;

/**
 *
 * @author ste
 */
public class BugFreeHttpServerHandlers extends AbstractBugFreeHttpServer {
    
    @Rule
    public final ProvideSystemProperty SSL_PASSWORD
	 = new ProvideSystemProperty(CONFIG_SSL_PASSWORD, "20150630");
    
    @Test
    public void set_default_handlers() throws Exception {
        server.setHandlers(null);
        
        //
        // Not very nice because it may break if apache HttpServer changes the
        // internal implementation, but good enough for now...
        //
        HttpRequestHandlerMapper handlers = 
            (HttpRequestHandlerMapper)PrivateAccess.getInstanceValue(server.getSSLService(), "handlerMapper");
        
        then(handlers).isNotNull();
    }
        
    @Test
    public void not_restricted_handlers_set_for_both_ssl_and_web() throws Exception {
        server.setHandlers(null);
        then(PrivateAccess.getInstanceValue(server.getSSLService(), "handlerMapper")).isNotNull();
        
        //
        // Not very nice because it may break if apache HttpServer changes the
        // internal implementation, but good enough for now...
        //
        HashMap<String, HttpRequestHandler> handlers = new HashMap<>();
        handlers.put("/1/*", new TestHandler());
        handlers.put("/2/*", new TestHandler());
        
        server.setHandlers(handlers);
        
        UriHttpRequestHandlerMapper mapper = 
            (UriHttpRequestHandlerMapper)PrivateAccess.getInstanceValue(server.getSSLService(), "handlerMapper");
        then(mapper.lookup(HttpUtils.getSimpleGet("/1/something"))).isSameAs(handlers.get("/1/*"));
        then(mapper.lookup(HttpUtils.getSimpleGet("/2/something"))).isSameAs(handlers.get("/2/*"));
    }
    
    @Test
    public void restricted_handlers_set_for_ssl_only() throws Exception {
        HashMap<String, HttpRequestHandler> handlers = new HashMap<>();
        
        handlers.put("/1/*", new RestrictedResourceHandler(new TestHandler(), null, null));
        handlers.put("/2/*", new RestrictedResourceHandler(new TestHandler(), null, null));
        
        server.setHandlers(handlers);
        
        UriHttpRequestHandlerMapper mapper = 
            (UriHttpRequestHandlerMapper)PrivateAccess.getInstanceValue(server.getSSLService(), "handlerMapper");
        then(mapper.lookup(HttpUtils.getSimpleGet("/1/something"))).isSameAs(handlers.get("/1/*"));
        then(mapper.lookup(HttpUtils.getSimpleGet("/2/something"))).isSameAs(handlers.get("/2/*"));
        
        mapper = 
            (UriHttpRequestHandlerMapper)PrivateAccess.getInstanceValue(server.getWebService(), "handlerMapper");
        then(mapper.lookup(HttpUtils.getSimpleGet("/1/something"))).isNull();
        then(mapper.lookup(HttpUtils.getSimpleGet("/2/something"))).isNull();
    }
    
    @Test
    public void mixed_handlers_set_for_ssl_or_web() throws Exception {
        HashMap<String, HttpRequestHandler> handlers = new HashMap<>();
        
        handlers.put("/1/*", new RestrictedResourceHandler(new TestHandler(), null, null));
        handlers.put("/2/*", new TestHandler());
        
        server.setHandlers(handlers);
        
        UriHttpRequestHandlerMapper mapper = 
            (UriHttpRequestHandlerMapper)PrivateAccess.getInstanceValue(server.getSSLService(), "handlerMapper");
        then(mapper.lookup(HttpUtils.getSimpleGet("/1/something"))).isSameAs(handlers.get("/1/*"));
        then(mapper.lookup(HttpUtils.getSimpleGet("/2/something"))).isSameAs(handlers.get("/2/*"));
        
        mapper = 
            (UriHttpRequestHandlerMapper)PrivateAccess.getInstanceValue(server.getWebService(), "handlerMapper");
        then(mapper.lookup(HttpUtils.getSimpleGet("/1/something"))).isNull();
        then(mapper.lookup(HttpUtils.getSimpleGet("/2/something"))).isSameAs(handlers.get("/2/*"));
    }
    
    // -------------------------------------------------------------------------
    
    class TestHandler implements HttpRequestHandler {

        @Override
        public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
        }
  
    }
}
