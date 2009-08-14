/**
 * OWASP Enterprise Security API (ESAPI) This file is part of the Open Web
 * Application Security Project (OWASP) Enterprise Security API (ESAPI) project.
 * For details, please see <a
 * href="http://www.owasp.org/index.php/ESAPI">http://
 * www.owasp.org/index.php/ESAPI</a>. Copyright (c) 2007 - The OWASP Foundation
 * The ESAPI is published by OWASP under the BSD license. You should read and
 * accept the LICENSE before you use, modify, and/or redistribute this software.
 * 
 * @author Jeff Williams <a href="http://www.aspectsecurity.com">Aspect
 *         Security</a>
 * @created 2007
 */
package org.owasp.esapi.filters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import org.owasp.esapi.errors.ValidationException;

/**
 * This request wrapper simply overrides unsafe methods in the
 * HttpServletRequest API with safe versions that return canonicalized data
 * where possible. The wrapper returns a safe value when a validation error is
 * detected, including stripped or empty strings.
 */
public class SecurityWrapperRequest extends HttpServletRequestWrapper implements HttpServletRequest {

    private final Logger logger = ESAPI.getLogger("SecurityWrapperRequest");

    /**
     * Construct a safe request that overrides the default request methods with
     * safer versions.
     * 
     * @param request
     */
    public SecurityWrapperRequest(HttpServletRequest request) {
    	super( request );
    }

    private HttpServletRequest getHttpServletRequest() {
    	return (HttpServletRequest)super.getRequest();
    }
    
    /**
     * Same as HttpServletRequest, no security changes required.
     * @param name
     * @return
     */
    public Object getAttribute(String name) {
        return getHttpServletRequest().getAttribute(name);
    }

    /**
     * Same as HttpServletRequest, no security changes required.
     * @return
     */
    public Enumeration getAttributeNames() {
        return getHttpServletRequest().getAttributeNames();
    }

    /**
     * Same as HttpServletRequest, no security changes required.
     * @return
     */
    public String getAuthType() {
        return getHttpServletRequest().getAuthType();
    }

    /**
     * Same as HttpServletRequest, no security changes required.
     * @return
     */
    public String getCharacterEncoding() {
        return getHttpServletRequest().getCharacterEncoding();
    }

    /**
     * Same as HttpServletRequest, no security changes required.
     * @return
     */
    public int getContentLength() {
        return getHttpServletRequest().getContentLength();
    }

    /**
     * Same as HttpServletRequest, no security changes required.
     * @return
     */
    public String getContentType() {
        return getHttpServletRequest().getContentType();
    }

    /**
     * Returns the context path from the HttpServletRequest after canonicalizing
     * and filtering out any dangerous characters.
     * @return
     */
    public String getContextPath() {
        String path = getHttpServletRequest().getContextPath();
        String clean = "";
        try {
            clean = ESAPI.validator().getValidInput("HTTP context path: " + path, path, "HTTPContextPath", 150, false);
        } catch (ValidationException e) {
            // already logged
        }
        return clean;
    }

    /**
     * Returns the array of Cookies from the HttpServletRequest after
     * canonicalizing and filtering out any dangerous characters.
     * @return
     */
    public Cookie[] getCookies() {
        Cookie[] cookies = getHttpServletRequest().getCookies();
        if (cookies == null) return new Cookie[0];
        
        List newCookies = new ArrayList();
        for (int i = 0; i < cookies.length; i++) {
            Cookie c = cookies[i];

            // build a new clean cookie
            try {
                // get data from original cookie
                String name = ESAPI.validator().getValidInput("Cookie name: " + c.getName(), c.getName(), "HTTPCookieName", 150, false);
                String value = ESAPI.validator().getValidInput("Cookie value: " + c.getValue(), c.getValue(), "HTTPCookieValue", 1000, false);
                int maxAge = c.getMaxAge();
                String domain = c.getDomain();
                String path = c.getPath();
				
                Cookie n = new Cookie(name, value);
                n.setMaxAge(maxAge);

                if (domain != null) {
                    n.setDomain(ESAPI.validator().getValidInput("Cookie domain: " + domain, domain, "HTTPHeaderValue", 200, false));
                }
                if (path != null) {
                    n.setPath(ESAPI.validator().getValidInput("Cookie path: " + path, path, "HTTPHeaderValue", 200, false));
                }
                newCookies.add(n);
            } catch (ValidationException e) {
                logger.warning(Logger.SECURITY_FAILURE, "Skipping bad cookie: " + c.getName() + "=" + c.getValue(), e );
            }
        }
        return (Cookie[]) newCookies.toArray(new Cookie[newCookies.size()]);
    }

