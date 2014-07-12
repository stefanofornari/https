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

import java.lang.reflect.Method;
import java.util.HashMap;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Ignore;
import static ste.xtest.reflect.PrivateAccess.*;
import org.junit.Test;

/**
 *
 * TODO: when the session is expired, all data shall be cleaned
 * TODO: synchronize also get()?
 * TODO: sanity check for get
 */
public class BugFreeSessionCache {
    
    @Test
    public void constructurSetLifetime() {
        SessionCache s = new SessionCache();
        then(s.getLifetime()).isEqualTo(SessionCache.DEFAULT_SESSION_LIFETIME);
        s = new SessionCache(100);
        then(s.getLifetime()).isEqualTo(100);
        
        //
        // 0 or negative lifetime values mean "no expiration"
        //
        s = new SessionCache(0);
        then(s.getLifetime()).isZero();
        
        final int[] TEST_NEGATIVE_LIFETIME = {-1, -5, -100};
        
        for (int l: TEST_NEGATIVE_LIFETIME) {
            s = new SessionCache(-1);
            then(s.getLifetime()).isZero();
        }
    }
        
    @Test
    public void mergeExistingSession() throws Exception {
        SessionCache c = new SessionCache(0);
        
        HttpSession s1 = new HttpSession();
        s1.setAttribute("test1", "value1");
        
        c.put(s1);
        then(c.get(s1.getId())).isSameAs(s1);
        
        HttpSession s2 = new HttpSession();
        s2.setId(s1.getId()); c.put(s2);
        then(c.get(s2.getId())).isSameAs(s2);
        then(s2.getAttribute("test1")).isEqualTo("value1");
    }
    
    @Test
    public void doNotExpireUsedSession() throws Exception {
        final long TEST_LIFETIME = 250;
        SessionCache c = new SessionCache(TEST_LIFETIME);
        
        HttpSession s = new HttpSession(); c.put(s);
        
        long ts = System.currentTimeMillis();
        while (System.currentTimeMillis()-ts <= TEST_LIFETIME) {
            Thread.sleep(50);
            c.get(s.getId());
        }
        then(c.get(s.getId()).getId()).isEqualTo(s.getId()); // still the same
    }
        
    @Test
    public void expireUnusedSession() throws Exception {
        final long TEST_LIFETIME = 50;
        SessionCache c = new SessionCache(TEST_LIFETIME);
        
        HttpSession s = new HttpSession(); c.put(s);
        
        Thread.sleep(100);
        then(c.get(s.getId())).isNull();
    }
    
    @Test
    public void purgeExpiredSessions() throws Exception {
        SessionCache c = new SessionCache(75, 200);
        HashMap lastAccess = (HashMap)getInstanceValue(c, "lastAccess");
        
        HttpSession s1 = new HttpSession(); c.put(s1); Thread.sleep(25);
        HttpSession s2 = new HttpSession(); c.put(s2); Thread.sleep(25);
        
        //
        // Before a session expires, I get it; once expired I get null, but 
        // lastAccess still contains it; after purgetime lastAccess shall be
        // empty
        //
        then(c.get(s1.getId())).isNotNull();
        then(lastAccess).hasSize(2);
        Thread.sleep(50);
        then(c.get(s1.getId())).isNotNull();
        then(c.get(s2.getId())).isNull();
        then(lastAccess).isNotEmpty();
        Thread.sleep(200);
        then(c.get(s1.getId())).isNull();
        then(c.get(s2.getId())).isNull();
        then(lastAccess).isEmpty();
    }
    
    @Test
    public void noExpiration() throws Exception {
        SessionCache c = new SessionCache(0);
        HttpSession s = new HttpSession();
        c.put(s);
        
        Method m = SessionCache.class.getDeclaredMethod("expireSession", String.class);
        m.setAccessible(true); m.invoke(c, s.getId());
        then(c).hasSize(1);
        
        m = SessionCache.class.getDeclaredMethod("purge");
        m.setAccessible(true); m.invoke(c);
        then(c).hasSize(1);
    }

}
