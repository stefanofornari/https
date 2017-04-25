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

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import org.apache.commons.lang.StringUtils;
import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

/**
 * TODO: to be replace with the one in ste.https
 * @author ste
 */
public class CertificateBuilder {
    private final String SIGNING_ALGHORITM = "SHA512withRSA";
    private final String dn;
    private final long validFrom, validTo;
    
    private KeyPair keys;
    
    public CertificateBuilder(final String dn) {
        if (StringUtils.isBlank(dn)) {
            throw new IllegalArgumentException("dn can not be blank");
        }
        this.dn = dn;
        
        Calendar calendar = Calendar.getInstance();
        validFrom = calendar.getTimeInMillis();
        calendar.add(Calendar.YEAR, 2);
        validTo = calendar.getTimeInMillis();
    }
    
    public String getDN() {
        return dn;
    }
    
    public Date getValidFrom() {
        return new Date(validFrom);
    }
    
    public Date getValidTo() {
        return new Date(validTo);
    }
    
    public KeyPair getKeyPair() {
        return keys;
    }
    
    public CertificateBuilder generateKeyPair() {
        try {
            KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
            keyGenerator.initialize(1024, SecureRandom.getInstance("SHA1PRNG", "SUN"));
            keys = keyGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException | NoSuchProviderException x) {
            //
            // it is supposed these parameters are correct for any jdk
            // TODO: turn this into a runtime exception
            //
        }
        
        return this;
    }
    
    public X509Certificate build() {
        if (keys == null) {
            generateKeyPair();
        }
        
        PrivateKey privkey = keys.getPrivate();
        X509CertInfo info = new X509CertInfo();
        CertificateValidity interval = 
            new CertificateValidity(new Date(validFrom), new Date(validTo));
        
        BigInteger sn = new BigInteger(64, new SecureRandom());
        try {
            X500Name owner = new X500Name(dn);

            info.set(X509CertInfo.VALIDITY, interval);
            info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn));
            info.set(X509CertInfo.SUBJECT, owner);
            info.set(X509CertInfo.ISSUER, owner);
            info.set(X509CertInfo.KEY, new CertificateX509Key(keys.getPublic()));
            info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
            AlgorithmId algo = new AlgorithmId(AlgorithmId.md5WithRSAEncryption_oid);
            info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));

            // Sign the cert to identify the algorithm that's used.
            X509CertImpl cert = new X509CertImpl(info);
            cert.sign(privkey, SIGNING_ALGHORITM);

            // Update the algorith, and resign.
            algo = (AlgorithmId) cert.get(X509CertImpl.SIG_ALG);
            info.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, algo);
            cert = new X509CertImpl(info);
            cert.sign(privkey, SIGNING_ALGHORITM);
            
            return cert;
        } catch (Exception x) {
            //
            // it is supposed these parameters are correct for any jdk
            // TODO: turn this into a runtime exception
            //
            x.printStackTrace();
        }
        
        return null;
    }
}