    /**
     * Same as HttpServletRequest, no security changes required.
     * @param name 
     * @return
     */
    public long getDateHeader(String name) {
        return getHttpServletRequest().getDateHeader(name);
    }

    /**
     * Returns the named header from the HttpServletRequest after canonicalizing
     * and filtering out any dangerous characters.
     * @param name 
     * @return
     */
    public String getHeader(String name) {
        String value = getHttpServletRequest().getHeader(name);
        String clean = "";
        try {
            clean = ESAPI.validator().getValidInput("HTTP header value: " + value, value, "HTTPHeaderValue", 150, false);
        } catch (ValidationException e) {
            // already logged
        }
        return clean;
    }

    /**
     * Returns the enumeration of header names from the HttpServletRequest after
     * canonicalizing and filtering out any dangerous characters.
     * @return
     */
    public Enumeration getHeaderNames() {
        Vector v = new Vector();
        Enumeration en = getHttpServletRequest().getHeaderNames();
        while (en.hasMoreElements()) {
            try {
                String name = (String) en.nextElement();
                String clean = ESAPI.validator().getValidInput("HTTP header name: " + name, name, "HTTPHeaderName", 150, false);
                v.add(clean);
            } catch (ValidationException e) {
                // already logged
            }
        }
        return v.elements();
    }

    /**
     * Returns the enumeration of headers from the HttpServletRequest after
     * canonicalizing and filtering out any dangerous characters.
     * @param name
     * @return
     */
    public Enumeration getHeaders(String name) {
        Vector v = new Vector();
        Enumeration en = getHttpServletRequest().getHeaders(name);
        while (en.hasMoreElements()) {
            try {
                String value = (String) en.nextElement();
                String clean = ESAPI.validator().getValidInput("HTTP header value (" + name + "): " + value, value, "HTTPHeaderValue", 150, false);
                v.add(clean);
            } catch (ValidationException e) {
                // already logged
            }
        }
        return v.elements();
    }

    /**
     * Same as HttpServletRequest, no security changes required. Note that this
     * input stream may contain attacks and the developer is responsible for
     * canonicalizing, validating, and encoding any data from this stream.
     * @return 
     * @throws IOException
     */
    public ServletInputStream getInputStream() throws IOException {
        return getHttpServletRequest().getInputStream();
    }

    /**
     * Same as HttpServletRequest, no security changes required.
     * @param name 
     * @return
     */
    public int getIntHeader(String name) {
        return getHttpServletRequest().getIntHeader(name);
    }

    /**
     * Same as HttpServletRequest, no security changes required.
     * @return
     */
    public String getLocalAddr() {
        return getHttpServletRequest().getLocalAddr();
    }

    /**
     * Same as HttpServletRequest, no security changes required.
     * @return
     */
    public Locale getLocale() {
        return getHttpServletRequest().getLocale();
    }

    /**
     * Same as HttpServletRequest, no security changes required.
     * @return
     */
    public Enumeration getLocales() {
        return getHttpServletRequest().getLocales();
    }

    /**
     * Same as HttpServletRequest, no security changes required.
     * @return
     */
    public String getLocalName() {
        return getHttpServletRequest().getLocalName();
    }

    /**
     * Same as HttpServletRequest, no security changes required.
     * @return
     */
    public int getLocalPort() {
        return getHttpServletRequest().getLocalPort();
    }

    /**
     * Same as HttpServletRequest, no security changes required.
     * @return
     */
    public String getMethod() {
        return getHttpServletRequest().getMethod();
    }

    /**
     * Returns the named parameter from the HttpServletRequest after
     * canonicalizing and filtering out any dangerous characters.
     * @param name
     * @return
     */
    public String getParameter(String name) {
        String orig = getHttpServletRequest().getParameter(name);
        String clean = "";
        try {
            clean = ESAPI.validator().getValidInput("HTTP parameter name: " + name, orig, "HTTPParameterValue", 2000, false);
        } catch (ValidationException e) {
            // already logged
        }
        return clean;
    }

