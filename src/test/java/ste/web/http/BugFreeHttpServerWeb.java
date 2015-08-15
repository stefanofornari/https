/*
 * Copyright (C) 2015 Stefano Fornari.
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

import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.commons.configuration.ConfigurationException;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Test;
import static ste.web.http.Constants.CONFIG_HTTPS_WEB_PORT;

/**
 *
 * @author ste
 */
public class BugFreeHttpServerWeb extends AbstractBugFreeHttpServer {

    @Test
    public void start_web_on_given_port() throws Exception {
        URL web = new URL("http://localhost:8888/index.html");
        URL ssh = new URL("https://localhost:8400/index.html");
        
        server.start(); waitServerStartup();
        
        then(((HttpURLConnection)ssh.openConnection()).getResponseCode()).isEqualTo(200);
        then(((HttpURLConnection)web.openConnection()).getResponseCode()).isEqualTo(200);
        
        server.stop(); waitServerShutdown();
        
        configuration.setProperty(Constants.CONFIG_HTTPS_WEB_PORT, "8880");
        createServer(); server.start(); waitServerStartup();
        
        web = new URL("http://localhost:8880/index.html");
        then(((HttpURLConnection)web.openConnection()).getResponseCode()).isEqualTo(200);
    }
    
    @Test
    public void start_web_on_incorrect_port() throws Exception {
        configuration.setProperty(Constants.CONFIG_HTTPS_WEB_PORT, "nan");
        try {
            createServer();
            fail("missing invalid port check");
        } catch (ConfigurationException x) {
            then(x).hasMessageContaining("the web port <nan> is invalid")
                   .hasMessageContaining(CONFIG_HTTPS_WEB_PORT);
        }
        
        configuration.setProperty(Constants.CONFIG_HTTPS_WEB_PORT, "-1");
        try {
            createServer();
            fail("missing invalid port check");
        } catch (ConfigurationException x) {
            then(x).hasMessageStartingWith("the web port <-1> is invalid")
                   .hasMessageContaining(CONFIG_HTTPS_WEB_PORT);
        }
        
        configuration.setProperty(Constants.CONFIG_HTTPS_WEB_PORT, "");
        try {
            createServer();
            fail("missing invalid port check");
        } catch (ConfigurationException x) {
            then(x).hasMessageContaining("the web port <> is invalid")
                   .hasMessageContaining(CONFIG_HTTPS_WEB_PORT);
        }
        
        configuration.setProperty(Constants.CONFIG_HTTPS_WEB_PORT, null);
        try {
            createServer();
            fail("missing invalid port check");
        } catch (ConfigurationException x) {
            then(x).hasMessageStartingWith("the web port is unset;")
                   .hasMessageContaining(CONFIG_HTTPS_WEB_PORT);
        }
    }
    
    @Test
    public void web_server_has_its_own_handlers() {
        
    }

    // --------------------------------------------------------- private methods

    
}
