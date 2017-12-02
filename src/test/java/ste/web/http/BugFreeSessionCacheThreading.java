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

import java.util.concurrent.CountDownLatch;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Test;
import static ste.web.http.Constants.CONFIG_HTTPS_SESSION_LIFETIME;

/**
 *
 * @author ste
 */
public class BugFreeSessionCacheThreading {
    @Test
    public void concurrent_access_to_expired_session_results_in_two_sessions() 
    throws Exception {
        SessionCache c = new TestSessionCache(getSessionFactory());
        HttpSession s = c.get(null);
        
        ((TestSessionCache)c).LATCH = new CountDownLatch(1);
        
        TestTask t1 = new TestTask(c, s.getId());
        TestTask t2 = new TestTask(c, s.getId());
        
        Thread th1 = new Thread(t1); th1.start();  // now t1 is blocked on traceAccess()
        Thread th2 = new Thread(t2); th2.start();  // now t1 enters the critical section and blocks on expireSession
        
        Thread.sleep(50); // let's give some time to get blocked
        
        //
        // release the latch and wait for termination
        //
        ((TestSessionCache)c).LATCH.countDown();
        th1.join(); th2.join();
        
        //
        // we should have a valid session
        //
        then(t1.session.getId()).isNotEqualTo(t2.session.getId());
        then(t1.session).isNotSameAs(t2.session);
    }
    
    // --------------------------------------------------------- private methods
    
    private ConfigurationSessionFactory getSessionFactory() throws Exception {
        Configuration c = new PropertiesConfiguration();
        c.addProperty(CONFIG_HTTPS_SESSION_LIFETIME, 25);
        return new ConfigurationSessionFactory(c);
    }
    
    // -------------------------------------------------------------------------
    
    private class TestTask implements Runnable {
        private String ret = null;
        
        private SessionCache cache;
        private HttpSession session;
        private String id;
        
        public TestTask(SessionCache c, String id) {
            this.cache = c;
            this.id = id;
            this.session = null;
        }
        
        @Override
        public void run() {
            session = cache.get(id);
        }
    }
    
    private class TestSessionCache extends SessionCache {
        public CountDownLatch LATCH = null;
        
        public TestSessionCache(ConfigurationSessionFactory c) {
            super(c);
        }
        
        @Override
        protected boolean isExpired(Long ts) {
            if (LATCH != null) {
                try { LATCH.await(); } catch (InterruptedException x) {};
            }
            return super.isExpired(ts);
        }
    }
}
