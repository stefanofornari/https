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

import static org.assertj.core.api.BDDAssertions.then;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static ste.web.http.Constants.*;
import ste.xtest.reflect.PrivateAccess;

/**
 * 
 * @author ste
 */
@Ignore
public class BugFreeHttpServerListeners extends BaseBugFreeHttpServer {
    
    @Rule
    public final TemporaryFolder TESTDIR = new TemporaryFolder();
    
    @Before
    public void setUp() {
        //
        // just dont call the base class method
        //
    }
    
    @After
    @Override
    public void after() throws Exception {
        super.after();
        waitServerShutdown();
    }
    
    
    @Test
    public void create_server_with_configuration_object_port_diable_ssl_listener() throws Exception {
        createDefaultConfiguration();
        
        configuration.setProperty(CONFIG_HTTPS_SSL_PORT, "-1");
        
        createServer(); server.start(); waitServerStartup();
        
        then(PrivateAccess.getInstanceValue(server, "listenerThread")).isNull();
        then(PrivateAccess.getInstanceValue(server, "webListenerThread")).isNotNull();
        
        server.stop(); waitServerShutdown();
        
        configuration.setProperty(CONFIG_HTTPS_SSL_PORT, "0");
        
        createServer(); server.start(); waitServerStartup();
        
        then(PrivateAccess.getInstanceValue(server, "listenerThread")).isNull();
        then(PrivateAccess.getInstanceValue(server, "webListenerThread")).isNotNull();
        
        server.stop(); waitServerShutdown();
    }
    
    @Test
    public void create_server_with_configuration_object_port_diable_web_listener() throws Exception {
        createDefaultConfiguration();
        
        configuration.setProperty(CONFIG_HTTPS_WEB_PORT, "-1");
        
        createServer(); server.start(); waitServerStartup();
        
        then(PrivateAccess.getInstanceValue(server, "webListenerThread")).isNull();
        then(PrivateAccess.getInstanceValue(server, "listenerThread")).isNotNull();
        
        server.stop(); waitServerShutdown();
        
        configuration.setProperty(CONFIG_HTTPS_WEB_PORT, "0");
        
        createServer(); server.start(); waitServerStartup();
        
        then(PrivateAccess.getInstanceValue(server, "webListenerThread")).isNull();
        then(PrivateAccess.getInstanceValue(server, "listenerThread")).isNotNull();
        
        server.stop(); waitServerShutdown();
    }
}
