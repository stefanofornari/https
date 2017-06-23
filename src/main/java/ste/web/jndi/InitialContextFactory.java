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
package ste.web.jndi;

import java.io.File;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingException;

/**
 *
 * @author ste
 */
public class InitialContextFactory extends org.osjava.sj.SimpleContextFactory {
    public InitialContextFactory() {
        super();
    }
    
    /**
     * @see javax.naming.spi.InitialContextFactory#getInitialContext(java.util.Hashtable)
     */
    @Override
    public Context getInitialContext(Hashtable env) throws NamingException {
        env.put("org.osjava.sj.delimiter", "/");
        env.put("org.osjava.sj.space", "java:comp/env");
        env.put("org.osjava.sj.jndi.shared", "true");
        env.put("org.osjava.sj.root", new File("conf/https.properties").getAbsolutePath());
        
        return super.getInitialContext(env);
    }
}
