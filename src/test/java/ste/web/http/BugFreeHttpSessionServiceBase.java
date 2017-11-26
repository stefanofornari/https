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
import java.security.Principal;
import java.util.logging.Logger;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpException;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;
import org.junit.Before;
import static ste.web.http.Constants.CONFIG_HTTPS_SESSION_LIFETIME;
import static ste.web.http.HttpServer.LOG_ACCESS;

/**
 * TODO: bug free code for selectSession (see cobertura)
 * 
 * @author ste
 */
public class BugFreeHttpSessionServiceBase {
    protected static final Logger LOG = Logger.getLogger(LOG_ACCESS);
    
    protected HttpSessionService service = null;
        
    protected static final BasicHttpResponse TEST_RESPONSE1 = 
        HttpUtils.getBasicResponse();
    
    protected static final BasicHttpRequest TEST_REQUEST1 = new BasicHttpRequest(
        "GET", "/something/something1.html", HttpVersion.HTTP_1_1
    );
    
    protected static final BasicHttpRequest TEST_REQUEST2 = new BasicHttpRequest(
        "POST", "/something?param1=one&param2=two", HttpVersion.HTTP_1_1
    );
    
    @Before
    public void before() throws Exception {
        HttpProcessor proc = HttpProcessorBuilder.create()
                            .add(new ResponseDate())
                            .add(new ResponseServer())
                            .add(new ResponseContent())
                            .add(new ResponseConnControl()).build();
        UriHttpRequestHandlerMapper handlers = new UriHttpRequestHandlerMapper();
        handlers.register("*", new TestHandler() );
        
        Configuration c = new PropertiesConfiguration();
        c.addProperty(CONFIG_HTTPS_SESSION_LIFETIME, 15*60*1000);
        
        service = new HttpSessionService(proc, handlers, new ConfigurationSessionFactory(c));
    }
    
    // ------------------------------------------------------- protected methods

    protected TestConnection getConnection(int b1, int b2, int b3, int b4) throws Exception {
        TestConnection connection = new TestConnection();
        connection.remoteAddress = InetAddress.getByAddress(new byte[] {(byte)b1, (byte)b2, (byte)b3, (byte)b4});
        
        return connection;
    }
    
    protected TestConnection getConnection() throws Exception  {
        return getConnection(127, 0, 0, 1);
    }
    
    // ---------------------------------------------------------- TestConnection
    
    protected class TestConnection implements HttpInetConnection {
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
    

    protected class TestHandler implements HttpRequestHandler {
        private Principal principal;

        @Override
        public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
            Integer status = (Integer)context.getAttribute("status");
            if (status != null) {
                response.setStatusCode(status);
            }
            principal = ((HttpSessionContext)context).getPrincipal();
        }
        
        public Principal getPrincipal() {
            return principal;
        }
    }
}
