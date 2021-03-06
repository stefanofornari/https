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
package ste.web.bsh.commands;

import bsh.Interpreter;
import java.io.File;
import java.util.concurrent.Executors;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ste.web.http.HttpApiServer;
import ste.web.http.HttpServerCLI;
import ste.web.http.HttpSessionContext;
import ste.web.http.HttpUtils;


public class BugFree_getconf {
    private Interpreter i;
    
    @Rule
    public final TemporaryFolder TESTDIR = new TemporaryFolder();
    
    @Before
    public void before() throws Exception {
        i = new Interpreter();
        
        FileUtils.copyDirectory(new File("src/test/conf"), TESTDIR.newFolder("conf"));
        
        System.setProperty(
            "user.home", TESTDIR.getRoot().getAbsolutePath()
        );
        System.setProperty(
            "user.dir", TESTDIR.getRoot().getAbsolutePath()
        );
        Executors.newFixedThreadPool(1).execute(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpServerCLI.main();
                } catch (Exception x) {
                    x.printStackTrace();
                }
            }
            
        });
        
        int c=0;
        do {
            Thread.sleep(250);
        } while ((++c < 5) && (HttpServerCLI.getServer() == null));
        
        
        i.set("response", HttpUtils.getBasicResponse(true));
        i.set("session", new HttpSessionContext());
    }
    
    @After
    public void after() throws Exception {
        HttpApiServer server = HttpServerCLI.getServer();
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void get_me_with_authenticated_session() throws Exception {
        final Configuration C = HttpServerCLI.getServer().getConfiguration();
        then(getconf.invoke(i, null)).isSameAs(C);
    }
}