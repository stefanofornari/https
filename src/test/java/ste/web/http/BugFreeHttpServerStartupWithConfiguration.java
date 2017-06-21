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

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Before;
import org.junit.Test;
import static ste.web.http.Constants.*;
import ste.web.http.HttpServer.ClientAuthentication;
import static ste.xtest.Constants.*;

/**
 *
 * TODO: move here web config
 * 
 * @author ste
 */
public class BugFreeHttpServerStartupWithConfiguration extends BaseBugFreeHttpServer {
    
    @Before
    public void setUp() {
        //
        // just dont call the base class method
        //
    }
    
    @Test
    public void createServerWithConfigurationObjectKO() throws Exception {       
        //
        // configuration object
        //
        try {
            new HttpServer(null);
            fail("missing check for null parameters");
        } catch (IllegalArgumentException x) {
            then(x.getMessage()).contains("configuration").contains("can not be null");
        }
    }
    
    @Test
    public void createServerWithConfigurationObjectKORoot() throws Exception {  
        Configuration configuration = new PropertiesConfiguration();
        configuration.setProperty(CONFIG_HTTPS_PORT, "8080");
        configuration.setProperty(CONFIG_HTTPS_WEB_PORT, "8888");
       
        try {
            new HttpServer(configuration);
            fail("missing check for invalid configuration");
        }  catch (ConfigurationException x) {
            then(x.getMessage()).contains(CONFIG_HTTPS_ROOT).contains("unset");
        }
        for (String BLANK: BLANKS) {
            configuration.setProperty(CONFIG_HTTPS_ROOT, BLANK);
            try {
                new HttpServer(configuration);
                fail("missing check for invalid configuration");
            } catch (ConfigurationException x) {
                then(x.getMessage()).contains(CONFIG_HTTPS_ROOT).contains("unset");
            }
        }
        configuration.setProperty(CONFIG_HTTPS_ROOT, "notexisting");
        try {
            new HttpServer(configuration);
            fail("missing check for invalid configuration");
        } catch (ConfigurationException x) {
            then(x.getMessage()).contains("must exist");
        }
    }
    
    @Test
    public void createServerWithConfigurationObjectPort() throws Exception {  
        Configuration configuration = new PropertiesConfiguration();
        configuration.setProperty(CONFIG_HTTPS_ROOT, "src/test");
        configuration.setProperty(CONFIG_SSL_PASSWORD, SSL_PASSWORD);
        
        try {
            new HttpServer(configuration);
            fail("missing check for invalid configuration");
        }  catch (ConfigurationException x) {
            then(x.getMessage()).contains(CONFIG_HTTPS_PORT).contains("unset");
        }
        for (String BLANK: BLANKS_WITHOUT_NULL) {
            configuration.setProperty(CONFIG_HTTPS_PORT, BLANK);
            try {
                new HttpServer(configuration);
                fail("missing check for invalid configuration");
            } catch (ConfigurationException x) {
                then(x.getMessage()).contains(CONFIG_HTTPS_PORT).contains("invalid");
            }
        }
        
        configuration.setProperty(CONFIG_HTTPS_PORT, "notanumber");
        try {
            new HttpServer(configuration);
            fail("missing check for invalid configuration");
        } catch (ConfigurationException x) {
            then(x.getMessage()).contains(CONFIG_HTTPS_PORT).contains("invalid");
        }
        
        configuration.setProperty(CONFIG_HTTPS_PORT, "-1");
        try {
            new HttpServer(configuration);
            fail("missing check for invalid configuration");
        } catch (ConfigurationException x) {
            then(x.getMessage()).contains(String.valueOf(configuration.getProperty(CONFIG_HTTPS_PORT)));
        }
        
        configuration.setProperty(CONFIG_SSL_PASSWORD, SSL_PASSWORD);
        configuration.setProperty(CONFIG_HTTPS_PORT, "8080");
        configuration.setProperty(CONFIG_HTTPS_WEB_PORT, "8888");
        then(new HttpServer(configuration).getSSLPort()).isEqualTo(8080);
        then(new HttpServer(configuration).getWebPort()).isEqualTo(8888);
        
        configuration.setProperty(CONFIG_HTTPS_PORT, "8181");
        configuration.setProperty(CONFIG_HTTPS_WEB_PORT, "8787");
        then(new HttpServer(configuration).getSSLPort()).isEqualTo(8181);
        then(new HttpServer(configuration).getWebPort()).isEqualTo(8787);
    }
    
     @Test
    public void createServerWithConfigurationObjecClientAuthentication() throws Exception {  
        Configuration configuration = new PropertiesConfiguration();
        configuration.setProperty(CONFIG_HTTPS_ROOT, "src/test");
        configuration.setProperty(CONFIG_HTTPS_PORT, "8000");
        configuration.setProperty(CONFIG_HTTPS_WEB_PORT, "8888");
        configuration.setProperty(CONFIG_SSL_PASSWORD, SSL_PASSWORD);
        
        then(new HttpServer(configuration).getAuthentication()).isEqualTo(ClientAuthentication.BASIC);
        
        for (String BLANK: BLANKS) {
            configuration.setProperty(CONFIG_HTTPS_AUTH, BLANK);
            then(new HttpServer(configuration).getAuthentication()).isEqualTo(ClientAuthentication.BASIC);
        }
        
        configuration.setProperty(CONFIG_HTTPS_AUTH, "something");
        then(new HttpServer(configuration).getAuthentication()).isEqualTo(ClientAuthentication.BASIC);
        
        configuration.setProperty(CONFIG_HTTPS_AUTH, "basic");
        then(new HttpServer(configuration).getAuthentication()).isEqualTo(ClientAuthentication.BASIC);
        
        configuration.setProperty(CONFIG_HTTPS_AUTH, "cert");
        then(new HttpServer(configuration).getAuthentication()).isEqualTo(ClientAuthentication.CERTIFICATE);
        
        configuration.setProperty(CONFIG_HTTPS_AUTH, "none");
        then(new HttpServer(configuration).getAuthentication()).isEqualTo(ClientAuthentication.NONE);
    }
}
