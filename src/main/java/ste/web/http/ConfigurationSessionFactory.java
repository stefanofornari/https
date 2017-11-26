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

import org.apache.commons.configuration.Configuration;
import static ste.web.http.Constants.CONFIG_HTTPS_SESSION_ID_NAME;
import static ste.web.http.Constants.CONFIG_HTTPS_SESSION_LIFETIME;

/**
 *
 */
public class ConfigurationSessionFactory {
    private final long lifetime;
    private final String sessionIdName;
    
    public ConfigurationSessionFactory(Configuration configuration) {
        long sessionLifetime = configuration.getLong(CONFIG_HTTPS_SESSION_LIFETIME, 15*60*1000);
        sessionIdName = configuration.getString(
                            CONFIG_HTTPS_SESSION_ID_NAME, 
                            SessionHeader.DEFAULT_SESSION_HEADER
                        );
        
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
