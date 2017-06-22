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
import java.net.URL;
import java.util.Properties;
import org.junit.Test;

/**
 *
 */
public class BugFreeHttpServerDB extends BaseBugFreeHttpServer {
    
    @Test
    public void after_server_creation_initial_context_is_provided()
    throws Exception {
        /*
        server.start(); waitServerStartup();
        
        URL url = new URL("https://localhost:" + server.getSSLPort() + "/api/https/conf/get");
        Object content = url.getContent();
        
        System.out.println(content);
        */
        
        /*
        Properties env = createJNDIContext();
        
        DataSource ds1 = (DataSource)new InitialContext(env).lookup("java:comp/env/jdbc/ds1");
        then(ds1).isNotNull().isInstanceOf(DataSource.class);
        
        env.put("org.osjava.sj.root", new File("conf/https2.properties").getAbsolutePath());
        DataSource ds2 = (DataSource)new InitialContext(env).lookup("java:comp/env/jdbc/ds1");
        
        System.out.println(ds1 == ds2);
        */
    }
    
    // --------------------------------------------------------- private methods
    
    private Properties createJNDIContext() {
        final Properties env = new Properties();
        env.put("java.naming.factory.initial", "org.osjava.sj.SimpleContextFactory");
        env.put("org.osjava.sj.delimiter", "/");
        env.put("org.osjava.sj.space", "java:comp/env");
        env.put("org.osjava.sj.root", new File("conf/https.properties").getAbsolutePath());
        env.put("org.osjava.sj.jndi.shared", "true");
        
        return env;
    }
    
}
