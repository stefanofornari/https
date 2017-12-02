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

/**
 *
 * @author ste
 */
public class Constants {
    public static final String CONFIG_SSL_PASSWORD   = "ste.https.ssl.password";
    public static final String CONFIG_HTTPS_ROOT     = "ste.https.root";
    public static final String CONFIG_HTTPS_SSL_PORT = "ste.https.ssl.port";
    public static final String CONFIG_HTTPS_WEB_PORT 
                                                     = "ste.https.web.port";
    public static final String CONFIG_HTTPS_AUTH     = "ste.https.auth";
    public static final String CONFIG_HTTPS_SESSION_ID_NAME
                                                     = "ste.https.session.name";
    public static final String CONFIG_HTTPS_SESSION_LIFETIME 
                                                     = "ste.https.session.lifetime";
    public static final String CONFIG_HTTPS_WEBROOT 
                                                     = "ste.https.webroot";
    
    public static final int DEFAULT_SSL_PORT = 8484;
    public static final int DEFAULT_WEB_PORT = 8400;
    
}
