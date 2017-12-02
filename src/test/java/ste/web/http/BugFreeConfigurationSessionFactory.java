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
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Test;
import static ste.web.http.BaseBugFreeHttpServer.SSL_PASSWORD;
import static ste.web.http.ConfigurationSessionFactory.DEFAULT_LIFETIME;
import static ste.web.http.ConfigurationSessionFactory.REGEX_VALID_NAME;
import static ste.web.http.Constants.CONFIG_HTTPS_ROOT;
import static ste.web.http.Constants.CONFIG_HTTPS_SESSION_ID_NAME;
import static ste.web.http.Constants.CONFIG_HTTPS_SESSION_LIFETIME;
import static ste.web.http.Constants.CONFIG_HTTPS_WEB_PORT;
import static ste.web.http.Constants.CONFIG_SSL_PASSWORD;
import static ste.web.http.Constants.CONFIG_HTTPS_SSL_PORT;

/**
 *
 * @author ste
 */
public class BugFreeConfigurationSessionFactory {
    
    @Test
    public void constructor() throws Exception {
        try {
            new ConfigurationSessionFactory(null);
            fail("missing argument validity check");
        } catch (IllegalArgumentException x) {
            then(x).hasMessage("configuration can not be null");
        }
    }
    
    @Test
    public void default_values() throws Exception {
        final ConfigurationSessionFactory F 
            = new ConfigurationSessionFactory(new PropertiesConfiguration());
        
        then(F.getLifetime()).isEqualTo(DEFAULT_LIFETIME);
        then(F.getSessionIdName()).isEqualTo(SessionHeader.DEFAULT_SESSION_HEADER);
    }
    
    @Test
    public void lifetime_values_ok() throws Exception {
        final PropertiesConfiguration C = new PropertiesConfiguration();
        C.setProperty(CONFIG_HTTPS_SESSION_LIFETIME, 0);
        then(new ConfigurationSessionFactory(C).getLifetime()).isZero();
        
        C.setProperty(CONFIG_HTTPS_SESSION_LIFETIME, 100);
        then(new ConfigurationSessionFactory(C).getLifetime()).isEqualTo(100);
    }
    
    @Test
    public void lifetime_min_value() throws Exception {
    final PropertiesConfiguration C = new PropertiesConfiguration();
        C.setProperty(CONFIG_HTTPS_SESSION_LIFETIME, -10);
        then(new ConfigurationSessionFactory(C).getLifetime()).isZero();
    }
    
    @Test
    public void lifetime_values_ko() throws Exception {  
        final PropertiesConfiguration C = new PropertiesConfiguration();
        
        C.setProperty(CONFIG_HTTPS_SESSION_LIFETIME, "nan");
        try {
            new ConfigurationSessionFactory(C);
        } catch (ConfigurationException x) {
            then(x).hasMessageContaining(CONFIG_HTTPS_SESSION_LIFETIME + " must be a number");
        }
    }
    
    @Test
    public void session_id_name_value_ok() throws Exception {  
        final PropertiesConfiguration C = new PropertiesConfiguration();
        
        for (String N: new String[] {"id", "ID", "iD", "i", "a_b", "_jsessionid", "j_S_eS9iOn1"}) {
            C.setProperty(CONFIG_HTTPS_SESSION_ID_NAME, N);
            then(new ConfigurationSessionFactory(C).getSessionIdName()).isEqualTo(N);
        }
    }
    
    @Test
    public void session_id_name_value_ko() throws Exception {  
        final PropertiesConfiguration C = new PropertiesConfiguration();
        
        for (String N: new String[] {"", "with_è", "with space", "with=char", "with;char", "with'char", "with\"char", "1session"}) {
            C.setProperty(CONFIG_HTTPS_SESSION_ID_NAME, N);
            try {
                new ConfigurationSessionFactory(C);
                fail("missing check for values validity (" + N + ")");
            } catch (ConfigurationException x) {
                then(x).hasMessageContaining(CONFIG_HTTPS_SESSION_ID_NAME + " '" + N + "' must be a valid identifier (" + REGEX_VALID_NAME+ ")");
            }
        }
    }
}
