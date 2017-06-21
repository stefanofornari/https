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

import java.io.File;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Test;
import static ste.web.http.Constants.CONFIG_HTTPS_PORT;
import static ste.web.http.Constants.CONFIG_HTTPS_ROOT;
import static ste.web.http.Constants.CONFIG_HTTPS_WEBROOT;
import static ste.web.http.Constants.CONFIG_HTTPS_WEB_PORT;
import static ste.web.http.Constants.CONFIG_SSL_PASSWORD;

/**
 *
 */
public class BugFreeHttpServerCLI extends BaseBugFreeHttpServerCLI {
    
    @Test
    public void start_with_default_parameters() throws Exception {
        HttpApiServer server = createAndStartServer();
        
        then(server).isNotNull();
        
        final String ROOT = TESTDIR.getRoot().getAbsolutePath();
        
        Configuration c = server.getConfiguration();
        then(c.getString(CONFIG_HTTPS_ROOT)).isEqualTo(ROOT + "/.");
        then(c.getString(CONFIG_HTTPS_WEBROOT)).isEqualTo(new File(ROOT, "docroot").getAbsolutePath());
        then(c.getInt(CONFIG_HTTPS_PORT)).isEqualTo(8484);
        then(c.getInt(CONFIG_HTTPS_WEB_PORT)).isEqualTo(8400);
    }
    
    @Test
    public void server_runs_until_closed() throws Exception {
        HttpApiServer server = createAndStartServer();
                
        //
        // Now the server should be started...
        //
        
        then(server.isRunning()).isTrue();
        Thread.sleep(250);
        then(server.isRunning()).isTrue();
        Thread.sleep(250);
        
        server.stop();
        Thread.sleep(250);
        then(server.isRunning()).isFalse();
    }
    
    @Test
    public void error_if_missing_configuration_file() throws Exception {
        File conf = new File(TESTDIR.getRoot(), "conf/https.properties");
        conf.delete();
        
        try {
            HttpServerCLI.main(new String[0]);
            fail("missing check for configuration file");
        } catch (ConfigurationException x) {
            then(x).hasMessageContaining("Unable to load the configuration")
                   .hasMessageContaining(conf.getAbsolutePath());
        }
    }
    
    @Test
    public void read_key_store_password() throws Exception {
        File conf = new File(TESTDIR.getRoot(), "conf/https.properties");
        
        PropertiesConfiguration c = new PropertiesConfiguration(conf);
        
        //
        // Set the trustStorePassword property to an arbitrary value (with the 
        // additional side effect of making the server abort so that main() 
        // exits
        //
        c.setProperty(CONFIG_SSL_PASSWORD, "pass1");
        c.save();
        try {
            HttpServerCLI.main(new String[0]);
        } catch (ConfigurationException x) {
            then(x).hasMessageContaining("Keystore was tampered");
        }
        then(System.getProperty("javax.net.ssl.trustStorePassword")).
            isEqualTo("pass1");
        
        c.setProperty(CONFIG_SSL_PASSWORD, "pass2");
        c.save();
        try {
            HttpServerCLI.main(new String[0]);
        } catch (ConfigurationException x) {
            then(x).hasMessageContaining("Keystore was tampered");
        }
        then(System.getProperty("javax.net.ssl.trustStorePassword")).
            isEqualTo("pass2");
    }       
}
