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

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Test;
import ste.xtest.reflect.CodeInjector;
import static ste.xtest.reflect.PrivateAccess.getInstanceValue;

/**
 *
 * @author ste
 */
public class BugFreeSessionCacheThreading {
    @Test
    public void concurrentNewSessions() throws Exception {
        //
        // Putting a delay before expiring a session allows another thread to 
        // enter the critical section so that if the section is not synchronized
        // two thread returns two new sessions
        //
        CodeInjector injector = new CodeInjector("ste.web.http.SessionCache");
        injector.beforeMethod(
            "expireSession", "ste.web.http.BugFreeSessionCacheThreading.Wait.await();"
        ).beforeMethod(
            "traceAccess", "ste.web.http.BugFreeSessionCacheThreading.Wait.await();"
        );
        
        SessionCache c = (SessionCache)injector.toClass().newInstance();
        HttpSession s = c.get(null);
        
        Wait.LATCH = new CountDownLatch(1);
        
        TestTask t1 = new TestTask(c, s);
        TestTask t2 = new TestTask(c, s);
        
        Thread th1 = new Thread(t1); th1.start();  // now t1 is blocked on traceAccess()
        Thread.sleep(50);
        HashMap<String, Long> lastAccess = (HashMap<String, Long>)getInstanceValue(c, "lastAccess");
        lastAccess.put(s.getId(), 0L);
        Thread th2 = new Thread(t2); th2.start();  // now t1 enters the critical section and block on expireSession
        
        Thread.sleep(100); // let's give some time to get blocked
        
        //
        // release the latches and wait for termination
        //
        Wait.kick();
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
    
    public static class Wait {
        public static CountDownLatch LATCH = null;
        
        public static void await() {           
            if (LATCH != null) {
                try {
                    LATCH.await();
                } catch (InterruptedException ex) {}
            }
        }
        
        public static void kick() {
            if (LATCH != null) {
                LATCH.countDown();
            }
        }
    }
}