    /**
     * Returns the parameter map from the HttpServletRequest after
     * canonicalizing and filtering out any dangerous characters.
     * @return
     */
    public Map getParameterMap() {
        Map map = getHttpServletRequest().getParameterMap();
        HashMap cleanMap = new HashMap();
        Iterator i = map.entrySet().iterator();
        while (i.hasNext()) {
            try {
                Map.Entry e = (Map.Entry) i.next();
                String name = (String) e.getKey();
                String cleanName = ESAPI.validator().getValidInput("HTTP parameter name: " + name, name, "HTTPParameterName", 100, false);

                String[] value = (String[]) e.getValue();
                String[] cleanValues = new String[value.length];
                for (int j = 0; j < value.length; j++) {
                    String cleanValue = ESAPI.validator().getValidInput("HTTP parameter value: " + value[j], value[j], "HTTPParameterValue", 2000, false);
                    cleanValues[j] = cleanValue;
                }
                cleanMap.put(cleanName, cleanValues);
            } catch (ValidationException e) {
                // already logged
            }
        }
        return cleanMap;
    }

    /**
     * Returns the enumeration of parameter names from the HttpServletRequest
     * after canonicalizing and filtering out any dangerous characters.
     * @return
     */
    public Enumeration getParameterNames() {
        Vector v = new Vector();
        Enumeration en = getHttpServletRequest().getParameterNames();
        while (en.hasMoreElements()) {
            try {
                String name = (String) en.nextElement();
                String clean = ESAPI.validator().getValidInput("HTTP parameter name: " + name, name, "HTTPParameterName", 150, false);
                v.add(clean);
            } catch (ValidationException e) {
                // already logged
            }
        }
        return v.elements();
    }

    /**
     * Returns the array of matching parameter values from the
     * HttpServletRequest after canonicalizing and filtering out any dangerous
     * characters.
     * @param name 
     * @return
     */
    public String[] getParameterValues(String name) {
        String[] values = getHttpServletRequest().getParameterValues(name);
        List newValues = new ArrayList();
        if ( values != null ) {
            for (int i = 0; i < values.length; i++) {
                try {
                    String value = values[i];
                    String cleanValue = ESAPI.validator().getValidInput("HTTP parameter value: " + value, value, "HTTPParameterValue", 2000, false);
                    newValues.add(cleanValue);
                } catch (ValidationException e) {
                    logger.warning(Logger.SECURITY_FAILURE, "Skipping bad parameter" );
                }
            }
        }
        return (String[]) newValues.toArray(new String[0]);
    }

    /**
     * Returns the path info from the HttpServletRequest after canonicalizing
     * and filtering out any dangerous characters.
     * @return
     */
    public String getPathInfo() {
        String path = getHttpServletRequest().getPathInfo();
        String clean = "";
        try {
            clean = ESAPI.validator().getValidInput("HTTP path: " + path, path, "HTTPPath", 150, false);
        } catch (ValidationException e) {
            // already logged
        }
        return clean;
    }

    /**
     * Same as HttpServletRequest, no security changes required.
     * @return
     */
    public String getPathTranslated() {
        return getHttpServletRequest().getPathTranslated();
    }

    /**
     * Same as HttpServletRequest, no security changes required.
     * @return
     */
    public String getProtocol() {
        return getHttpServletRequest().getProtocol();
    }

    /**
     * Returns the query string from the HttpServletRequest after canonicalizing
     * and filtering out any dangerous characters.
     * @return
     */
    public String getQueryString() {
        String query = getHttpServletRequest().getQueryString();
        String clean = "";
        try {
            clean = ESAPI.validator().getValidInput("HTTP query string: " + query, query, "HTTPQueryString", 2000, false);
        } catch (ValidationException e) {
            // already logged
        }
        return clean;
    }

    /**
     * Same as HttpServletRequest, no security changes required. Note that this
     * reader may contain attacks and the developer is responsible for
     * canonicalizing, validating, and encoding any data from this stream.
     * @return
     * @throws IOException
     */
    public BufferedReader getReader() throws IOException {
        return getHttpServletRequest().getReader();
    }

    /**
     * Same as HttpServletRequest, no security changes required.
     * @param path 
     * @return
     */
    public String getRealPath(String path) {
        return getHttpServletRequest().getRealPath(path);
    }

