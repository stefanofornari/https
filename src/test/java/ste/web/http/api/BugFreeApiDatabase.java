/*
 * Copyright (C) 2018 Stefano Fornari.
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
package ste.web.http.api;

import java.io.File;
import java.util.Properties;
import static org.assertj.core.api.BDDAssertions.then;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import ste.web.http.HttpSessionContext;
import ste.web.http.HttpUtils;
import ste.xtest.beanshell.BugFreeBeanShell;
import ste.xtest.json.api.JSONAssertions;

/**
 *
 * @author ste
 */
public class BugFreeApiDatabase extends BugFreeBeanShell {
    
    @Before
    public void before() throws Exception {
        super.setUp();
        
        beanshell.set("response", HttpUtils.getBasicResponse(true));
        beanshell.set("session", new HttpSessionContext());
        
        setBshFileNames(
            "src/main/webapp/https/db/get.bsh"
        );
    }
    
    @Test
    public void get_zero_datasrources() throws Exception {
        beanshell.set("jndiroot", new File("src/test/conf/https.properties").getAbsolutePath());
        exec();
        
        JSONObject result = (JSONObject)beanshell.get("db");
        JSONAssertions.then(result).contains("datasources");
        JSONArray datasources = result.getJSONArray("datasources");
        JSONAssertions.then(datasources)
            .hasSize(0);
    }
    
    @Test
    public void get_one_datasrource() throws Exception {
        beanshell.set("jndiroot", new File("src/test/conf/https2.properties").getAbsolutePath());
        
        exec();
        
        JSONObject result = (JSONObject)beanshell.get("db");
        JSONAssertions.then(result).contains("datasources");
        JSONArray datasources = result.getJSONArray("datasources");
        JSONAssertions.then(datasources)
            .hasSize(1);
        JSONAssertions.then(datasources.getJSONObject(0)).containsEntry("java:comp/env/jdbc/ds1", "org.osjava.sj.loader.SJDataSource");
    }
    
    @Test
    public void get_two_datasrource() throws Exception {
        beanshell.set("jndiroot", new File("src/test/conf/https3.properties").getAbsolutePath());
        
        exec();
        
        JSONObject result = (JSONObject)beanshell.get("db");
        JSONAssertions.then(result).contains("datasources");
        JSONArray datasources = result.getJSONArray("datasources");
        JSONAssertions.then(datasources)
            .hasSize(2);
        JSONAssertions.then(datasources.getJSONObject(1))
            .containsEntry("java:comp/env/jdbc/ds1", "org.osjava.sj.loader.SJDataSource");
        JSONAssertions.then(datasources.getJSONObject(0))
            .containsEntry("java:comp/env/jdbc/ds2", "org.osjava.sj.loader.SJDataSource");
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
