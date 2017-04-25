/*
 * Copyright (C) 2015 Stefano Fornari.
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
package ste.web.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ClearSystemProperties;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.TemporaryFolder;
import ste.web.http.HttpServer;
import ste.xtest.cli.BugFreeCLI;


/**
 *
 * @author ste
 * 
 */
public class BugFreeKeystoreCLI extends BugFreeCLI {
    
    @Rule
    public final TemporaryFolder TESTDIR = new TemporaryFolder();
    
    @Rule
    public final ClearSystemProperties USER_DIR = new ClearSystemProperties("user.dir");
    
    @Rule
    public final ClearSystemProperties USER_HOME = new ClearSystemProperties("user.home");
    
    @Before
    public void before() {
        System.setProperty("user.dir", TESTDIR.getRoot().getAbsolutePath());
        System.setProperty("user.home", TESTDIR.getRoot().getAbsolutePath());
    }
        
    @Test
    public void main_with_no_arguments() throws Exception {
        KeystoreCLI.main(new String[0]);
        
        then(STDOUT.getLog()).contains("welcome to keystore command line tool")
                             .contains("usage:")
                             .contains("ste.web.tools.KeystoreCLI commands options")
                             .contains("help     show keystore command line tool usage");
    }

    @Test
    public void help() throws Exception {
        KeystoreCLI.main(new String[] { "help" });

        String out = STDOUT.getLog();
        then(out).contains("welcome to keystore command line tool")
                .contains("usage:")
                .contains("\nhelp     show keystore command line tool usage")
                .contains("\ninit     initialize a new keystore with a new keypair");
    }

    @Test
    public void show_usage_if_command_not_recognized() throws Exception {
        for (String cmd: new String[] { "-", "abc", " show"}) {
            STDOUT.clearLog();
            
            KeystoreCLI.main(new String[] { cmd });
            then(STDOUT.getLog()).contains("invalid command " + cmd)
                                 .contains("usage:");
        }
    }

    //
    // init
    // ----
    //
    // creates a new keysore with new keypairs with the following characteristics:
    // - una certificate with alias ste.https
    // - keypair generator alghoritm: RSA 1024 bits
    // - validity: 2 years
    //
    @Test
    public void init_new_keystore_without_password_and_default_name() throws Exception {
        KeystoreCLI.main(new String[] {"init"});
        
        File keystoreFile = new File(TESTDIR.getRoot(), "keystore");
        
        then(STDOUT.getLog())
            .contains("keystore " + keystoreFile.getAbsolutePath() + " initialized with no password");
        
        checkKeystore(keystoreFile, null);
    }
    
    @Test
    public void init_new_keystore_with_password_and_no_pathname() throws Exception {
        String[] PASSWORDS = new String[] { "12345", "67890" };
        
        for (String PASSWORD: PASSWORDS) {
            KeystoreCLI.main(new String[] {"init", "--password", PASSWORD});

            File keystoreFile = new File(TESTDIR.getRoot(), "keystore");

            then(STDOUT.getLog())
                .contains("keystore " + keystoreFile.getAbsolutePath() + " initialized with password " + PASSWORD);

            checkKeystore(keystoreFile, PASSWORD);
            
            keystoreFile.delete();
        }
    }
    
    @Test
    public void init_new_keystore_with_password_and_pathname() throws Exception {
        String[] PATHNAMES = new String[] { "ks1", "ks2", "d1/d2/ks3"};
        
        TESTDIR.newFolder("d1", "d2");
        
        for (String PATHNAME: PATHNAMES) {
            KeystoreCLI.main(new String[] {"init", "--keystore", PATHNAME});

            File keystoreFile = new File(TESTDIR.getRoot(), PATHNAME);

            then(STDOUT.getLog())
                .contains("keystore " + keystoreFile.getAbsolutePath() + " initialized with no password");

            checkKeystore(keystoreFile, null);
            
            keystoreFile.delete();
        }
    }
    
    @Test
    public void confirmation_before_overwriting_keystore_no() throws Exception {
        File keystoreFile = TESTDIR.newFile("keystore");
        
        String NOs[] = new String[] {"n\n", "", "\n", "n", "a\n", "yao\n"};
        for(String NO: NOs) {            
            STDOUT.clearLog(); STDIN.provideLines("n\n");
            KeystoreCLI.main(new String[] { "init" });

            then(STDOUT.getLog())
                    .contains("keystore " + keystoreFile.getAbsolutePath() + " already exists\n")
                    .contains("do you want to overwrite it (y/N)?")
                    .contains("keystore not initialized");
            
            then(keystoreFile.length()).isEqualTo(0);
        }
    }
    
    @Test
    public void confirmation_before_overwriting_keystore_yes() throws Exception {
        File keystoreFile = TESTDIR.newFile("keystore");
        
        STDOUT.clearLog(); STDIN.provideText("y\n");
        KeystoreCLI.main(new String[] { "init" });

        then(STDOUT.getLog())
                .contains("keystore " + keystoreFile.getAbsolutePath() + " initialized with no password");
            
        checkKeystore(keystoreFile, null);
    }

    // --------------------------------------------------------- private methods
    
    private void checkKeystore(File keystoreFile, String password) 
        throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        then(keystoreFile).exists();
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(new FileInputStream(keystoreFile), (password == null) ? null : password.toCharArray());
        
        then(ks.containsAlias(HttpServer.CERT_ALIAS)).isTrue();
        X509Certificate cert = (X509Certificate)ks.getCertificate(HttpServer.CERT_ALIAS);
        then(cert.getIssuerDN().getName()).isEqualToIgnoringCase("cn=localhost");
    }
}