    /**
     * Same as HttpServletRequest, no security changes required.
     * @return
     */
    public String getRemoteAddr() {
        return getHttpServletRequest().getRemoteAddr();
    }

    /**
     * Same as HttpServletRequest, no security changes required.
     * @return
     */
    public String getRemoteHost() {
        return getHttpServletRequest().getRemoteHost();
    }

    /**
     * Same as HttpServletRequest, no security changes required.
     * @return
     */
    public int getRemotePort() {
        return getHttpServletRequest().getRemotePort();
    }

    /**
     * Returns the name of the ESAPI user associated with this getHttpServletRequest().
     * @return
     */
    public String getRemoteUser() {
        return ESAPI.authenticator().getCurrentUser().getAccountName();
    }

    /**
     * Checks to make sure the path to forward to is within the WEB-INF
     * directory and then returns the dispatcher. Otherwise returns null.
     * @param path
     * @return
     */
    public RequestDispatcher getRequestDispatcher(String path) {
        if (path.startsWith("WEB-INF")) {
            return getHttpServletRequest().getRequestDispatcher(path);
        }
        return null;
    }

    /**
     * Returns the URI from the HttpServletRequest after canonicalizing and
     * filtering out any dangerous characters. Code must be very careful not to
     * depend on the value of a requested session id reported by the user.
     * @return
     */
    public String getRequestedSessionId() {
        String id = getHttpServletRequest().getRequestedSessionId();
        String clean = "";
        try {
            clean = ESAPI.validator().getValidInput("Requested cookie: " + id, id, "HTTPJSESSIONID", 50, false);
        } catch (ValidationException e) {
            // already logged
        }
        return clean;
    }

    /**
     * Returns the URI from the HttpServletRequest after canonicalizing and
     * filtering out any dangerous characters.
     * @return
     */
    public String getRequestURI() {
        String uri = getHttpServletRequest().getRequestURI();
        String clean = "";
        try {
            clean = ESAPI.validator().getValidInput("HTTP URI: " + uri, uri, "HTTPURI", 2000, false);
        } catch (ValidationException e) {
            // already logged
        }
        return clean;
    }

    /**
     * Returns the URL from the HttpServletRequest after canonicalizing and
     * filtering out any dangerous characters.
     * @return
     */
    public StringBuffer getRequestURL() {
        String url = getHttpServletRequest().getRequestURL().toString();
        String clean = "";
        try {
            clean = ESAPI.validator().getValidInput("HTTP URL: " + url, url, "HTTPURL", 2000, false);
        } catch (ValidationException e) {
            // already logged
        }
        return new StringBuffer(clean);
    }

    /**
     * Returns the scheme from the HttpServletRequest after canonicalizing and
     * filtering out any dangerous characters.
     * @return
     */
    public String getScheme() {
        String scheme = getHttpServletRequest().getScheme();
        String clean = "";
        try {
            clean = ESAPI.validator().getValidInput("HTTP scheme: " + scheme, scheme, "HTTPScheme", 10, false);
        } catch (ValidationException e) {
            // already logged
        }
        return clean;
    }

    /**
     * Returns the server name (host header) from the HttpServletRequest after
     * canonicalizing and filtering out any dangerous characters.
     * @return
     */
    public String getServerName() {
        String name = getHttpServletRequest().getServerName();
        String clean = "";
        try {
            clean = ESAPI.validator().getValidInput("HTTP server name: " + name, name, "HTTPServerName", 100, false);
        } catch (ValidationException e) {
            // already logged
        }
        return clean;
    }

    /**
     * Returns the server port (after the : in the host header) from the
     * HttpServletRequest after parsing and checking the range 0-65536.
     * @return
     */
	public int getServerPort() {
		int port = getHttpServletRequest().getServerPort();
		if ( port < 0 || port > 0xFFFF ) {
			logger.warning( Logger.SECURITY_FAILURE, "HTTP server port out of range: " + port );
			port = 0;
		}
		return port;
	}
 	

    /**
     * Returns the server path from the HttpServletRequest after canonicalizing
     * and filtering out any dangerous characters.
     * @return
     */
    public String getServletPath() {
        String path = getHttpServletRequest().getServletPath();
        String clean = "";
        try {
            clean = ESAPI.validator().getValidInput("HTTP servlet path: " + path, path, "HTTPServletPath", 100, false);
        } catch (ValidationException e) {
            // already logged
        }
        return clean;
    }

