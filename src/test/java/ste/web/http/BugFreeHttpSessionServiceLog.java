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

import java.net.InetAddress;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.protocol.HttpCoreContext;
import static org.assertj.core.api.BDDAssertions.then;
import org.assertj.core.api.Condition;
import org.junit.BeforeClass;
import org.junit.Test;
import ste.xtest.logging.ListLogHandler;

/**
 * TODO: bug free code for selectSession (see cobertura)
 * 
 * @author ste
 */
public class BugFreeHttpSessionServiceLog extends BugFreeHttpSessionServiceBase {
    
    @BeforeClass
    public static void before_class() throws Exception {
        //
        // Logger.getLogger() returns the same instance to multiple threads
        // therefore each method must add its own handler; we clean up the 
        // handlers before starting.
        //
        for (Handler h: LOG.getHandlers()) {
            LOG.removeHandler(h);
        }
    }
    
    
    //
    // Logging
    //
        
    @Test
    public void log_at_INFO_level() throws Exception {
        final ListLogHandler h = configure();
        
        HttpSessionContext context = new HttpSessionContext();
        context.setAttribute(HttpCoreContext.HTTP_CONNECTION, getConnection());
        
        service.doService(TEST_REQUEST1, TEST_RESPONSE1, context);
        
        /**
         * A more generic spec may be required here. let's keep it simple 
         * for now
         */
        List<LogRecord> records = h.getRecords();
        then(records).hasSize(1);
        then(records.get(0).getLevel()).isEqualTo(Level.INFO);
        then(records.get(0).getMessage()).has(
            new MessageCondition(TEST_REQUEST1, TEST_RESPONSE1, context)
        );
    }
    
    @Test
    public void do_not_log_at_INFO_if_level_greater_than_info() throws Exception {
        final ListLogHandler h = configure();
        LOG.setLevel(Level.SEVERE);
        
        HttpSessionContext context = new HttpSessionContext();
        context.setAttribute(HttpCoreContext.HTTP_CONNECTION, getConnection());
        service.doService(TEST_REQUEST1, TEST_RESPONSE1, context);
        
        then(h.getRecords()).isEmpty();
    }
    
    @Test
    public void log_remote_address() throws Exception {
        ListLogHandler h = configure();
        
        TestConnection connection = getConnection();
        
        HttpSessionContext context = new HttpSessionContext();
        context.setAttribute(HttpCoreContext.HTTP_CONNECTION, connection);
        
        service.doService(TEST_REQUEST1, TEST_RESPONSE1, context);
        
        List<LogRecord> records = h.getRecords();
        then(records.get(0).getMessage()).has(
            new MessageCondition(TEST_REQUEST1, TEST_RESPONSE1, context)
        );
        records.clear();
        
        connection.remoteAddress = InetAddress.getByAddress(new byte[] {10, 10, 127, 15});
        service.doService(TEST_REQUEST1, TEST_RESPONSE1, context);
        
        records = h.getRecords();
        then(records.get(0).getMessage()).has(
            new MessageCondition(TEST_REQUEST1, TEST_RESPONSE1, context)
        );
    }
    
    @Test
    public void log_session_id() throws Exception {
        ListLogHandler h = configure();
        
        TestConnection connection = getConnection();
        
        final String[] TEST_SESSION_IDS = {"00001111", "22223333"};
        
        for (String sessionId: TEST_SESSION_IDS) {
            HttpSessionContext context = new HttpSessionContext();
            context.setAttribute(HttpCoreContext.HTTP_CONNECTION, connection);
            
            TEST_REQUEST1.addHeader("JSESSIONID", sessionId);

            service.doService(TEST_REQUEST1, TEST_RESPONSE1, context);
            
            List<LogRecord> records = h.getRecords();
            then(records.get(0).getMessage()).has(
                new MessageCondition(TEST_REQUEST1, TEST_RESPONSE1, context)
            );
            records.clear();
        }
    }
    
    @Test
    public void log_uri() throws Exception {
        ListLogHandler h = configure();
        
        TestConnection connection = getConnection();
        
        final HttpRequest[] TEST_REQUESTS = {TEST_REQUEST1, TEST_REQUEST2};
        
        for (HttpRequest request: TEST_REQUESTS) {
            HttpSessionContext context = new HttpSessionContext();
            context.setAttribute(HttpCoreContext.HTTP_CONNECTION, connection);
            
            service.doService(request, TEST_RESPONSE1, context);
            
            List<LogRecord> records = h.getRecords();
            then(records.get(0).getMessage()).has(
                new MessageCondition(request, TEST_RESPONSE1, context)
            );
            records.clear();
        }
    }
    
    @Test
    public void log_status() throws Exception {
        ListLogHandler h = configure();
        
        TestConnection connection = getConnection();
        
        final Integer[] TEST_STATUSES = {200, 404, 500};
        
        for (Integer status: TEST_STATUSES) {
            HttpSessionContext context = new HttpSessionContext();
            context.setAttribute(HttpCoreContext.HTTP_CONNECTION, connection);
            context.setAttribute("status", status);
            
            service.doService(TEST_REQUEST1, TEST_RESPONSE1, context);
            
            List<LogRecord> records = h.getRecords();
            then(records.get(0).getMessage()).has(
                new MessageCondition(TEST_REQUEST1, TEST_RESPONSE1, context)
            );
            records.clear();
        }
    }
    
    //
    // other functionalities
    //
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
    
    // --------------------------------------------------------- private methods
    
    private ListLogHandler configure() {
        ListLogHandler h = new ListLogHandler();
        LOG.addHandler(h);
        LOG.setLevel(Level.INFO);
        
        return h;
    }
    
    // -------------------------------------------------------- MessageCondition
    
    private class MessageCondition extends Condition<String> {
        private HttpRequest request;
        private HttpResponse response;
        private HttpSessionContext context;
        
        public MessageCondition(
            HttpRequest request, HttpResponse response, HttpSessionContext context
        ) {
            this.request    = request;
            this.response   = response;
            this.context = context;
        }
        
        @Override
        public boolean matches(String t) {
            TestConnection connection = (TestConnection)context.getAttribute(HttpCoreContext.HTTP_CONNECTION);
            
            return String.format(
                HttpSessionService.LOG_PATTERN,
                connection.remoteAddress.toString().substring(1),
                context.getSession().getId(),
                request.getRequestLine().toString(),
                response.getStatusLine().getStatusCode()
            ).equals(t);
        }
        
    }
}
