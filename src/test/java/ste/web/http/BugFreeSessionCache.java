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
import java.time.Clock;
import java.util.HashMap;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.then;
import static ste.xtest.reflect.PrivateAccess.*;
import org.junit.Test;
import static ste.web.http.Constants.CONFIG_HTTPS_SESSION_ID_NAME;
import static ste.web.http.Constants.CONFIG_HTTPS_SESSION_LIFETIME;
import ste.xtest.reflect.PrivateAccess;
import ste.xtest.time.FixedClock;

/**
 * TODO: see cobertura report
 */
public class BugFreeSessionCache {
    
    @Test
    public void constructur_set_lifetime() throws Exception {
        SessionCache s = new SessionCache(getSessionFactory((Long)null));
        then(s.getLifetime()).isEqualTo(SessionCache.DEFAULT_SESSION_LIFETIME);
        s = new SessionCache(getSessionFactory((long)100));
        then(s.getLifetime()).isEqualTo(100);
        
        //
        // 0 or negative lifetime values mean "no expiration"
        //
        s = new SessionCache(getSessionFactory((long)0));
        then(s.getLifetime()).isZero();
        
        final int[] TEST_NEGATIVE_LIFETIME = {-1, -5, -100};
        
        for (int l: TEST_NEGATIVE_LIFETIME) {
            s = new SessionCache(getSessionFactory((long)-1));
            then(s.getLifetime()).isZero();
        }
    }
    
    @Test
    public void disable_putXXX() throws Exception {
        //
        // get(null) creates new items and put them in the cache; we disable 
        // the use of put() and putAll() for now
        //
        SessionCache c = new SessionCache(getSessionFactory((Long)null));
        try {
            HttpSession s = new HttpSession();
            c.put(s.getId(), s);
            fail("put shall be disabled");
        } catch (UnsupportedOperationException x) {
           then(x.getMessage()).contains("put()").contains("unsupported");
        }
        
        try {
            c.putAll(new HashMap<String, HttpSession>());
            fail("putAll shall be disabled");
        } catch (UnsupportedOperationException x) {
           then(x.getMessage()).contains("putAll()").contains("unsupported");
        }
    }
    
    @Test
    public void do_not_expire_used_session() throws Exception {
        final FixedClock CLOCK = new FixedClock();
        final long TEST_LIFETIME = 250;
        
        SessionCache c = createSessionCache(CLOCK, TEST_LIFETIME);
        
        HttpSession s = c.get(null);
        
        for(int i=0; i<10; ++i) {
            CLOCK.millis += i;
            c.get(s.getId());
        }
        then(c.get(s.getId()).getId()).isEqualTo(s.getId()); // still the same
    }
        
    @Test
    public void expire_unused_session() throws Exception {
        final FixedClock CLOCK = new FixedClock();
        final long TEST_LIFETIME = 50;
        
        SessionCache c = createSessionCache(CLOCK, TEST_LIFETIME);
        
        HttpSession s = c.get(null); s.setAttribute("TEST", "test");
        
        CLOCK.millis += 75;
        
        //
        // When expired, I still get a session with same id, but values will be
        // gone
        //
        then(c.get(s.getId()).getAttribute("TEST")).isNull();
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void purge_expired_sessions() throws Exception {
        final FixedClock CLOCK = new FixedClock();
        
        SessionCache c = createSessionCache(CLOCK, 75);
        
        HashMap lastAccess = (HashMap)getInstanceValue(c, "lastAccess");
        PrivateAccess.setInstanceValue(c, "purgetime", 200);
        
        HttpSession s1 = c.get(null); s1.setAttribute("TEST1", "test1"); CLOCK.millis += 25;
        HttpSession s2 = c.get(null); s1.setAttribute("TEST2", "test2"); CLOCK.millis += 25;
        
        //
        // Before a session expires, I get it; once expired I get a new session,
        // but lastAccess still contains it; after purgetime() lastAccess shall 
        // be empty
        //
        then(c.get(s1.getId())).isNotNull();
        then(lastAccess).hasSize(2);
        CLOCK.millis += 55;
        then(c.get(s1.getId())).isNotNull();
        then(c.get(s2.getId()).getAttribute("TEST2")).isNull();
        then(lastAccess).hasSize(3); // one session has expired
        
        CLOCK.millis += 200; c.get(null); // triggering purge
        
        then(lastAccess)
            .hasSize(1)
            .doesNotContainKey(s1.getId())
            .doesNotContainKey(s2.getId());
    }
    
    @Test
    public void no_expiration() throws Exception {
        SessionCache c = new SessionCache(getSessionFactory((long)0));
        HttpSession s = c.get(null);
        
        Method m = SessionCache.class.getDeclaredMethod("expireSession", String.class);
        m.setAccessible(true); m.invoke(c, s.getId());
        then(c).hasSize(1);
        
        m = SessionCache.class.getDeclaredMethod("purge");
        m.setAccessible(true); m.invoke(c);
        then(c).hasSize(1);
    }
    
    @Test
    public void new_session_when_id_is_null() throws Exception {
        SessionCache c = new SessionCache(getSessionFactory((long)0));
        HttpSession s = c.get(null);
        
        then(s).isNotNull();
        then(s.getId()).isNotNull();
    }
    
    @Test
    public void new_session_when_id_is_not_found() throws Exception {
        final String NOT_EXISTING_ID = "notexistingid";
        SessionCache c = new SessionCache(getSessionFactory((long)0));
        HttpSession s = c.get(NOT_EXISTING_ID);
        
        then(s).isNotNull();
        then(s.getId()).isNotNull().isNotEqualTo(NOT_EXISTING_ID);
    }
    
    @Test
    public void session_is_not_accessible_any_more_after_expiration() throws Exception {
        final FixedClock CLOCK = new FixedClock();

        SessionCache c = createSessionCache(CLOCK, 50);
        PrivateAccess.setInstanceValue(c, "purgetime", 50);
        HttpSession s = c.get(null);
        
        CLOCK.millis += 75; // the session shall be expired now
        c.get(null); // to trigger clean up
        
        try {
            s.setAttribute("test", null);
            fail("session should not be accessible after expiration!");
        } catch (IllegalStateException x) {
            then(x.getMessage()).contains(s.getId()).contains("expired");
        }
    }
    
    @Test
    public void get_session_id_name() throws Exception {
        for (String N: new String[] {"pid", "jsession"}) {
            SessionCache c = new SessionCache(getSessionFactory(N));

            HttpSession s = c.get(null);
            then(c.getSessionIdName()).isEqualTo(N);
        }
        
        
    }
    
    // --------------------------------------------------------- private methods
    
    protected ConfigurationSessionFactory getSessionFactory(Long lifetime) throws Exception {
        return getSessionFactory(lifetime, null);
    }
    
    protected ConfigurationSessionFactory getSessionFactory(String name) throws Exception {
        return getSessionFactory(null, name);
    }
    
    protected ConfigurationSessionFactory getSessionFactory(Long lifetime, String name)
    throws Exception {
        Configuration c = new PropertiesConfiguration();
        if (lifetime != null) {
            c.addProperty(CONFIG_HTTPS_SESSION_LIFETIME, lifetime);
        }
        if (name != null) {
            c.addProperty(CONFIG_HTTPS_SESSION_ID_NAME, name);
        }
        return new ConfigurationSessionFactory(c);
    }
    
    private SessionCache createSessionCache(Clock clock, long lifetime) throws Exception {
        SessionCache c = new SessionCache(getSessionFactory((long)lifetime));
        setInstanceValue(c, "clock", clock);
        
        return c;
    }
}
