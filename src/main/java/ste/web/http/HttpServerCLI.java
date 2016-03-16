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

import java.util.HashMap;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;
import static ste.web.http.Constants.*;
import ste.web.http.handlers.FileHandler;

/**
 *
 * @author ste
 */
public class HttpServerCLI {
    public static void main(String[] args) throws Exception {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.addProperty(CONFIG_HTTPS_ROOT, "src/test");
        configuration.addProperty(CONFIG_HTTPS_WEBROOT, "src/test/docroot");
        configuration.addProperty(CONFIG_HTTPS_PORT, "8000");
        configuration.addProperty(CONFIG_SSL_PASSWORD, "20150630");
        
        System.setProperty("javax.net.debug", "ssl");
        System.setProperty("javax.net.ssl.trustStoreType", "jks");
        System.setProperty("javax.net.ssl.trustStore", "src/test/conf/castore");
        System.setProperty("javax.net.ssl.trustStorePassword", "20150630");
        
        HttpServer server = new HttpServer(configuration);
        
        HashMap<String, HttpRequestHandler> handlers = new HashMap<>();
        handlers.put("*", new FileHandler("src/test/docroot"));
        server.setHandlers(handlers);
        
        server.start();
    }
}
