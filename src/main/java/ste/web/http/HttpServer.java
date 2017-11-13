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
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.HttpServerConnection;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.X509KeyManager;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConversionException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerMapper;
import static ste.web.http.Constants.*;
import ste.web.http.handlers.RestrictedResourceHandler;

/**
 * An HTTPS server
 *
 */
public class HttpServer {

    public static final String LOG_ACCESS = "ste.https.access";
    public static final String LOG_SERVER = "ste.https.server";
    public static final String CERT_ALIAS = "ste.https";
    
    final Logger LOG = Logger.getLogger(LOG_SERVER);

    public static enum ClientAuthentication {
        BASIC, NONE, CERTIFICATE
    };

    private SSLServerSocketFactory sf;
    private int sslPort, webPort;
    private HttpSessionService ssl, web;
    private boolean running;
    private RequestListenerThread listenerThread, webListenerThread;
    private ClientAuthentication authentication;
    private Configuration configuration;
    
    UriHttpRequestHandlerMapper sslMapper;
    UriHttpRequestHandlerMapper webMapper;

    /**
     * Creates a HTTPS server given a configuration object. The following 
     * configuration properties must be set: CONFIG_HTTPS_HOME, CONFIG_HTTPS_PORT,
     * CONFIG_SSL_PASSWORD.
     * 
     * The following properties are optional: CONFIG_HTTPS_AUTH
     *  (default to none), CONFIG_HTTPS_WEBROOT (default to webroot);
     *
     * @param configuration the configuration object - NOT NULL
     *
     * @throws ConfigurationException if any of the mandatory parameter is missing
     *         or invalid
     */
    public HttpServer(Configuration configuration) throws ConfigurationException {
        if (configuration == null) {
            throw new IllegalArgumentException("configuration can not be null");
        }
        
        configure(configuration);
    }
    
    /**
     * Creates a HTTPS server given a configuration file (properties) where to
     * read the configuration from.
     * 
     * @param configurationFilename the configuration file - NOT EMPTY
     * 
     * @throws ConfigurationException 
     */
    public HttpServer(String configurationFilename) throws ConfigurationException {
        if (StringUtils.isBlank(configurationFilename)) {
            throw new IllegalArgumentException("configuration can not be empty");
        }
        
        File configurationFile = new File(configurationFilename);
        
        if (!configurationFile.exists()) {
            throw new ConfigurationException("configuration file " + configurationFile.getAbsolutePath() + " not found");
        }
        
        configure(new PropertiesConfiguration(configurationFilename));
    }
    
    public void start() {
        SSLServerSocket sslSocket = null;
        ServerSocket webSocket = null;
        try {
            sslSocket = (SSLServerSocket) sf.createServerSocket(sslPort);
        } catch (IOException x) {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.info(String.format("unable to start the server because it was not possible to bind port %d (%s)",
                        sslPort,
                        x.getMessage().toLowerCase()
                    )
                );
            }
            
            return;
        }
        sslSocket.setNeedClientAuth(authentication == ClientAuthentication.CERTIFICATE);
        
        try {
            webSocket = new ServerSocket(webPort);
        } catch (IOException x) {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.info(
                    String.format(
                        "unable to start the server because it was not possible to bind port %d (%s)",
                        webPort,
                        x.getMessage().toLowerCase()
                    )
                );
            }
            
