/*
 * This file is part of Vestige.
 *
 * Vestige is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Vestige is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Vestige.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.googlecode.vestige.admin.web;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The webdav interceptor blocks some FrontPage requests and determinate the
 * encoding of the underlying request.
 */
public class WebdavInterceptor implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebdavInterceptor.class);

    public static final String MICROSOFT_PROTOCOL_AGENT = "Microsoft Data Access Internet Publishing Provider Protocol Discovery";

    public WebdavInterceptor() {
    }

    public void init(final javax.servlet.FilterConfig filterConfig) throws javax.servlet.ServletException {
    }

    public void destroy() {
    }

    /**
     * Filter some invalid calls from the Microsoft WebDAV clients (Windows
     * 2000, Windows XP and Office 2003).
     */
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String path = request.getServletPath();
        LOGGER.info("path is {}", path);

        HttpServletResponse response = (HttpServletResponse) servletResponse;
        if (path.equals("/")) {
            response.sendRedirect("/webdav");
        }
        // response.addHeader("DAV", "1,2");

        // response.addHeader("Allow", "OPTIONS, TRACE, PROPFIND");
        // response.addHeader("MS-AUTHOR-VIA", "DAV");

        /*
         * if (path != null) { if (path.endsWith("/index.jsp")) { // a very
         * strange microsoft webdav handling... (because of the // front page
         * extensions!) // Microsoft (protocol discoverer) is asking the root of
         * the // server // whether it can handles a WebDAV, so we have to catch
         * it here. // Unfortunately we can't // redirect the root URL ("/") to
         * a servlet, because we will // intercept any other // path handling...
         * String agent = request.getHeader("user-agent"); if
         * (MICROSOFT_PROTOCOL_AGENT.equals(agent)) { HttpServletResponse
         * response = (HttpServletResponse) servletResponse;
         * response.addHeader("DAV", "1,2"); response.addHeader("Allow",
         * "OPTIONS, TRACE, PROPFIND"); response.addHeader("MS-AUTHOR-VIA",
         * "DAV"); return; } } // block, front page and m$ office extensions
         * requests... if (path.startsWith("/_vti") ||
         * path.startsWith("/MSOffice")) { ((HttpServletResponse)
         * servletResponse).setStatus(HttpServletResponse.SC_NOT_FOUND); return;
         * } }
         */

        // request.setCharacterEncoding(WebdavServlet.getEnconding(request));
        filterChain.doFilter(servletRequest, servletResponse);
    }

}
