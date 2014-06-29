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

package ste.web.http.handlers;

import java.io.IOException;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import ste.web.http.HttpSession;

public class PrintSessionHandler implements HttpRequestHandler  {
    
    private static final String ATTRIBUTE_COUNTER = "counter";
    
    /**
     * Creates a new FileHandler returns a string representation of the session
     * object as a json object.
     * 
     */
    public PrintSessionHandler() {
        
    }

    public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context) throws HttpException, IOException {
                
        HttpSession session = (HttpSession)context;
        
        Long counterAttribute = (Long)session.getAttribute(ATTRIBUTE_COUNTER);
        long counter = (counterAttribute == null) 
                     ? 1 : counterAttribute.longValue()+1;
        
        System.out.println("request: " + request);
        
        response.setStatusCode(HttpStatus.SC_OK);
        
        StringBuilder body = new StringBuilder();
        body.append("{id: ").append(session.getId()).append("}").append('\n')
            .append("{counter: ").append(counter).append("}").append('\n');
        response.setEntity(new StringEntity(body.toString()));
        
        System.out.println("counter : " + counter);
        session.setAttribute(ATTRIBUTE_COUNTER, new Long(counter));
    }
}
