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

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpException;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;
import static org.assertj.core.api.BDDAssertions.then;
import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static ste.web.http.HttpServer.LOG_ACCESS;
import ste.xtest.logging.ListLogHandler;

/**
 * TODO: bug free code for selectSession (see cobertura)
 * 
 * @author ste
 */
public class BugFreeHttpSessionService {
    private static final Logger LOG = Logger.getLogger(LOG_ACCESS);
    
    private HttpSessionService service = null;
    
    private static final BasicHttpRequest TEST_REQUEST1 = new BasicHttpRequest(
        "GET", "/something/something1.html", HttpVersion.HTTP_1_1
    );
    
    private static final BasicHttpRequest TEST_REQUEST2 = new BasicHttpRequest(
        "POST", "/something?param1=one&param2=two", HttpVersion.HTTP_1_1
    );
    
    private static final BasicHttpResponse TEST_RESPONSE1 = 
        HttpUtils.getBasicResponse();
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        //
        // Logger.getLogger() returns the same instance to multiple threads
        // therefore each method must add its own handler; we clean up the 
        // handlers before starting.
        //
        for (Handler h: LOG.getHandlers()) {
            LOG.removeHandler(h);
        }
    }
    
    @Before
    public void setUp() throws Exception {
        HttpProcessor proc = HttpProcessorBuilder.create()
                            .add(new ResponseDate())
                            .add(new ResponseServer())
                            .add(new ResponseContent())
                            .add(new ResponseConnControl()).build();
        UriHttpRequestHandlerMapper handlers = new UriHttpRequestHandlerMapper();
        handlers.register("*", new HttpRequestHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
                Integer status = (Integer)context.getAttribute("status");
                if (status != null) {
                    response.setStatusCode(status);
                }
            }
        });
        
        service = new HttpSessionService(proc, handlers, 15*60*1000);
    }
    
    //
    // Logging
    //
        
    @Test
    public void logAtINFOLevel() throws Exception {
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
    public void doNotLogAtInfoIfLevelGreaterThenInfo() throws Exception {
        final ListLogHandler h = configure();
        LOG.setLevel(Level.SEVERE);
        
        HttpSessionContext context = new HttpSessionContext();
        context.setAttribute(HttpCoreContext.HTTP_CONNECTION, getConnection());
        service.doService(TEST_REQUEST1, TEST_RESPONSE1, context);
        
        then(h.getRecords()).isEmpty();
    }
    
    @Test
    public void logRemoteAddress() throws Exception {
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
    public void logSessionId() throws Exception {
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
    public void logUri() throws Exception {
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
    public void logStatus() throws Exception {
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
    public void basicEntityShallHaveEmptyContentByDefault() throws Exception {
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
    
    private TestConnection getConnection(int b1, int b2, int b3, int b4) throws Exception {
        TestConnection connection = new TestConnection();
        connection.remoteAddress = InetAddress.getByAddress(new byte[] {(byte)b1, (byte)b2, (byte)b3, (byte)b4});
        
        return connection;
    }
    
    private TestConnection getConnection() throws Exception  {
        return getConnection(127, 0, 0, 1);
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
    
    // ---------------------------------------------------------- TestConnection
    
    private class TestConnection implements HttpInetConnection {
        public InetAddress localAddress = null, remoteAddress = null;
        public int localPort = -1, remotePort = -1;
        public int timeout = -1;
        public boolean closed = false;
        

        @Override
        public InetAddress getLocalAddress() {
            return localAddress;
        }

        @Override
        public int getLocalPort() {
            return localPort;
        }

        @Override
        public InetAddress getRemoteAddress() {
            return remoteAddress;
        }

        @Override
        public int getRemotePort() {
            return remotePort;
        }

        @Override
        public void close() throws IOException {
            closed = true;
        }

        @Override
        public boolean isOpen() {
            return !closed;
        }

        @Override
        public boolean isStale() {
            return false;
        }

        @Override
        public void setSocketTimeout(int timeout) {
            this.timeout = timeout;
        }

        @Override
        public int getSocketTimeout() {
            return timeout;
        }

        @Override
        public void shutdown() throws IOException {
        }

        @Override
        public HttpConnectionMetrics getMetrics() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
    
    
}
