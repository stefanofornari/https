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
import java.util.Map;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.sql.DataSource;
import org.apache.commons.io.FileUtils;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osjava.sj.DelimiterConvertingContext;
import org.osjava.sj.SimpleContextFactory;
import ste.xtest.reflect.PrivateAccess;

/**
 *  NOTO: this specs use system properties, be careful running specs in parallel
 */
public class BugFreeHttpServerDB extends BaseBugFreeHttpServer {
    
    @Rule
    public final TemporaryFolder TESTDIR = new TemporaryFolder();
    
    @Before
    public void before_class() throws Exception {
        FileUtils.copyDirectory(new File("src/test/conf/"), TESTDIR.newFolder("conf"));
        String home = new File("src/test").getAbsolutePath();
        System.setProperty("user.home", TESTDIR.getRoot().getAbsolutePath());
        System.setProperty("user.dir", TESTDIR.getRoot().getAbsolutePath());
    }
    
    @Test
    public void after_server_creation_initial_context_is_provided()
    throws Exception {
        server.start(); waitServerStartup();
        
        File confFile = new File(TESTDIR.getRoot(), "conf/https.properties");
        FileUtils.copyFile(new File("src/test/conf/https1.properties"), confFile);
        
        InitialContext ctx = new InitialContext();
        then(ctx.lookup("java:comp/env/jdbc/ds1")).isNotNull().isInstanceOf(DataSource.class);
        then(ctx.lookup("java:comp/env/jdbc/ds2")).isNull();
        
        resetContext(ctx);
        FileUtils.copyFile(new File("src/test/conf/https2.properties"), confFile);
        
        ctx = new InitialContext();
        then(ctx.lookup("java:comp/env/jdbc/ds1")).isNull();
        then(ctx.lookup("java:comp/env/jdbc/ds2")).isNotNull().isInstanceOf(DataSource.class);
        then(ctx.lookup("java:comp/env/jdbc/ds3")).isNotNull().isInstanceOf(DataSource.class);
    }
    
    // --------------------------------------------------------- private methods
    
    private void resetContext(Context ctx) throws Exception {
        NamingEnumeration<NameClassPair> bindings = ctx.list("");
        while (bindings.hasMore()) {
            ctx.unbind(bindings.next().getName());
        }
        Map<String, DelimiterConvertingContext> root =
            (Map<String, DelimiterConvertingContext>)PrivateAccess.getStaticValue(SimpleContextFactory.class, "contextsByRoot");
        root.clear();
    }
    
}
