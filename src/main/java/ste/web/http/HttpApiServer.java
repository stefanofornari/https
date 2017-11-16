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

import java.util.HashMap;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.http.protocol.HttpRequestHandler;
import static ste.web.http.Constants.CONFIG_HTTPS_WEBROOT;
import ste.web.http.api.ApiHandler;
import ste.web.http.handlers.FileHandler;
import ste.web.http.handlers.RestrictedResourceHandler;

/**
 * 
 */
public class HttpApiServer extends HttpServer {
    
    public final String BSH_REGEXP = "(.*)\\.bsh";
    
    public HttpApiServer(Configuration configuration) throws ConfigurationException {
        super(configuration);
        
        final String WEBROOT = configuration.getString(CONFIG_HTTPS_WEBROOT);
        
        HashMap<String, HttpRequestHandler> handlers = new HashMap<>();
        handlers.put(
            "*", 
            new RestrictedResourceHandler(
                new FileHandler(WEBROOT).exclude(BSH_REGEXP), null, null
            )
        );
        handlers.put(
            "/public/*", new FileHandler(WEBROOT).exclude(BSH_REGEXP)
        );
        handlers.put(
            "/api/*", 
            new RestrictedResourceHandler(
                new ApiHandler(WEBROOT), null, null
            )
        );
        
        setHandlers(handlers);
    }
}
