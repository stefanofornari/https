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
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Test;

/**
 *
 * @author ste
 */
public class BugFreeSessionCacheThreading {
    @Test
    public void concurrentNewSessions() throws Exception {
        SessionCache c = new TestSessionCache();
        HttpSession s = c.get(null);
        
        ((TestSessionCache)c).LATCH = new CountDownLatch(1);
        
        TestTask t1 = new TestTask(c, s);
        TestTask t2 = new TestTask(c, s);
        
        Thread th1 = new Thread(t1); th1.start();  // now t1 is blocked on traceAccess()
        Thread th2 = new Thread(t2); th2.start();  // now t1 enters the critical section and block on expireSession
        
        Thread.sleep(100); // let's give some time to get blocked
        
        //
        // release the latches and wait for termination
        //
        ((TestSessionCache)c).LATCH.countDown();
        th1.join(); th2.join();
        
        //
        // we should have a valid session
        //
        then(t1.session.getId()).isEqualTo(t2.session.getId());
    }
    
    // -------------------------------------------------------------------------
    
    private class TestTask implements Runnable {
        private String ret = null;
        
        private SessionCache cache;
        private HttpSession session;
        
        public boolean done;
        
        public TestTask(SessionCache c, HttpSession s) {
            this.cache = c;
            this.session = s;
        }
        
        @Override
        public void run() {
            done = false;
            session = cache.get(session.getId());
            done = true;
        }
    }
    
    private class TestSessionCache extends SessionCache {
        public CountDownLatch LATCH = null;
        
        @Override
        protected void expireSession(String id) {
            if (LATCH != null) {
                try { LATCH.await(); } catch (InterruptedException x) {};
            }
            super.expireSession(id);
        }
    
        @Override
        protected void traceAccess(String id) {
            if (LATCH != null) {
                try { LATCH.await(); } catch (InterruptedException x) {};
            }
            super.traceAccess(id);
        }
    }
}
