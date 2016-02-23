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

import java.net.HttpCookie;
import org.apache.http.HttpHeaders;
import org.apache.http.protocol.HttpCoreContext;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Before;
import org.junit.Test;

/**
 * TOTO: improve parsing of Cookie value to extract JSESSIONID (e.g. NOJESSIONID="")
 * 
 * @author ste
 */
public class BugFreeHttpSessionServiceSession extends BugFreeHttpSessionServiceBase {
    
    public final String JSESSION_FORMAT = "JSESSIONID=\"%s\";$Path=\"/\"";
    
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
    public void new_session_if_no_jsession() throws Exception {
        service.doService(TEST_REQUEST1, TEST_RESPONSE1, context);
        HttpSession s1 = context.getSession();
        then(s1).isNotNull();
        then(TEST_RESPONSE1.getHeaders("Set-Cookie")).hasSize(1);
        then(TEST_RESPONSE1.getHeaders("Set-Cookie")[0].getValue())
            .isEqualTo(String.format(JSESSION_FORMAT, s1.getId()));
        
        service.doService(TEST_REQUEST1, TEST_RESPONSE1, context);
        HttpSession s2 = context.getSession();
        then(s2).isNotNull();
        then(s2.getId()).isNotEqualTo(s1.getId());
        then(TEST_RESPONSE1.getHeaders("Set-Cookie")).hasSize(1);
        then(TEST_RESPONSE1.getHeaders("Set-Cookie")[0].getValue())
            .isEqualTo(String.format(JSESSION_FORMAT, s2.getId()));
    }
    
    @Test
    public void same_session_if_jsession_simple() throws Exception {
        service.doService(TEST_REQUEST1, TEST_RESPONSE1, context);
        HttpSession s1 = context.getSession();
        
        TEST_REQUEST1.addHeader(
            "Cookie", 
            String.format("JSESSIONID=\"%s\"", s1.getId())
        );
        service.doService(TEST_REQUEST1, TEST_RESPONSE1, context);
        then(context.getSession().getId()).isEqualTo(s1.getId());
    }
    
    @Test
    public void same_session_if_jsession_with_other_cookies() throws Exception {
        service.doService(TEST_REQUEST1, TEST_RESPONSE1, context);
        HttpSession s1 = context.getSession();
        
        TEST_REQUEST1.addHeader(
            "Cookie", 
            String.format("one=1;JSESSIONID=\"%s\";two=2", s1.getId())
        );
        service.doService(TEST_REQUEST1, TEST_RESPONSE1, context);
        then(context.getSession().getId()).isEqualTo(s1.getId());
    }
    
}
