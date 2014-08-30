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
import ste.xtest.reflect.PrivateAccess;

/**
 *
 * @author ste
 */
public class BugFreeHttpServerHandlers extends BugFreeHttpServerBase {
    
    @Rule
    public final ProvideSystemProperty SSL_PASSWORD
	 = new ProvideSystemProperty(CONFIG_SSL_PASSWORD, "20150630");
    
    @Test
    public void setHandlersDefault() throws Exception {
        server.setHandlers(null);
        
        //
        // Not very nice because it may break if apache HttpServer changes the
        // internal implementation, but good enough for now...
        //
        HttpRequestHandlerMapper handlers = 
            (HttpRequestHandlerMapper)PrivateAccess.getInstanceValue(server.getHttpService(), "handlerMapper");
        
        then(handlers).isNotNull();
    }
    
    @Test
    public void setHandlers() throws Exception {
        server.setHandlers(null);
        then(PrivateAccess.getInstanceValue(server.getHttpService(), "handlerMapper")).isNotNull();
        
        //
        // Not very nice because it may break if apache HttpServer changes the
        // internal implementation, but good enough for now...
        //
        UriHttpRequestHandlerMapper handlers = new UriHttpRequestHandlerMapper();
        server.setHandlers(handlers);
        then(PrivateAccess.getInstanceValue(server.getHttpService(), "handlerMapper"))
            .isSameAs(handlers);
    }
    
    
    // -------------------------------------------------------------------------
    
    class TestHandler implements HttpRequestHandler {

        @Override
        public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
        }
  
    }
}