    /**
     * Returns a session, creating it if necessary, and sets the HttpOnly flag
     * on the JSESSIONID cookie.
     * @return
     */
    public HttpSession getSession() {
		HttpSession session = getHttpServletRequest().getSession();
		
		// User user = ESAPI.authenticator().getCurrentUser();
		// user.addSession( session );
		
		// send a new cookie header with HttpOnly on first and second responses
	    if (ESAPI.securityConfiguration().getForceHttpOnlySession()) {
	        if (session.getAttribute("HTTP_ONLY") == null) {
				session.setAttribute("HTTP_ONLY", "set");
				Cookie cookie = new Cookie("JSESSIONID", session.getId());
				cookie.setPath( getHttpServletRequest().getContextPath() );
				cookie.setMaxAge(-1); // session cookie
	            HttpServletResponse response = ESAPI.currentResponse();
	            if (response != null) {
	                ESAPI.currentResponse().addCookie(cookie);
	            }
	        }
	    }
        return session;
    }

    /**
     * Returns a session, creating it if necessary, and sets the HttpOnly flag
     * on the JSESSIONID cookie.
     * @param create 
     * @return
     */
    public HttpSession getSession(boolean create) {
        HttpSession session = getHttpServletRequest().getSession(create);
        if (session == null) {
            return null;
        }
        // User user = ESAPI.authenticator().getCurrentUser();
        // user.addSession( session );

        // send a new cookie header with HttpOnly on first and second responses
        if (ESAPI.securityConfiguration().getForceHttpOnlySession()) {
	        if (session.getAttribute("HTTP_ONLY") == null) {
	            session.setAttribute("HTTP_ONLY", "set");
	            Cookie cookie = new Cookie("JSESSIONID", session.getId());
	            cookie.setMaxAge(-1); // session cookie
	            cookie.setPath( getHttpServletRequest().getContextPath() );
	            HttpServletResponse response = ESAPI.currentResponse();
	            if (response != null) {
	                ESAPI.currentResponse().addCookie(cookie);
	            }
	        }
        }
        return session;
    }

    /**
     * Returns the ESAPI User associated with this getHttpServletRequest().
     * @return
     */
    public Principal getUserPrincipal() {
        return ESAPI.authenticator().getCurrentUser();
    }

    /**
     * Same as HttpServletRequest, no security changes required.
     * @return
     */
    public boolean isRequestedSessionIdFromCookie() {
        return getHttpServletRequest().isRequestedSessionIdFromCookie();
    }

    /**
     * Same as HttpServletRequest, no security changes required.
     * @return
     */
    public boolean isRequestedSessionIdFromUrl() {
        return getHttpServletRequest().isRequestedSessionIdFromUrl();
    }

    /**
     * Same as HttpServletRequest, no security changes required.
     * @return
     */
    public boolean isRequestedSessionIdFromURL() {
        return getHttpServletRequest().isRequestedSessionIdFromURL();
    }

    /**
     * Same as HttpServletRequest, no security changes required.
     * @return
     */
    public boolean isRequestedSessionIdValid() {
        return getHttpServletRequest().isRequestedSessionIdValid();
    }

    /**
     * Same as HttpServletRequest, no security changes required.
     * @return
     */
    public boolean isSecure() {
        // TODO Check request method to see if this is vulnerable
        return getHttpServletRequest().isSecure();
    }

    /**
     * Returns true if the ESAPI User associated with this request has the
     * specified role.
     * @param role
     * @return
     */
    public boolean isUserInRole(String role) {
        return ESAPI.authenticator().getCurrentUser().isInRole(role);
    }

    /**
     * Same as HttpServletRequest, no security changes required.
     * @param name
     */
    public void removeAttribute(String name) {
        getHttpServletRequest().removeAttribute(name);
    }

    /**
     * Same as HttpServletRequest, no security changes required.
     * @param name
     * @param o
     */
    public void setAttribute(String name, Object o) {
        getHttpServletRequest().setAttribute(name, o);
    }

    /**
     * Sets the character encoding scheme to the ESAPI configured encoding scheme.
     * @param enc
     * @throws UnsupportedEncodingException
     */
    public void setCharacterEncoding(String enc) throws UnsupportedEncodingException {
        getHttpServletRequest().setCharacterEncoding(ESAPI.securityConfiguration().getCharacterEncoding());
    }

}