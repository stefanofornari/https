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
 * @TODO set ssl configuration in a configuration file... 
 */
public class HttpServerCLI {
    private static HttpApiServer SERVER;
    
    /**
     *
     * @param args
     * 
     * @throws Exception
     */
    public static void main(String... args) throws Exception {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        
        configuration.addProperty(CONFIG_HTTPS_ROOT, new File(".").getAbsolutePath());
        configuration.addProperty(CONFIG_HTTPS_WEBROOT, new File("docroot").getAbsolutePath());
        /*
        configuration.addProperty(CONFIG_HTTPS_SSL_PORT, "8484");
        configuration.addProperty(CONFIG_HTTPS_WEB_PORT, "8400");
        */
        
        configuration.load(new File("conf/https.properties"));
        
        //System.setProperty("javax.net.debug", "ssl");
        System.setProperty("javax.net.ssl.trustStoreType", "jks");
        System.setProperty("javax.net.ssl.trustStore", "conf/keystore");
        System.setProperty(
            "javax.net.ssl.trustStorePassword",
            configuration.getString(CONFIG_SSL_PASSWORD)
        );
        
        SERVER = new HttpApiServer(configuration);
        
        SERVER.start();
        do {
            Thread.sleep(250);
        } while (SERVER.isRunning());
    }
    
    public static HttpApiServer getServer() {
        return SERVER;
    }
}
