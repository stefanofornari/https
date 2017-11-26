/*
 * Copyright (C) 2016 Stefano Fornari.
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

import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HttpCoreContext;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Before;
import org.junit.Test;

/**
 * TOTO: improve parsing of Cookie value to extract HTTPSID (e.g. NOHTTPSID="")
 * 
 * @author ste
 */
public class BugFreeHttpSessionServiceSession extends BugFreeHttpSessionServiceBase {
    
    public final String SESSIONID_FORMAT = "HTTPSID=%s; Path=/; Secure; HttpOnly";
    
    HttpSessionContext context = null;
    
    @Before
    @Override
    public void before() throws Exception {
        super.before();
        context = new HttpSessionContext();
        context.setAttribute(HttpCoreContext.HTTP_CONNECTION, getConnection());
        TEST_REQUEST1.removeHeaders("Cookie");
    }
    
    @Test
    public void new_session_if_no_session() throws Exception {
        service.doService(TEST_REQUEST1, TEST_RESPONSE1, context);
        HttpSession s1 = context.getSession();
        then(s1).isNotNull();
        then(TEST_RESPONSE1.getHeaders("Set-Cookie")).hasSize(1);
        then(TEST_RESPONSE1.getHeaders("Set-Cookie")[0].getValue())
            .isEqualTo(String.format(SESSIONID_FORMAT, s1.getId()));
        
        service.doService(TEST_REQUEST1, TEST_RESPONSE1, context);
        HttpSession s2 = context.getSession();
        then(s2).isNotNull();
        then(s2.getId()).isNotEqualTo(s1.getId());
        then(TEST_RESPONSE1.getHeaders("Set-Cookie")).hasSize(1);
        then(TEST_RESPONSE1.getHeaders("Set-Cookie")[0].getValue())
            .isEqualTo(String.format(SESSIONID_FORMAT, s2.getId()));
    }
    
    @Test
    public void no_jsession_if_session_simple() throws Exception {
        BasicHttpResponse res1 = HttpUtils.getBasicResponse();
        service.doService(TEST_REQUEST1, res1, context);
        HttpSession s1 = context.getSession();
        
        TEST_REQUEST1.addHeader(
            "Cookie", 
            String.format("HTTPSID=%s", s1.getId())
        );
        BasicHttpResponse res2 = HttpUtils.getBasicResponse();
        service.doService(TEST_REQUEST1, res2, context);
        then(context.getSession().getId()).isEqualTo(s1.getId());
        then(res2.getHeaders("Set-Cookie")).isEmpty();
    }
    
    @Test
    public void skip_quotes_in_sessionid() throws Exception {
        BasicHttpResponse res1 = HttpUtils.getBasicResponse();
        service.doService(TEST_REQUEST1, res1, context);
        HttpSession s1 = context.getSession();
        
        TEST_REQUEST1.addHeader(
            "Cookie", 
            String.format("HTTPSID=\"%s\"", s1.getId())
        );
        BasicHttpResponse res2 = HttpUtils.getBasicResponse();
        service.doService(TEST_REQUEST1, res2, context);
        then(context.getSession().getId()).isEqualTo(s1.getId());
        then(res2.getHeaders("Set-Cookie")).isEmpty();
    }
    
    @Test
    public void no_jsession_if_jsession_with_other_cookies() throws Exception {
        BasicHttpResponse res1 = HttpUtils.getBasicResponse();
        service.doService(TEST_REQUEST1, res1, context);
        HttpSession s1 = context.getSession();
        
        TEST_REQUEST1.addHeader(
            "Cookie", 
            String.format("one=1;HTTPSID=%s;two=2", s1.getId())
        );
        BasicHttpResponse res2 = HttpUtils.getBasicResponse();
        service.doService(TEST_REQUEST1, res2, context);
        then(context.getSession().getId()).isEqualTo(s1.getId());
        then(res2.getHeaders("Set-Cookie")).isEmpty();
    }
    
    @Test
    public void new_jsession_if_invalid_jsession() throws Exception {
        BasicHttpResponse res1 = HttpUtils.getBasicResponse();
        service.doService(TEST_REQUEST1, res1, context);
        HttpSession s1 = context.getSession();
        
        TEST_REQUEST1.addHeader("Cookie", "one=1;HTTPSID=123;two=2");
        BasicHttpResponse res2 = HttpUtils.getBasicResponse();
        service.doService(TEST_REQUEST1, res2, context);
        HttpSession s2 = context.getSession();
        then(s2).isNotNull();
        then(s2.getId()).isNotEqualTo(s1.getId());
        then(res2.getHeaders("Set-Cookie")).hasSize(1);
        then(res2.getHeaders("Set-Cookie")[0].getValue())
            .isEqualTo(String.format(SESSIONID_FORMAT, s2.getId()));
    }
    
}
