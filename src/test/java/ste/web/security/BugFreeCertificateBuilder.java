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
package ste.web.security;

import java.security.KeyPair;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Test;
import static ste.xtest.Constants.BLANKS;

/**
 * TODO: to be replaced with the one in ste.https
 * @author ste
 */
public class BugFreeCertificateBuilder {
    
    private final String   DN1 = "cn=localhost";
    private final String   DN2 = "dn=localhost,o=servrone";
    private final   Date TODAY = new Date();
    
    private final Calendar CAL = Calendar.getInstance();
    
    @Test
    public void create_buidler_with_dn_ok() {
        then(new CertificateBuilder(DN1).getDN()).isEqualTo(DN1);
        then(new CertificateBuilder(DN2).getDN()).isEqualTo(DN2);
                
        //
        // by default:
        // - the certificate is valid 3 years
        // - keypair is null 
        //
        CertificateBuilder c = new CertificateBuilder(DN1);
        then(c.getValidFrom()).isAfterOrEqualsTo(TODAY);
        CAL.setTime(c.getValidFrom());
        CAL.add(Calendar.YEAR, 2);
        then(c.getValidTo()).isEqualTo(CAL.getTime());
        then(c.getKeyPair()).isNull();
    }
    
    @Test
    public void create_certificate_with_dn_ko() {
        for (String BLANK: BLANKS) {
            try {
                new CertificateBuilder(BLANK);
                fail("mising check for empty arguments");
            } catch (IllegalArgumentException x) {
                then(x).hasMessageContaining("dn").hasMessageContaining("can not be blank");
            }
        }
    }
    
    @Test
    public void generate_key_pair() {
        CertificateBuilder builder = new CertificateBuilder(DN1);
        then(builder.generateKeyPair()).isNotNull().isSameAs(builder);
        KeyPair keys = builder.getKeyPair();
        then(keys).isNotNull();
        
        //
        // generateKeyPair shall generate another pair
        //
        builder.generateKeyPair(); 
        then(builder.getKeyPair()).isNotSameAs(keys);
    }
    
    @Test 
    public void generate_certificate() throws CertificateExpiredException, CertificateNotYetValidException {
        CertificateBuilder builder = new CertificateBuilder(DN1);
        X509Certificate certificate = builder.build();
        then(certificate).isNotNull();
        then(certificate.getIssuerDN().getName()).isEqualToIgnoringCase(DN1);
        then(certificate.getSigAlgName()).isEqualTo("SHA1withRSA");
        then(certificate.getType()).isEqualTo("X.509");
        then(certificate.getVersion()).isEqualTo(3);
        then(certificate.getNotBefore()).isEqualTo(builder.getValidFrom());
        then(certificate.getNotAfter()).isEqualTo(builder.getValidTo());
        
        certificate.checkValidity();
    }
}
