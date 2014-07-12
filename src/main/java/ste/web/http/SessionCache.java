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
import java.util.Map;

/**
 * This class implements the cache for the sessions. Entries expire after a
 * given lifetime if not accessed (either in read or write). Expiration is the
 * entries is checked triggered by the access to any entry and if the last check
 * was performed earlier than a threshold.
 * 
 */
class SessionCache extends HashMap<String, HttpSession> {
    
    public static final long DEFAULT_SESSION_LIFETIME = 1000*60*15; // 15 min
    public static final long DEFAULT_SESSION_PURGETIME = 1000*5; // 5 seconds
    
    private final Map<String, Long> lastAccess;
    
    private final long lifetime, purgetime;
    private long lastPurge;
    
    
    /**
     * Creates the session cache with default session lifetime and purgetime
     */
    public SessionCache() {
        this(DEFAULT_SESSION_LIFETIME);
    }
    /**
     * Creates the session cache with the given lifetime.
     * 
     * @param lifetime entries lifetime - NOT NULL
     */
    public SessionCache(long lifetime) {
        this(lifetime, DEFAULT_SESSION_PURGETIME);
    }
    
    public SessionCache(long lifetime, long purgetime) {
        super();
        
        this.lifetime = (lifetime >= 0) ? lifetime : 0;
        this.purgetime = (purgetime >= 0) ? purgetime : 0;
        this.lastPurge = 0;
        
        this.lastAccess = new HashMap<>();
    }
    
    public long getLifetime() {
        return lifetime;
    }
    
    @Override
    public HttpSession put(String id, HttpSession session) {
        HttpSession ret = super.put(id, session);
        traceAccess(id);
        
        return ret;
    }
    
    /**
     * Puts the given session in the cache, replacing an existing one with the 
     * same session Id if already cached. If a session with same id is already
     * in the cache, the values of the existing sessions are stored in the new 
     * one.
     * 
     * @param session the session id - NOT NULL
     * 
     * @return the session with the given id if found in the cache, or a new
     *         session if id is null or a session with the given id is not found
     *         or expired.
     */
    public synchronized HttpSession put(final HttpSession session) {
        String id = session.getId();
        Long lastTS = lastAccess.get(id);
        if ((lastTS == null) || isExpired(lastTS)) {
            expireSession(id);
            put(id, session);  // this performs traceAccess()
            purge();
            
            return null;
        }
        
        HttpSession oldSession = get(session.getId());
        if (oldSession != null) {
            session.putAll(oldSession);
        }
    
        traceAccess(id);
        purge();
        return (HttpSession)super.put(id, session);
    }
    
    public HttpSession get(final String id) {
        purge();
        
        Long lastTS = lastAccess.get(id);
        if ((lastTS == null) || isExpired(lastTS)) {
            return null;
        }
        traceAccess(id);
        
        return (HttpSession)super.get(id);
    }

    // --------------------------------------------------------- private methods
    
    private boolean isExpired(Long lastTS) {
        long ts = System.currentTimeMillis();
        return (lifetime != 0) && (ts-lastTS > lifetime);
    }
    
    protected void expireSession(String id) {
        if ((lifetime > 0) && (id != null)) {
            remove(id);
            lastAccess.remove(id);
        }
    }
    
    protected void traceAccess(String id) {
        lastAccess.put(id, System.currentTimeMillis());
    }
    
    private void purge() {
        if (lifetime == 0) {
            return;
        }
        
        long ts = System.currentTimeMillis();
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
