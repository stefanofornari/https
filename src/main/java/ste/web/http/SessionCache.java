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

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

/**
 * This class implements the cache for the sessions. Entries expire after a
 * given lifetime if not accessed (either in read or write). Expiration is the
 * entries is checked triggered by the access to any entry and if the last check
 * was performed earlier than a threshold.
 * 
 */
class SessionCache extends HashMap<String, HttpSession> {
    //
    // making time predictable...
    //
    private Clock clock = Clock.systemDefaultZone();
    
    public static final long DEFAULT_SESSION_LIFETIME = 1000*60*15; // 15 min
    public static final long DEFAULT_SESSION_PURGETIME = 1000*5; // 5 seconds
    
    private final Map<String, Long> lastAccess;
    
    private final ConfigurationSessionFactory sessionFactory;
    private final long lifetime, purgetime;
    private long lastPurge;
    private String sessionIdName;
    
    /**
     * Creates the session cache with default session lifetime and purgetime
     */
    public SessionCache(ConfigurationSessionFactory sessionFactory) {
        super();
        
        this.sessionFactory = sessionFactory;
        this.lifetime = sessionFactory.getLifetime();
        this.sessionIdName = sessionFactory.getSessionIdName();
        this.purgetime = DEFAULT_SESSION_PURGETIME;
        this.lastPurge = 0;
        
        this.lastAccess = new HashMap<>();
    }
    
    public long getLifetime() {
        return lifetime;
    }
    
    public String getSessionIdName() {
        return sessionIdName;
    }
    
    /**
     * @throws UnsupportedOperationException - use get(null) instead
     */
    @Override
    public HttpSession put(String id, HttpSession session) {
        throw new UnsupportedOperationException("put() is unsupported; use get(null) instead");
    }
    
    /**
     * @throws UnsupportedOperationException - use get(null) instead
     */
    @Override
    public void putAll(Map<? extends String, ? extends HttpSession> map) {
        throw new UnsupportedOperationException("putAll() is unsupported; use get(null) instead");
    }
    
    /**
     * Returns the session associated to the given id if found end not expired; 
     * otherwise a new session is created.
     * 
     * @param id the session id - ANY VALUE
     * 
     * @return the session associated to the given id if found end not expired; 
     * otherwise a new session is created.
     */
    public synchronized HttpSession get(final String id) {
        HttpSession session = (HttpSession)super.get(id);
        
        purge();
        Long lastTS = lastAccess.get(id);
        if ((lastTS == null) || isExpired(lastTS) || (session == null)) {
            session = sessionFactory.create();
            super.put(session.getId(), session);
        }
        trackAccess(session.getId());
        
        return session;
    }

    // --------------------------------------------------------- private methods
    
    protected boolean isExpired(Long lastTS) {
        long ts = clock.millis();
        return (lifetime != 0) && (ts-lastTS > lifetime);
    }
    
    private void expireSession(final String id) {
        if ((lifetime != 0) && (id != null)) {
            HttpSession session = (HttpSession)super.get(id);
            if (session != null) {
                session.expire();
            }
            remove(id);
            lastAccess.remove(id);
        }
    }
    
    private void trackAccess(String id) {
        lastAccess.put(id, clock.millis());
    }
    
    @SuppressWarnings("unchecked")
    private void purge() {
        if (lifetime == 0) {
            return;
        }
        
        long ts = clock.millis();
        if (ts-lastPurge <= purgetime) {
            return;
        }
        
        Object[] access = lastAccess.entrySet().toArray();
        for (Object o: access) {
            Map.Entry<String, Long> a = (Map.Entry<String, Long>)o;
            long lastTS = a.getValue();
            if (isExpired(lastTS)) {
                expireSession(a.getKey());
            }
        }
        lastPurge = ts;
    }
}
