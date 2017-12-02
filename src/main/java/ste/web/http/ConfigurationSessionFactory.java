/*
 * Copyright (C) 2017 Stefano Fornari.
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

import java.util.NoSuchElementException;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConversionException;
import static ste.web.http.Constants.CONFIG_HTTPS_SESSION_ID_NAME;
import static ste.web.http.Constants.CONFIG_HTTPS_SESSION_LIFETIME;

/**
 *
 */
public class ConfigurationSessionFactory {
    
    public static final long DEFAULT_LIFETIME = 15*60*1000;
    public static final String REGEX_VALID_NAME = "[_a-zA-Z][_a-zA-Z0-9]*";
    
    private final long lifetime;
    private final String sessionIdName;
    
    public ConfigurationSessionFactory(Configuration configuration) 
    throws ConfigurationException {
        if (configuration == null) {
            throw new IllegalArgumentException("configuration can not be null");
        }
        
        try {
            configuration.getLong(CONFIG_HTTPS_SESSION_LIFETIME);
        } catch (ConversionException x) {
            throw new ConfigurationException(CONFIG_HTTPS_SESSION_LIFETIME + " must be a number (seconds)");
        } catch (NoSuchElementException x) {
            //
            // OK
            //
        }
        
        String sessionIdName = configuration.getString(CONFIG_HTTPS_SESSION_ID_NAME);
        if ((sessionIdName != null) && !sessionIdName.matches(REGEX_VALID_NAME)) {
            throw new ConfigurationException(CONFIG_HTTPS_SESSION_ID_NAME + " '" + sessionIdName + "' must be a valid identifier (" + REGEX_VALID_NAME + ")");
        }
        
        long sessionLifetime = configuration.getLong(CONFIG_HTTPS_SESSION_LIFETIME, DEFAULT_LIFETIME);
        this.sessionIdName = (sessionIdName == null) 
                           ? SessionHeader.DEFAULT_SESSION_HEADER
                           : sessionIdName
                           ;
        this.lifetime = (sessionLifetime > 0) ? sessionLifetime : 0;
    }
    
    public long getLifetime() {
        return lifetime;
    }
    
    public String getSessionIdName() {
        return sessionIdName;
    }
    
    public HttpSession create() {
        return new HttpSession(sessionIdName);
    }
}
