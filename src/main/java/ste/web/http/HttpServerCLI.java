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

import java.io.File;
import org.apache.commons.configuration.PropertiesConfiguration;
import static ste.web.http.Constants.*;

/**
 *
 * @author ste
 */
public class HttpServerCLI {
    private static HttpApiServer SERVER;
    
    public static void main(String[] args) throws Exception {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.addProperty(CONFIG_HTTPS_ROOT, new File(".").getAbsolutePath());
        configuration.addProperty(CONFIG_HTTPS_WEBROOT, new File("docroot").getAbsolutePath());
        configuration.addProperty(CONFIG_HTTPS_PORT, "8484");
        configuration.addProperty(CONFIG_HTTPS_WEB_PORT, "8400");
        configuration.addProperty(CONFIG_SSL_PASSWORD, "20150630");
        
        System.setProperty("javax.net.debug", "ssl");
        System.setProperty("javax.net.ssl.trustStoreType", "jks");
        System.setProperty("javax.net.ssl.trustStore", "conf/castore");
        System.setProperty("javax.net.ssl.trustStorePassword", "20150630");
        
        SERVER = new HttpApiServer(configuration);
        
        //server.start();
    }
    
    public static HttpApiServer getServer() {
        return SERVER;
    }
}
