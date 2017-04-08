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
import org.apache.commons.io.FileUtils;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static ste.web.http.Constants.CONFIG_HTTPS_PORT;
import static ste.web.http.Constants.CONFIG_HTTPS_ROOT;
import static ste.web.http.Constants.CONFIG_HTTPS_WEBROOT;
import static ste.web.http.Constants.CONFIG_HTTPS_WEB_PORT;

/**
 *
 * @author ste
 */
public class BugFreeHttpServerCLI {
    
    @Rule
    public final TemporaryFolder TESTDIR = new TemporaryFolder();
    
    @Before
    public void before() throws Exception {
        FileUtils.copyDirectory(new File("src/test/conf/"), TESTDIR.newFolder("conf"));
        FileUtils.copyDirectory(new File("src/test/docroot/"), TESTDIR.newFolder("docroot"));
        
        System.setProperty(
            "user.home", TESTDIR.getRoot().getAbsolutePath()
        );
        System.setProperty(
            "user.dir", TESTDIR.getRoot().getAbsolutePath()
        );
    }
    
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
        File conf = new File(TESTDIR.getRoot(), "conf/server.properties");
        conf.delete();
        
        try {
            HttpServerCLI.main(new String[0]);
            fail("missing check for configuration file");
        } catch (ConfigurationException x) {
            then(x).hasMessageContaining("Unable to load the configuration")
                   .hasMessageContaining(conf.getAbsolutePath());
        }
    }
    
    // --------------------------------------------------------- private methods
    
    private HttpApiServer createAndStartServer() throws Exception {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpServerCLI.main(new String[0]);
                } catch (Exception x) {
                    x.printStackTrace();
                }
            }         
        }).start();
        
        HttpApiServer server = null;
        int i = 0;
        while ((server = HttpServerCLI.getServer()) == null && (++i<10)) {
            System.out.println("attendere prego... ");
            Thread.sleep(500);
        }
        
        System.out.println("server: " + server);
        return server;
    }
            
}
