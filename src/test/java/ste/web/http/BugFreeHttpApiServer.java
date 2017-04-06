/*
 * Copyright (C) 2017 Stefano Fornari.
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

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.http.protocol.HttpRequestHandlerMapper;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ste.web.http.api.ApiHandler;
import ste.web.http.handlers.FileHandler;
import ste.web.http.handlers.RestrictedResourceHandler;

/**
 * 
 * @author ste
 */
public class BugFreeHttpApiServer extends AbstractBugFreeHttpServer {
    
    @Before
    @Override
    public void set_up() throws Exception {   
        super.set_up();

    }
    
    @After
    public void after() throws Exception {
        if (server != null) {
            server.stop(); waitServerShutdown();
        }
    }
    
    
    /**
     *
     * 
     * By default we want the following handlers:
     * 
     * '*', restricted, any resource under WEBROOT
     * '/public/*', not restricted, any resource under WEBROOT
     * '/api/*', restricted, any Api resource
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void set_api_and_public_handlers() throws Exception {
        HttpRequestHandlerMapper sslHandlers = server.getSSLHandlers();
        HttpRequestHandlerMapper webHandlers = server.getWebHandlers();
        
        then(webHandlers.lookup(HttpUtils.getSimpleGet("/1/something"))).isNull();
        then(webHandlers.lookup(HttpUtils.getSimpleGet("/public/something")))
            .isNotNull().isInstanceOf(FileHandler.class);
        
        RestrictedResourceHandler h = 
            (RestrictedResourceHandler)sslHandlers.lookup(HttpUtils.getSimpleGet("/1/something"));
        then(h).isNotNull();
        then(h.getHandler()).isInstanceOf(FileHandler.class);
        
        h = 
            (RestrictedResourceHandler)sslHandlers.lookup(HttpUtils.getSimpleGet("/api/something"));
        then(h).isNotNull();
        then(h.getHandler()).isInstanceOf(ApiHandler.class);
    }
    
    @Test
    public void get_configuration() throws Exception {
        final PropertiesConfiguration C1 = (PropertiesConfiguration)configuration;
        final PropertiesConfiguration C2 = (PropertiesConfiguration)C1.clone();
        
        then(server.getConfiguration()).isSameAs(C1);
        
        server = new HttpApiServer(C2);
        then(server.getConfiguration()).isSameAs(C2);
    }

    
    // ------------------------------------------------------- protected methods
      
    @Override
    protected void createServer() throws Exception {
        server = new HttpApiServer(configuration);
    }
    
}