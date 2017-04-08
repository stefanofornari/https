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
package ste.web.http.api;

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
public class BugFreeApiVersion extends BugFreeBeanShell {
    
    @Before
    public void before() throws Exception {
        super.setUp();
        
        beanshell.set("response", HttpUtils.getBasicResponse(true));
        beanshell.set("session", new HttpSessionContext());
        
        setBshFileNames(
            "src/main/webapp/https/version/get.bsh"
        );
    }
    
    @Test
    public void get_server_version_number() throws Exception {
        exec();
        
        JSONObject result = (JSONObject)beanshell.get("version");
        JSONAssertions.then(result).contains("version");
        JSONObject version = result.getJSONObject("version");
        JSONAssertions.then(version)
            .containsEntry("name", "${pom.name}")
            .containsEntry("ver", "${pom.version}");
    }
    
    
    
}
