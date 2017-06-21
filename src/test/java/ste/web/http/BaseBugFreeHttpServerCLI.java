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
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 *
 */
public abstract class BaseBugFreeHttpServerCLI {
    
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
    
    // --------------------------------------------------------- private methods
    
    protected HttpApiServer createAndStartServer() throws Exception {
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
