/*
 * (c) Copyright 2010 Nate Riffe <inkblot@movealong.org>
 *
 * This file is part of movealong-hibernate.
 *
 * movealong-hibernate is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * movealong-hibernate is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with movealong-hibernate.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.movealong.persistence.servlet;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.movealong.persistence.HibernateFacade;

import javax.servlet.*;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * <p>A Servlet {@link Filter} which creates a Hibernate session for each servlet request.   This filter requires no
 * configuration.  It may be included in a web application by adding these lines to web.xml:</p>
 *
 * <p><pre>
 *  &lt;filter&gt;
 *      &lt;filter-name&gt;HibernateFacadeFilter&lt;/filter-name&gt;
 *      &lt;filter-class&gt;org.movealong.persistence.servlet.HibernateFacadeFilter&lt;/filter-class&gt;
 *  &lt;/filter&gt;
 *
 *  &lt;filter-mapping&gt;
 *      &lt;filter-name&gt;HibernateFacadeFilter&lt;/filter-name&gt;
 *      &lt;url-pattern&gt;*&lt;/url-pattern&gt;
 *  &lt;/filter-mapping&gt;
 * </pre></p>
 */
public class HibernateFacadeFilter implements Filter {

    @Inject
    private HibernateFacade hibernate;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Injector injector = (Injector) filterConfig.getServletContext().getAttribute("injector");
        if (injector == null) {
            throw new ServletException("Servlet attribute not bound to a Guice injector: injector");
        }
        injector.injectMembers(this);
    }

    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain) throws IOException, ServletException {
        try {
            hibernate.runInSession(new Runnable() {
                @Override
                public void run() {
                    try {
                        filterChain.doFilter(servletRequest, servletResponse);
                    } catch (IOException e) {
                        throw new UndeclaredThrowableException(e);
                    } catch (ServletException e) {
                        throw new UndeclaredThrowableException(e);
                    }
                }
            });
        } catch (UndeclaredThrowableException e) {
            if (e.getUndeclaredThrowable() instanceof IOException) {
                throw (IOException) e.getUndeclaredThrowable();
            } else if (e.getUndeclaredThrowable() instanceof ServletException) {
                throw (ServletException) e.getUndeclaredThrowable();
            } else {
                throw e;
            }
        }
    }

    @Override
    public void destroy() {
        hibernate = null;
    }

}
