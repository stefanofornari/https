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
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import ste.web.security.CertificateBuilder;
import ste.web.http.HttpServer;

/**
 * Tools to manage the keystore used by https
 *
 * Syntax: IndexExplorerCLI
 */
public class KeystoreCLI {

    // --------------------------------------------------------------- Constants

    static public final String  COMMAND_HELP = "help";
    static public final String  COMMAND_INIT = "init";
    
    static public final String   OPTION_KEYSTORE = "keystore";
    static public final String   OPTION_PASSWORD = "password";
    
    static private final OptionParser parser = new OptionParser();
    
    static {
        parser.accepts(OPTION_KEYSTORE, "keystore pathname").withRequiredArg().defaultsTo("keystore").describedAs("pathname");
        parser.accepts(OPTION_PASSWORD, "keystore password").withRequiredArg().defaultsTo("").describedAs("string");
    }

    // -------------------------------------------------------------------- Main

    /**
     * Main
     *
     * @param args
     */
    public static void main(String[] args) throws Exception {
        System.out.println("welcome to keystore command line tool\n");
        
        if (args.length == 0) {
            usage();
            return;
        }
        
        if (COMMAND_HELP.equalsIgnoreCase(args[0])) {
            usage();
            
            System.out.println();
            return;
        }
        
        if (COMMAND_INIT.equalsIgnoreCase(args[0])) {
            OptionSet options = null;
            try {
                String[] notCommandArgs = new String[args.length-1];
                System.arraycopy(args, 1, notCommandArgs, 0, notCommandArgs.length);
                
                options = parser.parse(notCommandArgs);
                
                File keystoreFile = new File((String)options.valueOf(OPTION_KEYSTORE)).getAbsoluteFile();
                
                if (keystoreFile.exists()) {
                    System.out.println("keystore " + keystoreFile.getAbsolutePath() + " already exists");
                    System.out.println("do you want to overwrite it (y/N)?");
                    
                    if ((System.in.read() != 'y') ||  (System.in.read() != '\n')) {
                        System.out.println("keystore not initialized");
                        return;
                    }
                }
                
                try (FileOutputStream os = new FileOutputStream(keystoreFile)) {
                    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                    ks.load(null, null);
                    
                    CertificateBuilder builder = new CertificateBuilder("cn=localhost");
                    X509Certificate certificate = builder.build();
                    
                    String password = (String)options.valueOf(OPTION_PASSWORD);
                    
                    ks.setKeyEntry(
                            HttpServer.CERT_ALIAS,
                            builder.getKeyPair().getPrivate(),
                            password.toCharArray(),
                            new Certificate[]{ certificate }
                    );
                    
                    ks.store(os, password.toCharArray());
                    
                    System.out.println(String.format(
                        "keystore %s initialized with %s",
                         keystoreFile.getAbsolutePath(),
                         ("".equals(options.valueOf(OPTION_PASSWORD))) ? "no password" : ("password " + password)
                    ));
                }
            } catch(OptionException x) {
                System.out.println("ups! " + x.getMessage().toLowerCase());
                usage();
            }
            return;
        }
        
        System.out.println("ups! invalid command " + args[0]);
        usage();
    }

    // --------------------------------------------------------- private methods
    public static void usage() {
        System.out.println("usage:\n  " + KeystoreCLI.class.getName() + " commands options\n");
        System.out.println("Command  Description\n-------  -----------\n");
        System.out.println("help     show keystore command line tool usage");
        System.out.println("init     initialize a new keystore with a new keypair");
        System.out.println("");
        try {
            parser.printHelpOn(System.out);
        } catch (IOException x) {
            //
            // nothing to do...
            //
        }
    }
}
