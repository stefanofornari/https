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
import static ste.xtest.reflect.PrivateAccess.*;
import org.junit.Test;

/**
 *
 * TODO: when the session is expired, all data shall be cleaned
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
    public void getNewSession() {
        HttpSession s;
        SessionCache c = new SessionCache(0);
        then(s = c.get(null)).isNotNull();
        then(c.get(null).getId()).isNotEqualTo(s.getId());
    }
    
    @Test
    public void getExistingSession() {
        SessionCache c = new SessionCache(0);
        
        HttpSession s = c.get(null);
        then(c.get(s.getId())).isSameAs(s);
    }
    
    @Test
    public void expireUnusedSessionDoNotExpireUsedSession() throws Exception {
        final long TEST_LIFETIME = 250;
        SessionCache c = new SessionCache(TEST_LIFETIME);
        
        HttpSession s1 = c.get(null);
        HttpSession s2 = c.get(null);
        
        long ts = System.currentTimeMillis();
        while (System.currentTimeMillis()-ts <= TEST_LIFETIME) {
            Thread.sleep(50);
            c.get(s2.getId());
        }
        then(c.get(s2.getId()).getId()).isEqualTo(s2.getId()); // still the same
        then(c.get(s1.getId()).getId()).isNotEqualTo(s1.getId()); // new
        
        //
        // check that the maps has been clened up
        //
        HashMap lastAccess = (HashMap)getInstanceValue(c, "lastAccess");
        then(lastAccess).hasSize(2);
        then(c).hasSize(2);
    }
    
    @Test
    public void expireAllUnusedSessions() throws Exception {
        final long TEST_LIFETIME = 200;
        final long TEST_PURGETIME = 500;
        SessionCache c = new SessionCache(TEST_LIFETIME, TEST_PURGETIME);
        
        HttpSession s1 = c.get(null);
        HttpSession s2 = c.get(null);
        HttpSession s3 = c.get(null);
        
        long ts = System.currentTimeMillis();
        while (System.currentTimeMillis()-ts <= TEST_LIFETIME) {
            Thread.sleep(10);
            c.get(s2.getId());
        }
        then(c.get(s2.getId()).getId()).isEqualTo(s2.getId()); // still the same
        
        HashMap lastAccess = (HashMap)getInstanceValue(c, "lastAccess");
        then(lastAccess).hasSize(3);
        then(c).hasSize(3);
        
        //
        // let's expire s2 too; s2 will expire only after purgePeriod 
        // milliseconds, therefore the first time no items will be purged
        //
        Thread.sleep(TEST_LIFETIME + 10); c.get(s2.getId());
        then(lastAccess).hasSize(3);
        then(c).hasSize(3);
        
        Thread.sleep(TEST_LIFETIME);
        c.get(s2.getId());
        then(lastAccess).hasSize(1);
        then(c).hasSize(1);
    }
    
    @Test
    public void noExpiration() throws Exception {
        SessionCache c = new SessionCache(0);
        HttpSession s = c.get(null);
        
        Method m = SessionCache.class.getDeclaredMethod("expireSession", String.class);
        m.setAccessible(true); m.invoke(c, s.getId());
        then(c).hasSize(1);
        
        m = SessionCache.class.getDeclaredMethod("purge");
        m.setAccessible(true); m.invoke(c);
        then(c).hasSize(1);
    }

}
