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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.HttpServerConnection;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnectionFactory;
import org.apache.http.protocol.HttpContext;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import org.apache.http.HttpConnectionFactory;
import org.apache.http.protocol.HttpRequestHandlerMapper;

/**
 * ServerOne http server
 * 
 * TODO: requests logging
 */
public class HttpServer {
    
    public static enum ClientAuthentication {
        NONE, CERTIFICATE
    };
    
    private SSLServerSocketFactory sf;
    private int port;
    private HttpService http;
    private boolean running;
    private RequestListenerThread requestListenerThread;
    private ClientAuthentication authentication;

    /**
     * Creates a HTTPS server given the home, the SSL authentiaction, the port
     * and the request handlers to use.
     * 
     * @param home the file system directory that should be considered the root
     *             of the server (it may be different than the current working 
     *             directory - NOT NULL
     * @param authentication the SSL authentication method to use
     * @param port the port the serve shall listen to
     * @param handlers the handlers to be used to process the requests; if null 
     *                 an empty handlers map will be used - MAY BE NULL
     */
    public HttpServer(
        final String home, 
        final ClientAuthentication authentication, 
        final int port,
        final HttpRequestHandlerMapper handlers
    ) {
        if (home == null) {
            throw new IllegalArgumentException("home can not be null");
        }
        if (port <= 0) {
            throw new IllegalArgumentException("port can not be <= 0");
        }
        File fileHome = new File(home);
        if (!fileHome.exists() || !fileHome.isDirectory()) {
            throw new IllegalArgumentException(
                String.format("the given home [%s] must exist and must be a directory", home)
            );
        }
        this.port = port;
        this.running = false;
        this.requestListenerThread = null;
        this.authentication = authentication;

        setHandlers(handlers);
        
        try {
            sf = getSSLContext(home).getServerSocketFactory();
        } catch (Exception x) {
            //
            // TODO: handle the error
            //
            x.printStackTrace();
        }
        
    }
    
    public void start() throws IOException {
        //
        // TODO: to be reviewed
        //
        requestListenerThread = new RequestListenerThread(this);
        requestListenerThread.setDaemon(false);
        requestListenerThread.start();
        running = true;
    }
    
    public void stop() throws IOException {
        running = false;
        if (requestListenerThread != null) {
            requestListenerThread.interrupt();
        }
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public HttpService getHttpService() {
        return http;
    }
    
    public int getPort() {
        return port;
    }
    
    /**
     * TODO: bug free code
     * 
     * @param handlers the new handlers; if null, no handlers will be set - MAYBE NULL
     */
    public void setHandlers(HttpRequestHandlerMapper handlers) {
        // Set up the HTTP protocol processor
        HttpProcessor httpproc = buildHttpProcessor();

        // Set up request handlers end HTTP service
        if (handlers != null) {               
            http = new HttpService(httpproc, handlers);
        } else {
            UriHttpRequestHandlerMapper registry = new UriHttpRequestHandlerMapper();
            http = new HttpService(httpproc, registry);
        }
    }
    
    public ServerSocket createServerSocket() throws IOException {
        try {
            SSLServerSocket socket = (SSLServerSocket)sf.createServerSocket(port);
            socket.setNeedClientAuth(authentication == ClientAuthentication.CERTIFICATE);
            return socket;
        } catch (Exception x) {
            x.printStackTrace();
            throw x;
        }
    }

    static class RequestListenerThread extends Thread {

        private final HttpConnectionFactory<DefaultBHttpServerConnection> connFactory;
        private final ServerSocket serversocket;
        private final HttpServer server;
        
        private Socket socket;

        public RequestListenerThread(final HttpServer server) throws IOException {
            this.connFactory = DefaultBHttpServerConnectionFactory.INSTANCE;
            this.serversocket = server.createServerSocket();
            this.server = server;
        }

        @Override
        public void run() {
            while (server.isRunning() && !Thread.interrupted()) {
                try {
                    // Set up HTTP connection
                    Socket socket = this.serversocket.accept();
                    System.out.println("Incoming connection from " + socket.getInetAddress());
                    HttpServerConnection conn = this.connFactory.createConnection(socket);

                    // Start worker thread
                    Thread t = new WorkerThread(server.getHttpService(), conn);
                    t.setDaemon(true);
                    t.start();
                } catch (InterruptedIOException ex) {
                    System.out.println("STOP listening... ");
                    break;
                } catch (IOException e) {
                    System.out.println("STOP listening... ");
                    System.err.println("I/O error initialising connection thread: "
                            + e.getMessage());
                    
                    break;
                }
            }
        }
        
        public void interrupt() {
            System.out.println("stopping " + this.serversocket);
            if (this.serversocket != null) {
                try {
                    this.serversocket.close();
                } catch (IOException x) {
                    //
                    // ignore
                    //
                    x.printStackTrace();
                }
            }
        }
    }

    static class WorkerThread extends Thread {

        private final HttpService httpservice;
        private final HttpServerConnection conn;

        public WorkerThread(
                final HttpService httpservice,
                final HttpServerConnection conn) {
            super();
            this.httpservice = httpservice;
            this.conn = conn;
        }

        @Override
        public void run() {
            System.out.println("New connection thread");
            HttpContext context = new BasicHttpContext(null);
            try {
                while (!Thread.interrupted() && this.conn.isOpen()) {
                    this.httpservice.handleRequest(this.conn, context);
                }
            } catch (ConnectionClosedException ex) {
                System.err.println("Client closed connection");
            } catch (IOException ex) {
                System.err.println("I/O error: " + ex.getMessage());
            } catch (HttpException ex) {
                System.err.println("Unrecoverable HTTP protocol violation: " + ex.getMessage());
            } finally {
                try {
                    this.conn.shutdown();
                } catch (IOException ignore) {}
            }
        }

    }
    
    private SSLContext getSSLContext(final String home) 
        throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, KeyManagementException {
        //
        // SSL Setup
        //
        //
        // TODO: use a proper certificate alias
        // TODO: handle the case the client does not send the certificate
        //
        KeyStore keystore = KeyStore.getInstance("jks");
        keystore.load(new FileInputStream(home + "/etc/keystore"), "serverone".toCharArray());
        KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm()
        );
        kmfactory.init(keystore, "serverone".toCharArray());
        KeyManager[] keymanagers = kmfactory.getKeyManagers();
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keymanagers, null, null);
        
        return context;
    }
    
    private HttpProcessor buildHttpProcessor() {
        return HttpProcessorBuilder.create()
                .add(new ResponseDate())
                .add(new ResponseServer())
                .add(new ResponseContent())
                .add(new ResponseConnControl()).build();
    }

}