            return;
        }
        running = true;
             
        listenerThread = new RequestListenerThread(this, sslSocket);
        listenerThread.setDaemon(true);
        listenerThread.start();
        
        webListenerThread = new RequestListenerThread(this, webSocket);
        webListenerThread.setDaemon(true);
        webListenerThread.start();      
    }

    public void stop() {
        running = false;
        if (listenerThread != null) {
            listenerThread.interrupt();
            try {
                listenerThread.join(1000);
            } catch (InterruptedException x) {
                throw new RuntimeException(x);
            }
        }
        if (webListenerThread != null) {
            webListenerThread.interrupt();
            try {
                webListenerThread.join(1000);
            } catch (InterruptedException x) {
                throw new RuntimeException(x);
            }
        }
    }

    public boolean isRunning() {
        return running;
    }

    public HttpSessionService getSSLService() {
        return ssl;
    }
    
    public HttpSessionService getWebService() {
        return web;
    }

    public int getSSLPort() {
        return sslPort;
    }
    
    public int getWebPort() {
        return webPort;
    }
    
    public ClientAuthentication getAuthentication() {
        return authentication;
    }

    /**
     * If processor is null a default implementation is provided as follows:
     * 
     * <code>
     * HttpProcessorBuilder.create()
     *          .add(new ResponseDate())
     *          .add(new ResponseServer())
     *          .add(new ResponseContent())
     *          .add(new ResponseConnControl()).build()
     * </code>
     * 
     * @param handlers the new handlers; if null, no handlers will be set - MAY BE NULL
     * @param processor the HttpProcessor to use in the HttpSessionProcessors - MAY BE NULL
     */
    public void setHandlers(
        final HashMap<String, HttpRequestHandler> handlers,
        HttpProcessor processor
    ) {
        long sessionLifetime = configuration.getLong(CONFIG_HTTPS_SESSION_LIFETIME, 15*60*1000);
        
        // Set up the HTTP protocol processor
        if (processor == null) {
            processor = buildDefaultHttpProcessor();
        }
        
        // Set up request handlers end HTTP service
        if (handlers != null) {
            sslMapper = new UriHttpRequestHandlerMapper();
            webMapper = new UriHttpRequestHandlerMapper();
            
            for (String pattern: handlers.keySet()) {
                HttpRequestHandler handler = handlers.get(pattern);
                
                sslMapper.register(pattern, handler);
                if (!(handler instanceof RestrictedResourceHandler)) {
                    webMapper.register(pattern, handler); 
                }
            }
            
            ssl = new HttpSessionService(processor, sslMapper, sessionLifetime);
            web = new HttpSessionService(processor, webMapper, sessionLifetime);
        } else {
            UriHttpRequestHandlerMapper registry = new UriHttpRequestHandlerMapper();
            ssl = new HttpSessionService(processor, registry, sessionLifetime);
            web = new HttpSessionService(processor, registry, sessionLifetime);
        }
    }
    
    /**
     * Equivalent to <code>setHandlers(handlers, null)</code>.
     * 
     * @param handlers the new handlers; if null, no handlers will be set - MAY BE NULL
     */
    public void setHandlers(HashMap<String, HttpRequestHandler> handlers) {
        setHandlers(handlers, null);
    }
    
    public HttpRequestHandlerMapper getWebHandlers() {
        return this.webMapper;
    }
    
    public HttpRequestHandlerMapper getSSLHandlers() {
        return this.sslMapper;
    }
    
    public Configuration getConfiguration() {
        return configuration;
    }
    
    // --------------------------------------------------------- private methods
    
    private void configure(Configuration c) throws ConfigurationException {
        this.configuration = c;
        
        String home = configuration.getString(CONFIG_HTTPS_ROOT);
        if (StringUtils.isBlank(home)) {
            throw new ConfigurationException(
                "the server home directory is unset or blank; please specify a proper value for the property " + CONFIG_HTTPS_ROOT
            );
        }
        File fileHome = new File(home);
        if (!fileHome.exists() || !fileHome.isDirectory()) {
            throw new ConfigurationException (
                    String.format("the given home [%s] must exist and must be a directory", home)
            );
        }

        sslPort = configPort("ssl");
        webPort = configPort("web");
        
        try {
            String password = configuration.getString(CONFIG_SSL_PASSWORD);
            sf = getSSLContext(home, password).getServerSocketFactory();
        } catch (Exception x) {
            throw new ConfigurationException(x.getMessage(), x);
        }
        
        String auth = configuration.getString(CONFIG_HTTPS_AUTH);
        authentication = ClientAuthentication.BASIC;
        
        if ("none".equalsIgnoreCase(auth)) {
            authentication = ClientAuthentication.NONE;
        } else if ("cert".equalsIgnoreCase(auth)) {
            authentication = ClientAuthentication.CERTIFICATE;
        }
        
        this.running = false;
        this.listenerThread = null;
        this.webListenerThread = null;
        
        setHandlers(null);
    }
   
    private SSLContext getSSLContext(final String home, final String password)
            throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, KeyManagementException {
        if (StringUtils.isBlank(password)) {
            throw new UnrecoverableKeyException("ssl password not provided; set the system propoerty " + CONFIG_SSL_PASSWORD);
        }

        //
        // SSL Setup
        //
        char[] sslPassword = password.toCharArray();

        //
        // TODO: handle the case the client does not send the certificate
        //
        String keystoreFile = home + "/conf/keystore";
        KeyStore keystore = KeyStore.getInstance("jks");
        keystore.load(new FileInputStream(keystoreFile), sslPassword);
        
        //
        // check that there is a certificate with alias ste.https; this is the 
        // server certificate. If such certificate is not available provide 
        // proper message and description.
        //
        Certificate serverCertificate = keystore.getCertificate(CERT_ALIAS);
        if (serverCertificate == null) {
            throw new CertificateException(String.format(
                    "missing server certificate with alias '%s' in keystore %s",
                    CERT_ALIAS, keystoreFile
            ));
        }

        KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm()
        );
        kmfactory.init(keystore, sslPassword);
        X509KeyManager x509KeyManager = 
            (X509KeyManager)kmfactory.getKeyManagers()[0];
        
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(
                new KeyManager[] {new HttpKeyManager(x509KeyManager)}, 
                null, null
        );

        return context;
    }

    private HttpProcessor buildDefaultHttpProcessor() {
        return HttpProcessorBuilder.create()
                .add(new ResponseDate())
                .add(new ResponseServer())
                .add(new ResponseContent())
                .add(new ResponseConnControl()).build();
    }
    
    private int configPort(final String whichPort) throws ConfigurationException {
        final String KEY = "web".equals(whichPort) 
                         ? CONFIG_HTTPS_WEB_PORT
                         : CONFIG_HTTPS_PORT
                         ;
        int p = 0;
        try {
            p = configuration.getInt(KEY);
        } catch (NoSuchElementException x) {
            throw new ConfigurationException(
                "the " + whichPort + " port is unset; please specify a proper value for the property " + KEY
            );
        } catch (ConversionException x) {
            throw new ConfigurationException(
                "the " + whichPort + " port <" + 
                configuration.getProperty(KEY) + 
                "> is invalid; please specify a proper value for the property " + 
                KEY
            );
        }
        if (p <= 0) {
            throw new ConfigurationException(
                "the " + whichPort + " port <" +
                configuration.getProperty(KEY) + 
                "> is invalid; please specify a value between 1 and " 
                + Integer.MAX_VALUE + 
                " for the property " +
                KEY
            );
        }
        
        return p;
    }

    // --------------------------------------------------- RequestListenerThread
    
    static class RequestListenerThread extends Thread {
        private final ServerSocket serverSocket;
        private final HttpServer server;
        private final boolean isSSL;

        public RequestListenerThread(final HttpServer server, final ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
            this.server = server;
            this.isSSL = (this.serverSocket.getLocalPort() == server.sslPort);
        }

        @Override
        public void run() {
            Logger LOG = Logger.getLogger(LOG_SERVER);
            
            while (server.isRunning() && !Thread.interrupted()) {
                Socket socket = null;
                HttpServerConnection conn = null;
                try {
                    if (LOG.isLoggable(Level.INFO)) {
                        LOG.info(String.format("starting %s listener on port %d",
                                isSSL ? "ssl" : "web",
                                this.serverSocket.getLocalPort()
                            )
                        );
                    }
                    socket = this.serverSocket.accept();
                } catch (IOException x) {
                    if (LOG.isLoggable(Level.INFO)) {
                        LOG.info(
                            String.format(
                                "stopping to listen on port %d (%s)",
                                this.serverSocket.getLocalPort(),
                                x.getMessage()
                            )
                        );
                    }
                    break;
                }
                try {
                    conn = BasicHttpConnectionFactory.INSTANCE.createConnection(socket);
                } catch (IOException x) {
                    if (LOG.isLoggable(Level.INFO)) {
                        LOG.info(
                            String.format(
                            "stopping to create connections on port %d (%s)",
                            this.serverSocket.getLocalPort(),
                            x.getMessage()
                            )
                        );   
                    }
                    break;
                }

                // Start worker thread
                Thread t = new WorkerThread(
                    isSSL ? server.getSSLService() : server.getWebService(), 
                    conn
                );
                t.setDaemon(true);
                t.start();
            }
        }

        public void interrupt() {
            if (this.serverSocket != null) {
                try {
                    this.serverSocket.close();
                } catch (IOException x) {
                    //
                    // ignore
                    //
                    x.printStackTrace();
                }
            }
        }
    }

    // ------------------------------------------------------------ WorkerThread
    
    static class WorkerThread extends Thread {

        private final HttpSessionService http;
        private final HttpServerConnection conn;

        public WorkerThread(
                final HttpSessionService http,
                final HttpServerConnection conn) {
            super();
            this.http = http;
            this.conn = conn;
        }

        @Override
        public void run() {
            Logger LOG = Logger.getLogger(LOG_SERVER);
            try {
                if (!Thread.interrupted() && this.conn.isOpen()) {
                    this.http.handleRequest(this.conn);
                }
            } catch (ConnectionClosedException x) {
                LOG.fine(String.format("connection closed by the client (%s)", x.getMessage()));
            } catch (IOException x) {
                LOG.fine(String.format("io error (%s)", x.getMessage()));
            } catch (HttpException x) {
                LOG.fine(String.format("http error (%s)", x.getMessage()));
            } finally {
                try {
                    this.conn.shutdown();
                } catch (IOException ignore) {
                }
            }
        }

    }

    // ---------------------------------------------------------- HttpKeyManager
    
    private class HttpKeyManager implements X509KeyManager {

        private X509KeyManager defaultKeyManager;

        public HttpKeyManager(X509KeyManager defaultKeyManager) {
            this.defaultKeyManager = defaultKeyManager;
        }

        @Override
        public String[] getClientAliases(String keyType, Principal[] issuers) {
            return defaultKeyManager.getClientAliases(keyType, issuers);
        }

        @Override
        public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
            return defaultKeyManager.chooseClientAlias(keyType, issuers, socket);
        }

        @Override
        public String[] getServerAliases(String keyType, Principal[] issuers) {
            return defaultKeyManager.getServerAliases(keyType, issuers);
        }

        @Override
        public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
            return HttpServer.CERT_ALIAS;
        }

        @Override
        public X509Certificate[] getCertificateChain(String alias) {
            return defaultKeyManager.getCertificateChain(alias);
        }

        @Override
        public PrivateKey getPrivateKey(String alias) {
            return defaultKeyManager.getPrivateKey(alias);
        }

    }
    
}
