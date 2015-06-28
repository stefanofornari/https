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

import org.apache.http.HttpHeaders;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.protocol.HttpCoreContext;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Before;
import org.junit.Test;
import ste.web.acl.User;

/**
 * TODO: bug free code for selectSession (see cobertura)
 * 
 * @author ste
 */
public class BugFreeHttpSessionService extends BugFreeHttpSessionServiceBase {
    
    @Before
    @Override
    public void before() throws Exception {
        super.before();
        
        TEST_REQUEST1.removeHeaders(HttpHeaders.AUTHORIZATION);
    }
    
    @Test
    public void basic_entity_shall_have_empty_content_by_default() throws Exception {
        HttpSessionContext context = new HttpSessionContext();
        context.setAttribute(HttpCoreContext.HTTP_CONNECTION, getConnection());
        service.doService(TEST_REQUEST1, TEST_RESPONSE1, context);
        
        BasicHttpEntity body = (BasicHttpEntity)TEST_RESPONSE1.getEntity();
        then(body).isNotNull();
        then(body.getContentLength()).isZero();
        then(body.getContent()).isNotNull();
    }
    
    @Test
    public void no_principal_when_no_credentials_are_provided() throws Exception {
        HttpSessionContext context = new HttpSessionContext();
        context.setAttribute(HttpCoreContext.HTTP_CONNECTION, getConnection());
        service.doService(TEST_REQUEST1, TEST_RESPONSE1, context);
        
        then(context.getPrincipal()).isNull();
    }
    
    @Test
    public void retrieve_principal_from_basic_authentication() throws Exception {
        HttpSessionContext context = new HttpSessionContext();
        context.setAttribute(HttpCoreContext.HTTP_CONNECTION, getConnection());
        
        // abcd:1234
        TEST_REQUEST1.setHeader(HttpHeaders.AUTHORIZATION, "Basic YWJjZDoxMjM0");
        service.doService(TEST_REQUEST1, TEST_RESPONSE1, context);
        
        then(context.getPrincipal()).isNotNull().isInstanceOf(User.class);
        User user = (User)context.getPrincipal();
        then(user.getName()).isEqualTo("abcd");
        then(user.getSecret()).isEqualTo("1234");
        
        // efgh:5678
        TEST_REQUEST1.setHeader(HttpHeaders.AUTHORIZATION, "Basic ZWZnaDo1Njc4");
        service.doService(TEST_REQUEST1, TEST_RESPONSE1, context);
        
        then(context.getPrincipal()).isNotNull().isInstanceOf(User.class);
        user = (User)context.getPrincipal();
        then(user.getName()).isEqualTo("efgh");
        then(user.getSecret()).isEqualTo("5678");
        
        //
        // for other comination of username and password see HttpUtils
        //
    }
}
