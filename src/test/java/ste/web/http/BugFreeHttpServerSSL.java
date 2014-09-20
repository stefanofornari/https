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
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ClearSystemProperties;
import org.junit.rules.TemporaryFolder;
import static ste.web.http.BugFreeHttpServerBase.SSL_PASSWORD;
import static ste.web.http.Constants.CONFIG_HTTPS_AUTH;
import static ste.web.http.Constants.CONFIG_HTTPS_PORT;
import static ste.web.http.Constants.CONFIG_HTTPS_ROOT;
import static ste.web.http.Constants.CONFIG_SSL_PASSWORD;
import ste.web.http.handlers.FileHandler;
import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateIssuerName;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateSubjectName;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

/**
 *
 * @author ste
 */
public class BugFreeHttpServerSSL {

    private File root = null;

    @Rule
    public final ClearSystemProperties SYSTEM_SSL_PASSWORD
            = new ClearSystemProperties(CONFIG_SSL_PASSWORD);

    @Rule
    public final TemporaryFolder FOLDER = new TemporaryFolder();

    @Before
    public void setUp() {
        this.root = FOLDER.getRoot();
        new File(root, "etc").mkdir();
    }

    @Test
    /**
     * We read the password from the SystemProperty ste.web.http.ssl.password
     * and we use one password for all types of stores/keys
     */
    public void keyStorePasswordOK() throws Exception {
        String s = String.valueOf(System.currentTimeMillis());
        createKeyStore(String.valueOf(s).toCharArray(), HttpServer.CERT_ALIAS);

        HttpServer server = createServer(s);
        try {
            server.start();
            then(server.isRunning()).isTrue();
        } finally {
            if (server != null) {
                server.stop();
            }
        }

        s = String.valueOf(System.currentTimeMillis());
        createKeyStore(String.valueOf(s).toCharArray(), HttpServer.CERT_ALIAS);

        try {
            server = createServer(s);

            server.start();
            then(server.isRunning()).isTrue();
        } finally {
            if (server != null) {
                server.stop();
            }
        }
    }

    @Test
    public void keyStorePasswordWrongPasswordKO() throws Exception {
        String s = String.valueOf(System.currentTimeMillis());
        createKeyStore(String.valueOf(s).toCharArray());
        try {
            createServer("none");
            fail("if SSL is not correctly setup server instantiation shall fail");
        } catch (ConfigurationException x) {
            then(x.getMessage()).contains("password").contains("incorrect");
        }

    }

    @Test
    public void keyStorePasswordMissingPasswordKO() throws Exception {
        String s = String.valueOf(System.currentTimeMillis());
        createKeyStore(String.valueOf(s).toCharArray());

        try {
            createServer(null);
            fail("if SSL is not correctly setup server instantiation shall fail");
        } catch (ConfigurationException x) {
            then(x.getMessage()).contains("password not provided").contains(CONFIG_SSL_PASSWORD);
        }

    }

    @Test
    public void missingKeystoreKO() throws Exception {
        String s = String.valueOf(System.currentTimeMillis());
        try {
            createServer(s);
            fail("if SSL is not correctly setup server instantiation shall fail");
        } catch (ConfigurationException x) {
            then(x.getMessage()).contains("No such file or directory").contains("etc/keystore");
        }
    }

    @Test
    public void missingServerCertificateKO() throws Exception {
        String s = String.valueOf(System.currentTimeMillis());
        createKeyStore(String.valueOf(s).toCharArray());

        try {
            createServer(s);
            fail("if SSL is not correctly setup server instantiation shall fail");
        } catch (ConfigurationException x) {
            then(x.getMessage())
                .contains("missing server certificate")
                .contains("etc/keystore")
                .contains(HttpServer.CERT_ALIAS);
        }
    }

    // --------------------------------------------------------- private methods
    
    private HttpServer createServer(String password) throws Exception {
        UriHttpRequestHandlerMapper handlers = new UriHttpRequestHandlerMapper();
        handlers.register("*", new FileHandler(root.getPath()));
        
        PropertiesConfiguration configuration= new PropertiesConfiguration();
        configuration.setProperty(CONFIG_HTTPS_ROOT, root.getAbsolutePath());
        configuration.setProperty(CONFIG_HTTPS_PORT, "8000");
        configuration.setProperty(CONFIG_HTTPS_AUTH, "none");
        if (password != null) {
            configuration.setProperty(CONFIG_SSL_PASSWORD, password);
        }

        HttpServer server = new HttpServer(configuration);
        server.setHandlers(handlers);

        return server;
    }

    private void createKeyStore(char[] password, String alias) throws Exception {
        FileOutputStream os = new FileOutputStream(root.getAbsolutePath() + "/etc/keystore");
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, password);
        
        if (alias != null) {
            KeyPair key = createNewKeyPair();

            X509Certificate localhost = generateCertificate(
                "cn=localhost",
                key,
                365*10
            );

            ks.setKeyEntry(
                HttpServer.CERT_ALIAS,
                key.getPrivate(), 
                password,
                new Certificate[]{localhost}
            );
        }
        
        ks.store(os, password);
        os.close();
    }
    
    private void createKeyStore(char[] password) throws Exception {
        createKeyStore(password, null);
    }
    
    private KeyPair createNewKeyPair() throws Exception {
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("DSA");
        keyGenerator.initialize(1024, SecureRandom.getInstance("SHA1PRNG", "SUN"));
        
        return keyGenerator.generateKeyPair();
    }

    private X509Certificate generateCertificate(String dn, KeyPair pair, int days)
            throws GeneralSecurityException, IOException {
        PrivateKey privkey = pair.getPrivate();
        X509CertInfo info = new X509CertInfo();
        Date from = new Date();
        Date to = new Date(from.getTime() + days * 86400000l);
        CertificateValidity interval = new CertificateValidity(from, to);
        BigInteger sn = new BigInteger(64, new SecureRandom());
        X500Name owner = new X500Name(dn);

        info.set(X509CertInfo.VALIDITY, interval);
        info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn));
        info.set(X509CertInfo.SUBJECT, owner);
        info.set(X509CertInfo.ISSUER, owner);
        info.set(X509CertInfo.KEY, new CertificateX509Key(pair.getPublic()));
        info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
        AlgorithmId algo = new AlgorithmId(AlgorithmId.md5WithRSAEncryption_oid);
        info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));

        // Sign the cert to identify the algorithm that's used.
        X509CertImpl cert = new X509CertImpl(info);
        cert.sign(privkey, "SHA1WithDSA");

        // Update the algorith, and resign.
        algo = (AlgorithmId) cert.get(X509CertImpl.SIG_ALG);
        info.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, algo);
        cert = new X509CertImpl(info);
        cert.sign(privkey, "SHA1WithDSA");
        
        return cert;
    }
}
